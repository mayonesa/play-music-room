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
  type PlaylistInfo = (Queue[Song], Int, Playing)
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

  private val ClearPlaylistSongId = -999
  private val ClearSong = Song(ClearPlaylistSongId, "", "", 0 seconds, "")
  private val KillSongId = -888
  private[models] val ClearPlaylist = (ClearSong, false)
  private[models] val KillSong = Song(KillSongId, "", "", 0 seconds, "")

  def schedule(body: () ⇒ Unit, delay: Duration, scheduler: Timer): TimerTask = {
    val task = timerTask(body)
    scheduler.schedule(task, delay.toMillis)
    task
  }

  def currentTime = System.currentTimeMillis().millis

  private def timerTask(body: () ⇒ Unit) =
    new TimerTask {
      def run = body()
    }
}