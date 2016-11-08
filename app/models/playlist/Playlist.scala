package models.playlist

import models.song.Song
import models.auxiliaries.{ PlayingType, Playing, DefaultPlaylistSize, SkippableSong, Skipped, SkippedType }

import collection.immutable.Queue

private[models] object Playlist {
  private[models] def apply(): Playlist = new Playlist
  private def apply(maxSize: Int): Playlist = new Playlist(maxSize)
  private def apply(list: Queue[SkippableSong], currentSongIndex: Int, playing: PlayingType, maxSize: Int) = new Playlist(list, currentSongIndex, playing, maxSize)
}

private[models] class Playlist(list: Queue[SkippableSong], currentSongIndex: Int, playing: PlayingType, maxSize: Int = DefaultPlaylistSize) {
  private def this(maxSize: Int) = this(Queue.empty[SkippableSong], -1, !Playing, maxSize)
  private def this() = this(Queue.empty[SkippableSong], -1, !Playing)

  override def toString =
    list.foldLeft((0, "")) { (acc, skippableSong) ⇒
      acc match {
        case (i, str) ⇒
          val songName = skippableSong._1.name
          (i + 1, str + (if (i == currentSongIndex) s"**$songName**" else songName) + ", ")
      }
    }._2

  private[models] def enqueue(song: Song) = {
    val (newList, newCurrentSongIndex) =
      if (currentSongIndex > 0 && list.size == maxSize) (list drop 1, currentSongIndex - 1) // don't get too big unless not doing so will effectively skip
      else (list, currentSongIndex)
    Playlist(newList enqueue (song, !Skipped), newCurrentSongIndex, Playing, maxSize)
  }
  private[models] def advance(skipped: SkippedType = false) = {
    if (!hasNext) {
      throw new IndexOutOfBoundsException("end of playlist")
    }
    Playlist(mList(skipped), currentSongIndex + 1, playing, maxSize)
  }
  private[models] def hasNext = currentSongIndex < list.size - 1
  private[models] def currentSongOpt = if (playing) Some(list(currentSongIndex)._1) else None
  private[models] def state = (list, currentSongIndex, playing)
  private[models] def isEmpty = list.isEmpty
  private[models] def isPlaying = playing
  private[models] def stop(skipped: SkippedType) = Playlist(mList(skipped), currentSongIndex, !Playing, maxSize)
  private def mList(skipped: SkippedType) = if (skipped) list updated (currentSongIndex, (list(currentSongIndex)._1, Skipped)) else list
}