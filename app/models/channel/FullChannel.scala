package models.channel

import models.chatbox.client.ChatBoxFullClient
import models.playlist.PlaylistViewer
import models.song.Song
import models.auxiliaries.{ ChatBoxClientName, ChatBoxClientNameEvent, ChatEvent, PlayableSong, PlaylistView }
import models.channel.FullChannel.{ Push, logger }

import collection.immutable.Queue
import scala.annotation.tailrec
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
    chatPush.pushChats
  }

  protected def postPlaylistInit() = {
    playlistView.map(playlistPush.addToBuff(_))
    pushRequestedPlaylistBuff()
  }
  protected def postSongAdd(song: Song) = {
    playlistPush.addToBuff((song, false))
    pushRequestedPlaylistBuff()
  }
  protected def postSongPush(): Unit = ()

  private[models] def pushSong(song: Song) = songPush.lock.synchronized {
    songPush.addToBuff(song.id)
    if (songPush.requested) {
      songPush.actuallyPushSong
    }
  }

  private def pushRequestedPlaylistBuff() =
    if (playlistPush.requested) {
      playlistPush.pushPlaylistBuff
    }

  private var songIdOpt: Option[Int] = None

  private class SongPush extends {
    val actuallyPushSong = (onNext: Int ⇒ Unit, decrementReqs: () ⇒ Unit, _: () ⇒ Boolean) ⇒ {
      logger.debug("actually pushing")
      songIdOpt.foreach(onNext)
      logger.debug("after opt.foreach")
      songIdOpt = None
      decrementReqs()
    }
  } with Push[Int](() ⇒ songIdOpt.isDefined, actuallyPushSong) {
    override def writes = Writes.IntWrites
    override def addToBuff(songId: Int) = songIdOpt = Some(songId)
  }

  private var playlistBuff = Queue.empty[PlayableSong]

  private class PlaylistPush extends {
    val pushPlaylistBuff = (onNext: PlayableSong ⇒ Unit, decrementReqs: () ⇒ Unit, _: () ⇒ Boolean) ⇒ {
      playlistBuff.foreach(onNext)
      playlistBuff = Queue.empty[PlayableSong]
      decrementReqs()
    }
  } with Push[PlayableSong](() ⇒ !playlistBuff.isEmpty, pushPlaylistBuff) {
    override def writes = new Writes[PlayableSong] {
      def writes(ps: PlayableSong) = ps match {
        case (song, current) ⇒
          Json.obj(
            "song" -> song.name,
            "duration" -> song.length.toString,
            "current" -> current)
      }
    }
    override def addToBuff(song: PlayableSong) = playlistBuff = playlistBuff.enqueue(song)
  }

  private var chatBuff = Queue.empty[ChatBoxClientNameEvent]

  private class ChatPush extends {
    private val chatLeft = () ⇒ !chatBuff.isEmpty
    val pushChats = (onNext: ChatBoxClientNameEvent ⇒ Unit, decrementReqs: () ⇒ Unit, requested: () ⇒ Boolean) ⇒ {
      @tailrec
      def loop(): Unit =
        if (requested() && chatLeft()) {
          val (chat, tempChatQ) = chatBuff.dequeue
          chatBuff = tempChatQ
          chatBuff.foreach(onNext)
          decrementReqs()
          loop()
        }
      loop()
    }
  } with Push[ChatBoxClientNameEvent](chatLeft, pushChats) {
    override def writes = new Writes[ChatBoxClientNameEvent] {
      def writes(cbcne: ChatBoxClientNameEvent) = cbcne match {
        case (author, chatEvent) ⇒
          Json.obj(
            "author" -> author,
            "text" -> chatEvent._1,
            "timestamp" -> chatEvent._2)
      }
    }
    override def addToBuff(cbcne: ChatBoxClientNameEvent) = chatBuff = chatBuff.enqueue(cbcne)
  }
}

import models.channel.Channel.newId
import com.sun.org.apache.xml.internal.resolver.helpers.Debug

object FullChannel {
  def apply(id: Int): FullChannel = Channel(id).asInstanceOf[FullChannel]
  private def apply(name: String): FullChannel = FullChannel(newId, name)

  private val logger = Logger(getClass)

  def addChannel(name: String): Channel = Channel.addChannel(FullChannel(name))

  private abstract class Push[T](readyForPush: () ⇒ Boolean, push: (T ⇒ Unit, () ⇒ Unit, () ⇒ Boolean) ⇒ Unit) {
    val lock = new AnyRef // TODO: needed? music room is got 'em
    val pub = new PublisherImpl
    private var unfulfilledRequests: Long = _
    private var sub: Subscriber[_ >: JsValue] = _

    implicit def writes: Writes[T]
    def decrementReqs() = unfulfilledRequests -= 1
    def requested = unfulfilledRequests > 0
    protected def addToBuff(t: T)
    private def onNext(t: T): Unit = sub.onNext(Json.toJson(t))

    class PublisherImpl extends Publisher[JsValue] {
      def subscribe(s: Subscriber[_ >: JsValue]): Unit = {
        sub = s
        s.onSubscribe(new Subscription {
          def request(n: Long): Unit = lock.synchronized {
            unfulfilledRequests += n
            logger.debug("ready for push? " + readyForPush())
            if (readyForPush()) {
              logger.debug("push")
              push(onNext, () ⇒ decrementReqs(), () ⇒ requested)
              logger.debug("pushed")
            }
          }
          def cancel(): Unit = () // Cancellation handled through the "LeaveRoom" event
        })
      }
    }
  }
}