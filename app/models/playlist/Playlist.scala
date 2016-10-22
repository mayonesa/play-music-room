package models.playlist

import models.song.Song
import models.auxiliaries.PlaylistWithCurrentSongIndex

import collection.immutable.Queue

private[models] object Playlist {
  private[models] def apply(): Playlist = new Playlist
  private def apply(maxSize: Int): Playlist = new Playlist(maxSize)
  private def apply(list: Queue[Song], currentSongIndex: Int) = new Playlist(list, currentSongIndex)
}

private[models] class Playlist(list: Queue[Song], currentSongIndex: Int, maxSize: Int = 1000) {
  /* starting current song index at 0 because fuzzy origins simplify the logic */
  private def this(maxSize: Int) = this(Queue.empty[Song], 0, maxSize)
  private def this() = this(Queue.empty[Song], 0)

  override def toString =
    list.foldLeft((0, "")) { (acc, song) ⇒
      acc match {
        case (i, str) ⇒
          val songName = song.name
          (i + 1, str + (if (i == currentSongIndex) s"**$songName**" else songName) + ", ")
      }
    }._2

  private[models] def enqueue(songId: Song): Playlist = {
    val (newList, newCurrentSongIndex) =
      if (currentSongIndex > 0 && list.size == maxSize) (list drop 1, currentSongIndex - 1) // don't get too big unless not doing so will effectively skip
      else (list, currentSongIndex)
    Playlist(newList enqueue songId, newCurrentSongIndex)
  }
  private[models] def advance: Playlist = Playlist(list, currentSongIndex + 1)
  private[models] def isPlayable: Boolean = currentSongIndex < list.size
  private[models] def hasNext: Boolean = currentSongIndex < list.size - 1
  private[models] def currentSong: Song = list(currentSongIndex)
  private[models] def state: PlaylistWithCurrentSongIndex = (list, currentSongIndex)
  private[models] def isEmpty: Boolean = list.isEmpty
}