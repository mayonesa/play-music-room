package models.chatbox.client

import models.auxiliaries.ChatBoxSubscriber
import models.chatbox.ChatBox

/** channels can inherit this trait to listen in on room conversations (excluding previous history) */
trait ChatBoxListener extends ChatBoxSubscriber {
  final private[models] def setUpChat(chatBox: ChatBox): Unit = chatBox.setUp(this)
}