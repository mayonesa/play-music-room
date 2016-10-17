package models

import models.song.Song

import concurrent.duration._
import collection.SeqView
import collection.immutable.Queue
import collection.mutable

import java.util.{ Timer, TimerTask }
import java.time.ZonedDateTime
import play.api.Logger

package object auxiliaries {
  type PlaylistWithCurrentSongIndex = (Queue[Song], Int)
  type PlaylistView = SeqView[PlayableSong, Seq[_]]
  type PlayableSong = (Song, Playing)
  type Playing = Boolean

  type ChatBoxSubscriber = mutable.Subscriber[ChatEvent, ChatBoxClientName] // for `log` to listen in on chats
  type ChatHistory = Iterator[ChatBoxClientNameEvent]
  type ChatBoxClientNameEvent = (ChatBoxClientName, ChatEvent)
  type ChatEvent = (Text, ZonedDateTime)
  type ChatBoxClientName = String
  type Text = String
  
  private val logger = Logger(getClass)

  def schedule(body: () ⇒ Unit, delay: Duration, scheduler: Timer): TimerTask = {
    val task = timerTask(body)
    logger.debug(s"schedule: $body in $delay")
    scheduler.schedule(task, delay.toMillis)
    task
  }

  def timerTask(body: () ⇒ Unit) =
    new TimerTask {
      def run = body()
    }
}