package models.chatbox

import models.auxiliaries.{ ChatBoxClientName, Text }

private[chatbox] trait ClientChatBox {
  private[chatbox] def chat(cn: ChatBoxClientName, t: Text): Unit
}
