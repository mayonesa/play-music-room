package models

import models.song.Song
import models.channel.Channel

import concurrent.duration._
import collection.SeqView
import collection.immutable.Queue
import collection.mutable

import java.util.{ Timer, TimerTask }
import java.time.ZonedDateTime
import play.api.Logger

package object auxiliaries {
  object PlaylistViewIndicator extends Enumeration {
    val Current, Skipped, Regular, Removable = Value
  }

  type PlaylistInfo = (Queue[PlaylistSong], Int, PlayingType)
  type PlaylistViewSong = (Song, PlaylistViewIndicator.Value, Int)
  type PlaylistSong = (Song, Adder, SkippedType)
  type PlayingType = Boolean
  type SkippedType = Boolean
  type Adder = Channel

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
  private val ClearPlaylistViewSongId = -999
  private val ClearSong = Song(ClearPlaylistViewSongId, "", "", 0 seconds, "")
  private val KillSongId = -888
  private[models] val ClearPlaylist = (ClearSong, PlaylistViewIndicator.Regular, -1)
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