package models.channel

import models.chatbox.client.ChatBoxFullClient
import models.playlist.PlaylistViewer
import PlaylistViewer.playlistForeach
import models.song.Song
import models.auxiliaries.{ ChatBoxClientName, ChatBoxClientNameEvent, ChatEvent, PlaylistViewSong, PlaylistInfo, DefaultChatHistorySize, DefaultPlaylistSize, PlaylistViewIndicator, Update }
import PlaylistViewIndicator.{ Regular, Removable }
import models.channel.FullChannel.{ SongPush, PlaylistPush, ChatPush, ChannelUpdatePush, Stop, logger }
import models.MaxSizeBuffer

import annotation.tailrec
import concurrent.duration._
import org.reactivestreams.{ Publisher, Subscriber, Subscription }
import play.api.libs.json._
import play.api.Logger

case class FullChannel(override val id: Int, _name: String) extends Channel(id, _name) with ChatBoxFullClient with PlaylistViewer {
  private val songPush = new SongPush()
  private val playlistPush = new PlaylistPush()
  private val chatPush = new ChatPush()
  private val channelUpdatePush = new ChannelUpdatePush()

  def songPub: Publisher[JsValue] = songPush.pub
  def playlistPub: Publisher[JsValue] = playlistPush.pub
  def chatPub: Publisher[JsValue] = chatPush.pub
  def channelUpdatePub: Publisher[JsValue] = channelUpdatePush.pub

  def notify(cn: ChatBoxClientName, e: ChatEvent) = chatPush.pushRequested((cn, e))

  protected[models] def initPlaylist(pl: PlaylistInfo) = playlistPush.lock.synchronized(sendPlaylist(pl))
  protected[models] def onSongPush(pl: PlaylistInfo) = playlistPush.lock.synchronized {
    playlistPush.toClear()
    playlistPush.pushRequested()
    sendPlaylist(pl)
  }
  protected[models] def onSongAdd(song: Song, adder: Channel, playlistIndex: Int) =
    playlistPush.pushRequested(Left((song, if (this == adder) Removable else Regular, playlistIndex)))

  private[models] def notifyOfChannel(ch: Channel, action: Update.Value) = channelUpdatePush.pushRequested((ch, action))

  private[models] def stop() = songPush.pushRequested(Right(Stop))

  private[models] def pushSong(song: Song, startTime: Duration) = songPush.pushRequested(Left((song.id, startTime)))

  private def sendPlaylist(pli: PlaylistInfo) = {
    playlistForeach(pli)(this) { pvSong ⇒
      playlistPush.addToBuff(Left(pvSong))
    }
    playlistPush.pushRequested()
  }
}


import models.channel.Channel.newId

object FullChannel {
  def apply(id: Int): FullChannel = Channel(id).asInstanceOf[FullChannel]
  private def apply(name: String): FullChannel = FullChannel(newId, name)

  private val Clear = "clear"
  private val Stop = "stop"

  private val logger = Logger(getClass)

  def addChannel(name: String): Channel = Channel.addChannel(FullChannel(name))

  private class SongPush extends Push[Either[(Int, Duration), String]](1) {
    override protected[FullChannel] def writes = new Writes[Either[(Int, Duration), String]] {
      def writes(songPlay: Either[(Int, Duration), String]) = songPlay match {
        case Left((songId, startTime)) ⇒
          Json.obj(
            "songId" -> songId,
            "startTimeInSecs" -> startTime.toSeconds)
        case Right(s) ⇒ JsString(s)
      }
    }
  }

  private class ChannelUpdatePush extends Push[(Channel, Update.Value)] {
    override protected[FullChannel] def writes = new Writes[(Channel, Update.Value)] {
      def writes(chAct: (Channel, Update.Value)) = chAct match {
        case (channel, action) ⇒
          Json.obj(
            "id" -> channel.id,
            "name" -> channel.name,
            "action" -> action)
      }
    }
  }

  private class PlaylistPush extends Push[Either[PlaylistViewSong, String]](DefaultPlaylistSize) {
    private[FullChannel] def toClear() = {
      clearBuff()
      pushRequested(Right(Clear))
    }
    override protected[FullChannel] def writes = new Writes[Either[PlaylistViewSong, String]] {
      def writes(ps: Either[PlaylistViewSong, String]) = ps match {
        case Left((song, songIndicator, playlistIndex)) ⇒
          Json.obj(
            "id" -> song.id,
            "name" -> song.name,
            "artist" -> song.artist,
            "duration" -> song.timeStr,
            "indicator" -> songIndicator,
            "index" -> playlistIndex)
        case Right(s) ⇒ JsString(s)
      }
    }
  }

  private class ChatPush extends Push[ChatBoxClientNameEvent](DefaultChatHistorySize) {
    override protected[FullChannel] def writes = new Writes[ChatBoxClientNameEvent] {
      def writes(cbcne: ChatBoxClientNameEvent) = cbcne match {
        case (author, chatEvent) ⇒
          Json.obj(
            "author" -> author,
            "text" -> chatEvent._1,
            "timestamp" -> chatEvent._2,
            "isSong" -> chatEvent._3)
      }
    }
  }

  private abstract class Push[T](maxBuffSize: Int = Int.MaxValue) {
    private var buff = MaxSizeBuffer[T](maxBuffSize)
    private[FullChannel] val lock = new AnyRef
    private[FullChannel] val pub = new PublisherImpl
    private var sub: Subscriber[_ >: JsValue] = _
    private var unfulfilledRequests: Long = _

    protected[FullChannel] implicit def writes: Writes[T]
    protected[FullChannel] def addToBuff(t: T) = buff = buff enqueue t
    private[FullChannel] def clearBuff() = buff = MaxSizeBuffer[T](maxBuffSize)
    private[FullChannel] def pushRequested(t: T): Unit = lock.synchronized {
      addToBuff(t)
      pushRequested()
    }
    private[FullChannel] def pushRequested() = if (requested) {
      push()
    }
    private def readyForPush = !buff.isEmpty
    private def onNext(t: T) = sub.onNext(Json.toJson(t))
    private def decrementReqs() = unfulfilledRequests -= 1
    private def requested = unfulfilledRequests > 0
    private def push() = {
      @tailrec
      def loop(): Unit =
        if (requested && !buff.isEmpty) {
          val (t, newBuff) = buff.dequeue
          buff = newBuff
          onNext(t)
          decrementReqs()
          loop()
        }
      loop()
    }
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