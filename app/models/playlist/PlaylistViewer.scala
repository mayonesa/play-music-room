package models.playlist

import models.song.Song
import models.channel.Channel
import models.auxiliaries.{ PlaylistInfo, PlaylistViewSong, PlaylistViewIndicator }
import PlaylistViewIndicator._

import annotation.tailrec

trait PlaylistViewer {
  protected[models] def initPlaylist(pwcsi: PlaylistInfo)
  protected[models] def onSongAdd(song: Song, adder: Channel, playlistIndex: Int)
  protected[models] def onSongPush(pl: PlaylistInfo)
}

object PlaylistViewer {
  def playlistForeach(pli: PlaylistInfo)(ch: Channel)(f: (PlaylistViewSong) ⇒ Unit): Unit = pli match {
    case (playlist, playingIndex, playing) ⇒
      @tailrec
      def loop(i: Int, future: Boolean): Unit =
        playlist(i) match {
          case (song, addedBy, skipped) ⇒
            val current = playing && i == playingIndex
            val playlistIndicator = if (future) {
              if (ch == addedBy) Removable else Regular
            } else {
              if (skipped) Skipped else if (current) Current else Regular
            }
            f((song, playlistIndicator, i))
            if (i < playlist.size - 1) {
              loop(i + 1, current || future)
            }
        }
      if (!playlist.isEmpty) {
        loop(0, false)
      }
  }
}
