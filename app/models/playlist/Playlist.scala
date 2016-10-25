package models.playlist

import models.song.Song
import models.auxiliaries.{ PlaylistInfo, Playing }

import collection.immutable.Queue

private[models] object Playlist {
  private[models] def apply(): Playlist = new Playlist
  private def apply(maxSize: Int): Playlist = new Playlist(maxSize)
  private def apply(list: Queue[Song], currentSongIndex: Int, playing: Playing, maxSize: Int) = new Playlist(list, currentSongIndex, playing, maxSize)
}

private[models] class Playlist(list: Queue[Song], currentSongIndex: Int, playing: Playing, maxSize: Int = 1000) {
  private def this(maxSize: Int) = this(Queue.empty[Song], -1, false, maxSize)
  private def this() = this(Queue.empty[Song], -1, false)

  override def toString =
    list.foldLeft((0, "")) { (acc, song) ⇒
      acc match {
        case (i, str) ⇒
          val songName = song.name
          (i + 1, str + (if (i == currentSongIndex) s"**$songName**" else songName) + ", ")
      }
    }._2

  private[models] def enqueue(songId: Song) = {
    val (newList, newCurrentSongIndex) =
      if (currentSongIndex > 0 && list.size == maxSize) (list drop 1, currentSongIndex - 1) // don't get too big unless not doing so will effectively skip
      else (list, currentSongIndex)
    Playlist(newList enqueue songId, newCurrentSongIndex, true, maxSize)
  }
  private[models] def advance() = {
    if (!hasNext) {
      throw new IndexOutOfBoundsException("end of playlist")
    }
    Playlist(list, currentSongIndex + 1, playing, maxSize)
  }
  private[models] def hasNext = currentSongIndex < list.size - 1
  private[models] def currentSongOpt = if (playing) Some(list(currentSongIndex)) else None
  private[models] def state = (list, currentSongIndex, playing)
  private[models] def isEmpty = list.isEmpty
  private[models] def isPlaying = playing
  private[models] def start = playlist(true)
  private[models] def stop = playlist(false)
  private def playlist(playing: Playing) = Playlist(list, currentSongIndex, playing, maxSize)
}