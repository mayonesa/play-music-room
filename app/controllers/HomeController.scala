package controllers

import models.musicroom.MusicRoom.{ roomViews, joinRoom, createAndJoinRoom }
import models.channel.FullChannel.addChannel

import concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{ Action, Controller }
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.Logger
import javax.inject.{ Singleton, Inject }

@Singleton
class HomeController @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {
  // TODO: send "Conflict" (?) if channel (or room) name already exist
  val entryForm: Form[Entry] = Form {
    mapping(
      "channelName" -> nonEmptyText,
      "joinRoomIdOpt" -> optional(number),
      "newRoomNameOpt" -> optional(text))(Entry.apply)(Entry.unapply) verifying ("must select a pre-existing or create a new one but not both",
        entry ⇒ entry.joinRoomIdOpt.isDefined ^ entry.newRoomNameOpt.isDefined)
  }

  def index() = Action {
    Ok(views.html.index(entryForm, rooms)).withNewSession
  }

  def join() = Action.async {
    implicit request ⇒
      entryForm.bindFromRequest().fold(formWithErrors ⇒ Future(BadRequest(views.html.index(formWithErrors, rooms))),
        { eForm ⇒
          val channel = addChannel(eForm.channelName)
          (eForm.newRoomNameOpt match {
            case None              ⇒ joinRoom(eForm.joinRoomIdOpt.get, channel)
            case Some(newRoomName) ⇒ createAndJoinRoom(newRoomName, channel)
          }) map { roomName ⇒
            Redirect(routes.MusicRoomController.index(channel.id)).withSession("roomName" -> roomName)
          }
        })
  }

  private def rooms = roomViews
}

case class Entry(channelName: String, joinRoomIdOpt: Option[Int], newRoomNameOpt: Option[String])