package controllers

import models.channel.{ Channel, FullChannel }
import models.song._
import events._

import concurrent.{ Future, blocking }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.EventSource
import play.api.http.ContentTypes.EVENT_STREAM
import play.api.mvc.{ Controller, Action, RangeResult }
import Action.async
import play.api.libs.json.JsValue
import play.api.Logger
import akka.stream.scaladsl.Source.fromPublisher
import org.reactivestreams.Publisher
import javax.inject.Inject
import akka.actor.ActorSystem
import javax.inject.Singleton

@Singleton
class MusicRoomController @Inject() (songs: SongLibrary, system: ActorSystem) extends Controller {

  private val ioOps = system.dispatchers.lookup("contexts.io-ops")
  private val logger = Logger(getClass)

  def index(channelId: Int) = Action { implicit request ⇒
    Ok(views.html.musicroom(Channel(channelId), songs.allSongs))
  }

  def addSong(channelId: Int, songId: Int) = react(channelId, songId, AddSong)

  def voteToSkip(channelId: Int, songId: Int) = react(channelId, songId, VoteToSkipSong)

  def leaveRoom(channelId: Int) = Action {
    val ch = Channel(channelId)
    Future {
      ch.msgHandler(LeaveRoom)
    }
    Ok(views.html.bye(ch.name)).withNewSession
  }

  def chat(channelId: Int) = Action(parse.urlFormEncoded) {
    implicit request ⇒
      Future {
        FullChannel(channelId).chat(request.body.get("text").get.head)
      }
      Accepted
  }

  def playSong(songId: Int) = async { implicit request ⇒
    songs.getFile(songId).map {
      RangeResult.ofFile(_, request.headers.get(RANGE), Some("audio/mp3"))
    }(ioOps)
  }

  def sseSongInfos(channelId: Int) = sse(channelId, _.songPub)

  def ssePlaylistAdds(channelId: Int) = sse(channelId, _.playlistPub)

  def sseChats(channelId: Int) = sse(channelId, _.chatPub)

  private def react(channelId: Int, songId: Int, msgr: Song ⇒ Message) = Action {
    Future(Channel(channelId).msgHandler(msgr(songs(songId))))
    Accepted
  }

  private def sse(channelId: Int, getPub: (FullChannel) ⇒ Publisher[JsValue]) = async {
    Future(getPub(FullChannel(channelId))).map { pub ⇒
      Ok.chunked(fromPublisher(pub) via EventSource.flow).as(EVENT_STREAM)
    }
  }
}
