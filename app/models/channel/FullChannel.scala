package models.channel

import models.chatbox.client.ChatBoxFullClient
import models.playlist.PlaylistViewer
import models.song.Song
import models.auxiliaries.{ ChatBoxClientName, ChatBoxClientNameEvent, ChatEvent, PlayableSong }
import models.channel.FullChannel.{ Push, ClearPlaylist, logger }

import collection.immutable.Queue
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
  protected def postPlaylistInit() = playlistPush.lock.synchronized(sendPlaylist())
  protected def postSongPush(): Unit = playlistPush.lock.synchronized {
    playlistBuff = Queue(ClearPlaylist)
    pushRequestedPlaylistBuff()
    sendPlaylist()
  }
  protected def postSongAdd(song: Song) = playlistPush.lock.synchronized {
    playlistPush.addToBuff((song, false))
    pushRequestedPlaylistBuff()
  }

  private[models] def pushSong(song: Song) = songPush.lock.synchronized {
    songPush.addToBuff(song.id)
    if (songPush.requested) {
      songPush.push()
    }
  }

  private def sendPlaylist() = {
    playlistView.map(playlistPush.addToBuff(_)).force
    pushRequestedPlaylistBuff()
  }

  private def pushRequestedPlaylistBuff() =
    if (playlistPush.requested) {
      playlistPush.push()
    }

  private var songIdOpt: Option[Int] = None

  private class SongPush extends {
    private val actuallyPushSong = (onNext: Int ⇒ Unit, decrementReqs: () ⇒ Unit, _: () ⇒ Boolean) ⇒ {
      songIdOpt.foreach(onNext)
      songIdOpt = None
      decrementReqs()
    }
  } with Push[Int](() ⇒ songIdOpt.isDefined, actuallyPushSong) {
    override protected[channel] def addToBuff(songId: Int) = songIdOpt = Some(songId)
    override protected[channel] def writes = Writes.IntWrites
  }

  private var playlistBuff = Queue.empty[PlayableSong]

  // TODO: refactor out push() after chat history works
  
  private class PlaylistPush extends {
    private val songLeft = () ⇒ !playlistBuff.isEmpty
    private val pushPlaylistBuff = (onNext: PlayableSong ⇒ Unit, decrementReqs: () ⇒ Unit, requested: () ⇒ Boolean) ⇒ {
      @tailrec
      def loop(): Unit =
        if (requested() && songLeft()) {
          val (song, tempPlaylistBuff) = playlistBuff.dequeue
          playlistBuff = tempPlaylistBuff
          onNext(song)
          decrementReqs()
          loop()
        }
      loop()
    }
  } with Push[PlayableSong](songLeft, pushPlaylistBuff) {
    override protected[channel] def addToBuff(song: PlayableSong) = playlistBuff = playlistBuff.enqueue(song)
    override protected[channel] def writes = new Writes[PlayableSong] {
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

  private var chatBuff = Queue.empty[ChatBoxClientNameEvent]

  private class ChatPush extends {
    private val chatLeft = () ⇒ !chatBuff.isEmpty
    private val pushChats = (onNext: ChatBoxClientNameEvent ⇒ Unit, decrementReqs: () ⇒ Unit, requested: () ⇒ Boolean) ⇒ {
      @tailrec
      def loop(): Unit =
        if (requested() && chatLeft()) {
          val (chat, tempChatQ) = chatBuff.dequeue
          chatBuff = tempChatQ
          onNext(chat)
          decrementReqs()
          loop()
        }
      loop()
    }
  } with Push[ChatBoxClientNameEvent](chatLeft, pushChats) {
    override protected[channel] def addToBuff(cbcne: ChatBoxClientNameEvent) = chatBuff = chatBuff.enqueue(cbcne)
    override protected[channel] def writes = new Writes[ChatBoxClientNameEvent] {
      def writes(cbcne: ChatBoxClientNameEvent) = cbcne match {
        case (author, chatEvent) ⇒
          Json.obj(
            "author" -> author,
            "text" -> chatEvent._1,
            "timestamp" -> chatEvent._2)
      }
    }
  }
}

import models.channel.Channel.newId
import com.sun.org.apache.xml.internal.resolver.helpers.Debug

object FullChannel {
  private val ClearPlaylistSongId = -999
  private val ClearPlaylist = (Song(ClearPlaylistSongId, "", "", 0 seconds, ""), false)
  def apply(id: Int): FullChannel = Channel(id).asInstanceOf[FullChannel]
  private def apply(name: String): FullChannel = FullChannel(newId, name)

  private val logger = Logger(getClass)

  def addChannel(name: String): Channel = Channel.addChannel(FullChannel(name))

  private abstract class Push[T](readyForPush: () ⇒ Boolean, push: (T ⇒ Unit, () ⇒ Unit, () ⇒ Boolean) ⇒ Unit) {
    private[channel] val lock = new AnyRef
    private[channel] val pub = new PublisherImpl
    private var sub: Subscriber[_ >: JsValue] = _
    private var unfulfilledRequests: Long = _

    protected[channel] implicit def writes: Writes[T]
    protected[channel] def addToBuff(t: T)
    private[channel] def push(): Unit = push(onNext, () ⇒ decrementReqs, () ⇒ requested)
    private[channel] def requested = unfulfilledRequests > 0
    private def onNext(t: T) = sub.onNext(Json.toJson(t))
    private def decrementReqs() = unfulfilledRequests -= 1

    private[channel] class PublisherImpl extends Publisher[JsValue] {
      def subscribe(s: Subscriber[_ >: JsValue]): Unit = {
        sub = s
        s.onSubscribe(new Subscription {
          def request(n: Long): Unit = lock.synchronized {
            unfulfilledRequests += n
            if (readyForPush()) {
              push(onNext, () ⇒ decrementReqs, () ⇒ requested)
            }
          }
          def cancel(): Unit = () // Cancellation handled through the "LeaveRoom" event
        })
      }
    }
  }
}