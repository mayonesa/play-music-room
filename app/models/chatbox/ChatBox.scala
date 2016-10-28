package models.chatbox

import models.chatbox.client.ChatBoxListener
import models.auxiliaries.{ ChatBoxClientName, Text }
import models.song.Song

private[models] trait ChatBox {
  private[models] def removeListener(l: ChatBoxListener)
  private[models] def chat(song: Song)
  private[chatbox] def setUp(l: ChatBoxListener)
}
