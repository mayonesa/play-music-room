$(function() {
	if (!!EventSource) {
		var CLEAR_PLAYLIST_SONG_ID = -999;
		var $playlist = $('#playlist');
		var song;
		var playlistAddsSrc = new EventSource(jsRoutes.controllers.MusicRoomController.ssePlaylistAdds(channelId).url);
		playlistAddsSrc.addEventListener('message', function(event) {
			song = JSON.parse(event.data);
			if (song.id === CLEAR_PLAYLIST_SONG_ID) {
				$playlist.empty();
			}
			else {
				var li = '<li id="' + song.id + '" class="' + song.indicator + 'Song">' + song.name + ' - ' + song.artist + ' (' + song.duration + ')</li>';
				$playlist.append(li);
				$playlist.scrollTop($playlist.prop('scrollHeight'));
			}
		});
	}
});