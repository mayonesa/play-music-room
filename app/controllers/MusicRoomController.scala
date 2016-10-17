package controllers

import models.channel.{ Channel, FullChannel }
import models.song._
import events._

import concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.http.ContentTypes.EVENT_STREAM
import play.api.libs.EventSource.flow
import play.api.mvc.{ Controller, Action }
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

  def addSong(channelId: Int, songId: Int) = react(channelId, AddSong(songs(songId)))

  def voteToSkip(channelId: Int, songId: Int) = react(channelId, VoteToSkipSong(songs(songId)))

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

  def sseSongInfos(channelId: Int) = Action {
    sse(FullChannel(channelId).songPub)
  }

  def playSong(songId: Int) = async {
    songs.getFile(songId).map {
      Ok.sendFile(_)
    }(ioOps)
  }

  def ssePlaylistAdds(channelId: Int) = Action {
    sse(FullChannel(channelId).playlistPub)
  }

  def sseChats(channelId: Int) = Action {
    sse(FullChannel(channelId).chatPub)
  }

  private def react(channelId: Int, msg: Message) = Action {
    Future(Channel(channelId).msgHandler(msg))
    Accepted
  }

  private def sse(pub: Publisher[JsValue]) = Ok.chunked(fromPublisher(pub) via flow).as(EVENT_STREAM)
}