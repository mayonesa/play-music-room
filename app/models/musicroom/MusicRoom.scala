package models.musicroom

import models.channel.Channel
import models.song.Song
import models.chatbox._
import models.chatbox.client._
import models.playlist._
import events._
import models.auxiliaries.{ schedule, currentTime, KillSong }
import models.chatbox.ChatBoxImpl
import models.chatbox.ChatBox
import models.playlist.Playlist
import models.playlist.PlaylistViewer
import models.chatbox.client.ChatBoxListener

import concurrent.Future
import concurrent.duration._
import collection.parallel.ParSet
import collection.mutable
import java.util.{ Timer, TimerTask }
import java.util.concurrent.atomic.AtomicInteger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger

object MusicRoom {
  private val logger = Logger(getClass)

  private def apply(name: String): MusicRoom = new MusicRoom(name)
  private def apply(id: Int, name: String, playlist: Playlist, cs: ParSet[Channel], nSkipVotes: Int, chatBox: ChatBox, roomScheduler: Timer, futurePlay: TimerTask, lastPlayTime: Duration, lock: AnyRef) =
    new MusicRoom(id, name, playlist, cs, nSkipVotes, chatBox, roomScheduler, futurePlay, lastPlayTime, lock)

  private val roomRepo = mutable.Map.empty[Int, MusicRoom]
  private val roomIdGen = new AtomicInteger

  /**
   * Public entry points... further communication with the clients will be through the channel
   * Assumptions: - only *one* room per channel
   *              - clients will not attempt to send messages to channels until they *finished* joining and/or creating the room.
   * @param roomId the id of the room.
   * @param channel the communication Channel with the clients going forward.
   */

  def createAndJoinRoom(roomName: String, channel: Channel): Future[String] = Future(addAndInitChannel(addNewRoom(roomName), channel).name)

  def joinRoom(roomId: Int, channel: Channel): Future[String] = Future {
    val room = addAndInitChannel(roomRepo(roomId), channel)
    room.lock.synchronized(roomRepo(roomId).playCurrentInMidstream(channel))
    room.name
  }

  def roomViews: Map[String, String] = roomRepo.toMap.map {
    case (k, v) ⇒ (k.toString, v.name)
  }

  private def addAndInitChannel(room: MusicRoom, channel: Channel) = {
    val roomLock = room.lock
    val roomId = room.id

    roomLock.synchronized {
      val r = roomRepo(roomId) addChannel channel
      putRoom(r)

      channel match {
        case pv: PlaylistViewer ⇒ pv.initPlaylist(r.playlist.state)
        case _                  ⇒
      }
    }

    val chatBox = roomRepo(roomId).chatBox
    channel match {
      case l: ChatBoxListener ⇒ l.setUpChat(chatBox)
      case _                  ⇒
    }

    channel.onReceive { message ⇒
      {
        val r = roomRepo(roomId)
        message match {
          case AddSong(song)        ⇒ roomLock.synchronized(putRoom(r.addSong(song)))
          case VoteToSkipSong(song) ⇒ roomLock.synchronized(r.voteForSkip(song).foreach(putRoom))
          case LeaveRoom ⇒
            roomLock.synchronized(putRoom(r dropChannel channel))
            channel match {
              case l: ChatBoxListener ⇒ chatBox.removeListener(l)
              case _                  ⇒
            }
        }
      }
    }
    room
  }

  private def addNewRoom(roomName: String) = roomRepo.synchronized {
    val room = MusicRoom(roomName)
    putRoom(room)
    room
  }

  private def putRoom(room: MusicRoom) = roomRepo += kv(room)

  private def kv(room: MusicRoom) = room.id -> room
}

import MusicRoom._

