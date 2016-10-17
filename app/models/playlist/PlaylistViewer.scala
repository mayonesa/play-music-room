package models.playlist

import models.song.Song
import models.auxiliaries.{ PlaylistWithCurrentSongIndex, PlaylistView, Playing }

import collection.immutable.Queue
import play.api.Logger

trait PlaylistViewer {
  private val logger = Logger(getClass)
  private var _playlist = Queue.empty[Song]
  private var _currentSongIndex: Int = _
  protected def postPlaylistInit(): Unit
  protected def postSongAdd(song: Song): Unit
  protected def postSongPush(): Unit
  private[models] def initPlaylist(pwcsi: PlaylistWithCurrentSongIndex): Unit = {
    pwcsi match {
      case (playlist, currentSongIndex) ⇒
        if (!_playlist.isEmpty) {
          throw new IllegalStateException("Playlist viewer already initialized")
        }
        _playlist = playlist
        _currentSongIndex = if (playlist.isEmpty) -1 else currentSongIndex // in the beginning, the source playlist current song index is a singularity
    }
    if (!_playlist.isEmpty) {
      postPlaylistInit()
    }
  }
  private[models] def onSongAdd(song: Song): Unit = {
    _playlist = playlist enqueue song
    postSongAdd(song)
  }
  private[models] def onSongPush(song: Song): Unit = {
    _currentSongIndex = playlist.indexWhere(_ == song, _currentSongIndex + 1)
    postSongPush()
  }
  def playlistView: PlaylistView = playlistMap((s, p) ⇒ (s, p))
  protected[models] def playlistForeach(playableSongHandler: (Song, Playing) ⇒ Unit) = indices.foreach(handle(playableSongHandler))
  protected[models] def playlistMap[B](playableSongHandler: (Song, Playing) ⇒ B) = indices.map(handle(playableSongHandler))
  protected[models] def playlist: Queue[Song] = _playlist
  protected[models] def currentSong: Song = playlist(currentSongIndex)
  protected[models] def currentSongIndex = _currentSongIndex
  private def handle[B](playableSongHandler: (Song, Playing) ⇒ B) = (i: Int) ⇒ playableSongHandler.tupled(playableSong(i))
  private def playableSong(i: Int) = (playlist(i), i == currentSongIndex)
  private def indices = playlist.indices.view
}
