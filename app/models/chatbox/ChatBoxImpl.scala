package models.chatbox

import models.auxiliaries.{ ChatBoxSubscriber, ChatEvent, ChatBoxClientName, Text, ChatHistory }
import models.chatbox.client._
import models.chatbox.client.ChatBoxListener
import models.chatbox.client.ChatBoxListenerWithHist
import models.chatbox.client.ChatBoxFullClient
import models.song.Song

import collection.parallel
import collection.mutable

import java.time.ZonedDateTime.now

private[models] class ChatBoxImpl extends ChatBox with ClientChatBox {
  private val listeners = parallel.mutable.ParSet.empty[ChatBoxSubscriber]
  private val log = new mutable.History[ChatEvent, ChatBoxClientName]
  addListener(log)

  private[models] def removeListener(l: ChatBoxListener): Unit = {
    listeners.synchronized(listeners -= l)
    l match {
      case client: ChatBoxFullClient ⇒ client.setChatBoxOpt(None)
      case _                         ⇒
    }
  }

  private[models] def chat(s: Song) = chat(s.artist, s.name, true)

  private[chatbox] def chat(sender: ChatBoxClientName, t: Text, isSong: Boolean) = {
    val n = now()
    listeners.synchronized(listeners.foreach(_.notify(sender, (t, n, isSong))))
  }

  private[chatbox] def setUp(l: ChatBoxListener): Unit = listeners.synchronized {
    /* Even though the room already captures channels, they are also captured 
     * here so that there is no need to directly notify the ones kept in the 
     * room and consequently block unrelated room ops when chatting */
    addListener(l)

    l match {
      case fc: ChatBoxFullClient       ⇒ setUp(fc)
      case lh: ChatBoxListenerWithHist ⇒ catchUp(lh)
    }
  }

  private def setUp(client: ChatBoxFullClient): Unit = {
    client.setChatBoxOpt(Some(this))
    catchUp(client)
  }

  private def catchUp(l: ChatBoxListenerWithHist) = history.foreach {
    case (clientName, chatEvt) ⇒ l.notify(clientName, chatEvt)
  }

  private def history: ChatHistory = log.iterator

  private def addListener(l: ChatBoxSubscriber) = listeners += l
}