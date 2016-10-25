package models.playlist

import models.song.Song
import models.auxiliaries.{ PlaylistInfo, PlaylistView, Playing }

trait PlaylistViewer {
  protected[models] def initPlaylist(pwcsi: PlaylistInfo)
  protected[models] def onSongAdd(song: Song)
  protected[models] def onSongPush(pl: PlaylistInfo)
}

object PlaylistViewer {
  def playlistView(pli: PlaylistInfo) = playlistMap(pli, (s, p) ⇒ (s, p))
  def playlistMap[B](pli: PlaylistInfo, playableSongHandler: (Song, Playing) ⇒ B) = indices(pli).map(handle(pli, playableSongHandler))
  def playlistForeach(pli: PlaylistInfo, playableSongHandler: (Song, Playing) ⇒ Unit) = indices(pli).foreach(handle(pli, playableSongHandler))
  private def handle[B](pli: PlaylistInfo, playableSongHandler: (Song, Playing) ⇒ B) = (i: Int) ⇒ playableSongHandler.tupled(playableSong(pli, i))
  private def playableSong(pli: PlaylistInfo, i: Int) = pli match {
    case (playlist, playingIndex, playing) ⇒
      (playlist(i), playing && i == playingIndex)
  }
  private def indices(pli: PlaylistInfo) = pli._1.indices.view
}