private class MusicRoom(private val id: Int,
                        private val name: String,
                        private val playlist: Playlist,
                        private val channels: ParSet[Channel], // parallel set curbs blocking subsequent channels during iterational processing
                        nSkipVotes: Int,
                        private val chatBox: ChatBox,
                        roomScheduler: Timer, // timer for all versions of the same room to avoid creating new threads for every room version
                        futurePlay: TimerTask, // cancellable future-play task
                        lastPlayTime: Duration,
                        private val lock: AnyRef) { // lock for all versions of the same room to avoid blocking all the rooms just for room-specific safety req'ts

  private def this(name: String) = this(roomIdGen.incrementAndGet(), name, Playlist(), ParSet.empty[Channel], 0, new ChatBoxImpl, new Timer(), null, null, new AnyRef)

  private def addChannel(channel: Channel) = room(channels + channel)

  private def dropChannel(channel: Channel) = room(channels - channel)

  private def addSong(song: Song) = {
    val newPlaylist = playlist enqueue song
    if (playlist.isPlaying) {
      publishAdd(song)
      room(newPlaylist)
    } else {
      room(newPlaylist.advance()).play()
    }
  }

  /**
   * assumptions:
   * 	one-vote limit per channel per song will be enforced by the client
   * 	users can only vote on current song
   */
  private def voteForSkip(song: Song) =
    if (currentSong == song) {
      Some {
        if (isMinSkipVotes) {
          skip()
        } else room(nSkipVotes + 1)
      }
    } else None

  private def playCurrentInMidstream(ch: Channel) = if (playlist.isPlaying) {
    val currentSongElapsed = currentTime - lastPlayTime
    if (currentSongElapsed < currentSong.length) {
      ch.pushSong(currentSong, currentSongElapsed)
    }
  }

  private def publishAdd(songId: Song) = channels.foreach {
    case pv: PlaylistViewer ⇒ pv.onSongAdd(songId)
    case _                  ⇒
  }

  private def isMinSkipVotes = nSkipVotes + 1 >= (channels.size / 2f).ceil

  private def skip() = {
    futurePlay.cancel()
    if (playlist.hasNext) {
      next(0).play()
    } else {
      stopCurrentSong()
    }
  }

  private def stopCurrentSong() = {
    val stoppedRoom = stopSongRoom
    channels.foreach(stoppedRoom.pushSong(_, KillSong))
    stoppedRoom
  }

  private def next(): MusicRoom = next(nSkipVotes)

  private def next(newNSkipVs: Int) = room(playlist.advance(), newNSkipVs)

  private def play(): MusicRoom = {
    channels.foreach(pushCurrentSong)
    chatBox.chat(currentSong)
    schedNextPlay()
  }

  private def schedNextPlay() = room(roomSchedule(processNext, currentSong.length))

  private def pushCurrentSong(c: Channel) = pushSong(c, currentSong)

  private def pushSong(c: Channel, s: Song) = {
    c.pushSong(s)
    pushPlaylist(c)
  }

  private def processNext() = lock.synchronized {
    val room = latestRoom
    putRoom {
      if (room.playlist.hasNext) {
        room.next().play()
      } else {
        room.onStop()
      }
    }
  }

  private def onStop() = {
    val stoppedRoom = stopSongRoom
    stoppedRoom.channels.foreach(stoppedRoom.pushPlaylist)
    stoppedRoom
  }

  private def pushPlaylist(c: Channel) = c match {
    case pv: PlaylistViewer ⇒ pv.onSongPush(playlist.state)
    case _                  ⇒
  }

  private def currentSong = playlist.currentSongOpt.get

  private def room(newSkipVs: Int): MusicRoom = room(playlist, newSkipVs)

  private def room(newPlaylist: Playlist): MusicRoom = room(newPlaylist, nSkipVotes)

  private def room(newChannels: ParSet[Channel]) = MusicRoom(id, name, playlist, newChannels, nSkipVotes, chatBox, roomScheduler, futurePlay, lastPlayTime, lock)

  private def room(newFuturePlay: TimerTask) = MusicRoom(id, name, playlist, channels, nSkipVotes, chatBox, roomScheduler, newFuturePlay, currentTime, lock)

  private def room(newPlaylist: Playlist, newSkipVs: Int) = MusicRoom(id, name, newPlaylist, channels, newSkipVs, chatBox, roomScheduler, futurePlay, lastPlayTime, lock)

  private def stopSongRoom = MusicRoom(id, name, playlist.stop, channels, 0, chatBox, roomScheduler, null, null, lock)

  private def roomSchedule(body: () ⇒ Unit, delay: Duration) = schedule(body, delay, roomScheduler)

  private def latestRoom = roomRepo(id)
}
