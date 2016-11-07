package models.channel

import events.{ Message, LeaveRoom }
import models.song.Song
import models.auxiliaries.schedule

import collection.concurrent
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicInteger
import java.util.{ Timer, TimerTask }

abstract class Channel(val id: Int, val name: String) {
  private var _msgHandler: (Message) ⇒ Unit = _ // TODO: get rid of var (and perhaps the whole thing)
	private val scheduler = new Timer
	private var leaveRoomTask: TimerTask = schedRoomLeave()

	def ping: Unit = {
		leaveRoomTask.cancel()
		leaveRoomTask = schedRoomLeave()
	}
  final def msgHandler = _msgHandler // consider wrapping up in Option and throwing exception for None
  private[models] def pushSong(song: Song, startTime: Duration = 0 seconds) // push a song to the client which will start to play. can be used to update client's playlist representation.
  private[models] def onReceive(handler: (Message) ⇒ Unit) = _msgHandler = handler
	private def schedRoomLeave() = schedule(() => msgHandler(LeaveRoom), 2.minutes, scheduler)
}

object Channel {
  def apply(id: Int): Channel = channels(id)

  private val channels = concurrent.TrieMap.empty[Int, Channel]
  private val youngestId = new AtomicInteger()

  private[channel] def addChannel(c: Channel) = {
    channels += c.id -> c
    c
  }
  private[channel] def newId = youngestId.incrementAndGet()
}