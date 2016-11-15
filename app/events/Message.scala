package events

import models.song.Song

sealed trait Message
case class AddSong(song: Song) extends Message
case class RemoveSong(index: Int) extends Message
case class VoteToSkipSong(song: Song) extends Message
case object LeaveRoom extends Message