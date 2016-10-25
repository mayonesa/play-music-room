package models.channel

import models.chatbox.client.ChatBoxFullClient
import models.playlist.PlaylistViewer
import PlaylistViewer.playlistView
import models.song.Song
import models.auxiliaries.{ ChatBoxClientName, ChatBoxClientNameEvent, ChatEvent, PlayableSong, ClearPlaylist, PlaylistInfo }
import models.channel.FullChannel.{ SongPush, PlaylistPush, ChatPush, logger }

import collection.mutable
import annotation.tailrec
import concurrent.duration._
import org.reactivestreams.{ Publisher, Subscriber, Subscription }
import play.api.libs.json._
import play.api.Logger

case class FullChannel(override val id: Int, _name: String) extends Channel(id, _name) with ChatBoxFullClient with PlaylistViewer {
  private val songPush = new SongPush()
  private val playlistPush = new PlaylistPush()
  private val chatPush = new ChatPush()

  def songPub: Publisher[JsValue] = songPush.pub
  def playlistPub: Publisher[JsValue] = playlistPush.pub
  def chatPub: Publisher[JsValue] = chatPush.pub

  def notify(cn: ChatBoxClientName, e: ChatEvent) = chatPush.lock.synchronized {
    chatPush.addToBuff((cn, e))
    chatPush.push()
  }
  protected[models] def initPlaylist(pl: PlaylistInfo) = playlistPush.lock.synchronized(sendPlaylist(pl))
  protected[models] def onSongPush(pl: PlaylistInfo) = playlistPush.lock.synchronized {
    playlistPush.toClear()
    pushRequestedPlaylistBuff()
    sendPlaylist(pl)
  }
  protected[models] def onSongAdd(song: Song) = playlistPush.lock.synchronized {
    playlistPush.addToBuff((song, false))
    pushRequestedPlaylistBuff()
  }

  private[models] def pushSong(song: Song, startTime: Duration) = songPush.lock.synchronized {
    songPush.addToBuff((song.id, startTime))
    if (songPush.requested) {
      songPush.push()
    }
  }

  private def sendPlaylist(pli: PlaylistInfo) = {
    playlistView(pli).map(playlistPush.addToBuff(_)).force
    pushRequestedPlaylistBuff()
  }

  private def pushRequestedPlaylistBuff() =
    if (playlistPush.requested) {
      playlistPush.push()
    }
}

import models.channel.Channel.newId
import com.sun.org.apache.xml.internal.resolver.helpers.Debug

object FullChannel {
  def apply(id: Int): FullChannel = Channel(id).asInstanceOf[FullChannel]
  private def apply(name: String): FullChannel = FullChannel(newId, name)

  private val logger = Logger(getClass)

  def addChannel(name: String): Channel = Channel.addChannel(FullChannel(name))

  private class SongPush extends Push[(Int, Duration)] {
    override protected[FullChannel] def push() = {
      onNext(buff.dequeueAll(_ ⇒ true).last)
      decrementReqs()
    }
    override protected[FullChannel] def addToBuff(songPlay: (Int, Duration)) = {
      buff.clear()
      buff.enqueue(songPlay)
    }
    override protected[FullChannel] def writes = new Writes[(Int, Duration)] {
      def writes(songPlay: (Int, Duration)) = songPlay match {
        case (songId, startTime) ⇒
          Json.obj(
            "songId" -> songId,
            "startTimeInSecs" -> startTime.toSeconds)
      }
    }
  }

  private class PlaylistPush extends Push[PlayableSong] {
    private[FullChannel] def toClear() = {
      buff.clear()
      addToBuff(ClearPlaylist)
    }
    override protected[FullChannel] def writes = new Writes[PlayableSong] {
      def writes(ps: PlayableSong) = ps match {
        case (song, current) ⇒
          Json.obj(
            "id" -> song.id,
            "name" -> song.name,
            "artist" -> song.artist,
            "duration" -> song.length.toString,
            "current" -> current)
      }
    }
  }

  private class ChatPush extends Push[ChatBoxClientNameEvent] {
    override protected[FullChannel] def writes = new Writes[ChatBoxClientNameEvent] {
      def writes(cbcne: ChatBoxClientNameEvent) = cbcne match {
        case (author, chatEvent) ⇒
          Json.obj(
            "author" -> author,
            "text" -> chatEvent._1,
            "timestamp" -> chatEvent._2)
      }
    }
  }

  private trait Push[T] {
    private[FullChannel] val buff = mutable.Queue.empty[T]
    private[FullChannel] val lock = new AnyRef
    private[FullChannel] val pub = new PublisherImpl
    private var sub: Subscriber[_ >: JsValue] = _
    private var unfulfilledRequests: Long = _

    protected[FullChannel] implicit def writes: Writes[T]
    protected[FullChannel] def addToBuff(t: T) = buff.enqueue(t)
    protected[FullChannel] def push() = {
      @tailrec
      def loop(): Unit =
        if (requested && !buff.isEmpty) {
          onNext(buff.dequeue)
          decrementReqs()
          loop()
        }
      loop()
    }
    private[FullChannel] def readyForPush = !buff.isEmpty
    private[FullChannel] def requested = unfulfilledRequests > 0
    protected[FullChannel] def onNext(t: T) = sub.onNext(Json.toJson(t))
    protected[FullChannel] def decrementReqs() = unfulfilledRequests -= 1

    private[FullChannel] class PublisherImpl extends Publisher[JsValue] {
      def subscribe(s: Subscriber[_ >: JsValue]): Unit = {
        sub = s
        s.onSubscribe(new Subscription {
          def request(n: Long): Unit = lock.synchronized {
            unfulfilledRequests += n
            if (readyForPush) {
              push()
            }
          }
          def cancel(): Unit = () // Cancellation handled through the "LeaveRoom" event
        })
      }
    }
  }
}