package models.playlist

import models.song.Song
import models.auxiliaries.{ PlaylistInfo, PlaylistView, PlayingType, SkippableSong, SongIndicator }

import collection.SeqView

trait PlaylistViewer {
  protected[models] def initPlaylist(pwcsi: PlaylistInfo)
  protected[models] def onSongAdd(song: Song)
  protected[models] def onSongPush(pl: PlaylistInfo)
}

object PlaylistViewer {
  def playlistView(pli: PlaylistInfo): PlaylistView = playlistMap(pli, (s, p) ⇒ (s, p))
  def playlistMap[B](pli: PlaylistInfo, playableSongHandler: (Song, SongIndicator.Value) ⇒ B): SeqView[B,Seq[_]] = indices(pli).map(handle(pli, playableSongHandler))
  def playlistForeach(pli: PlaylistInfo, playableSongHandler: (Song, SongIndicator.Value) ⇒ Unit): Unit = indices(pli).foreach(handle(pli, playableSongHandler))
  private def handle[B](pli: PlaylistInfo, playableSongHandler: (Song, SongIndicator.Value) ⇒ B) = (i: Int) ⇒ playableSongHandler.tupled(playableSong(pli, i))
  private def playableSong(pli: PlaylistInfo, i: Int) = pli match {
    case (playlist, playingIndex, playing) ⇒
			playlist(i) match {
				case (song, skipped) =>
      		(song, if (skipped) SongIndicator.Skipped else if (playing && i == playingIndex) SongIndicator.Current else SongIndicator.Regular)
			}
  }
  private def indices(pli: PlaylistInfo) = pli._1.indices.view
}
