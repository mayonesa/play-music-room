package models.chatbox

import models.chatbox.client.ChatBoxListener

private[models] trait ChatBox {
  private[models] def removeListener(l: ChatBoxListener): Unit
  private[chatbox] def setUp(l: ChatBoxListener): Unit
}