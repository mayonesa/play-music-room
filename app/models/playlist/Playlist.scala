package models.playlist

import models.song.Song
import models.auxiliaries.{ PlayingType, Playing, DefaultPlaylistSize, PlaylistSong, Skipped, SkippedType, Adder }

import collection.immutable.Queue

private[models] object Playlist {
  private[models] def apply(): Playlist = new Playlist
  private def apply(maxSize: Int): Playlist = new Playlist(maxSize)
  private def apply(list: Queue[PlaylistSong], currentSongIndex: Int, playing: PlayingType, maxSize: Int) = new Playlist(list, currentSongIndex, playing, maxSize)
}

private[models] class Playlist(list: Queue[PlaylistSong], currentSongIndex: Int, playing: PlayingType, maxSize: Int = DefaultPlaylistSize) {
  private def this(maxSize: Int) = this(Queue.empty[PlaylistSong], -1, !Playing, maxSize)
  private def this() = this(Queue.empty[PlaylistSong], -1, !Playing)

  override def toString =
    list.foldLeft((0, "")) { (acc, skippableSong) ⇒
      acc match {
        case (i, str) ⇒
          val songName = skippableSong._1.name
          (i + 1, str + (if (i == currentSongIndex) s"**$songName**" else songName) + ", ")
      }
    }._2

  private[models] def enqueue(song: Song, adder: Adder) = {
    val (newList, newCurrentSongIndex) =
      if (currentSongIndex > 0 && size == maxSize) (list drop 1, currentSongIndex - 1) // don't get too big unless not doing so will effectively skip
      else (list, currentSongIndex)
    Playlist(newList enqueue (song, adder, !Skipped), newCurrentSongIndex, Playing, maxSize)
  }
  private[models] def advance(skipped: SkippedType = false) = {
    if (!hasNext) {
      throw new IndexOutOfBoundsException("end of playlist")
    }
    Playlist(mList(skipped), currentSongIndex + 1, playing, maxSize)
  }
  private[models] def size = list.size
  private[models] def removeAt(i: Int) = Playlist(list.take(i) ++ list.drop(i + 1), currentSongIndex, playing, maxSize)
  private[models] def hasNext = currentSongIndex < size - 1
  private[models] def currentSongOpt = if (playing) Some(list(currentSongIndex)._1) else None
  private[models] def state = (list, currentSongIndex, playing)
  private[models] def isEmpty = list.isEmpty
  private[models] def isPlaying = playing
  private[models] def stop(skipped: SkippedType) = Playlist(mList(skipped), currentSongIndex, !Playing, maxSize)
  private def mList(skipped: SkippedType) =
    if (skipped) {
      val (song, adder, _) = list(currentSongIndex)
      list updated (currentSongIndex, (song, adder, Skipped))
    } else list
}