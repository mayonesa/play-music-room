package models.chatbox.client

/** channels can inherit this trait to listen in on room conversations (including previous history) */
private[models] trait ChatBoxListenerWithHist extends ChatBoxListener