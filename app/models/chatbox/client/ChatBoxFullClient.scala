package models.chatbox.client

import models.auxiliaries.{ ChatBoxClientName, Text }
import models.chatbox.ClientChatBox

import play.api.Logger
import models.auxiliaries.ChatBoxClientName

/** channels can extend this for room-chat capabilities */
trait ChatBoxFullClient extends ChatBoxListenerWithHist {
  private val logger = Logger(getClass)
  private var chatBox: Option[ClientChatBox] = None
  def chat(t: Text): Unit = chatBox match {
    case Some(cb) ⇒ cb.chat(name, t)
    case None     ⇒ logger.error(s"$name attempting to chat w/o a chat box -- most likely client (usually a channel) has already been removed")
  }
  protected def name: ChatBoxClientName
  private[chatbox] def setChatBoxOpt(cb: Option[ClientChatBox]): Unit = chatBox = cb
}