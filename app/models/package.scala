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
  type PlaylistInfo = (Queue[SkippableSong], Int, PlayingType)
  type PlaylistView = SeqView[PlayableSong, Seq[_]]
  type PlayableSong = (SkippableSong, PlayingType)
	type SkippableSong = (Song, SkippedType)
	type PlayingType = Boolean
	type SkippedType = Boolean
	
	private[models] val Skipped = true
	private[models] val Playing = true

  type ChatBoxSubscriber = mutable.Subscriber[ChatEvent, ChatBoxClientName] // for `log` to listen in on chats
  type ChatHistory = Iterator[ChatBoxClientNameEvent]
  type ChatBoxClientNameEvent = (ChatBoxClientName, ChatEvent)
  type ChatEvent = (Text, ZonedDateTime, Boolean)
  type ChatBoxClientName = String
  type Text = String

  private val logger = Logger(getClass)

  val DefaultChatHistorySize = 1000 // if increased from 1000, must mod ChatBoxImpl.log accordingly in order to be effective
  val DefaultPlaylistSize = 500
  private val ClearPlaylistSongId = -999
  private val ClearSong = Song(ClearPlaylistSongId, "", "", 0 seconds, "")
  private val KillSongId = -888
  private[models] val ClearPlaylist = ((ClearSong, false), false)
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