$(function() {
	if (!!EventSource) {
		var CLEAR_PLAYLIST_SONG_ID = -999;
		var $playlist = $('#playlist');
		var playlistSong;
		var playlistAddsSrc = new EventSource(jsRoutes.controllers.MusicRoomController.ssePlaylistAdds(channelId).url);
		playlistAddsSrc.addEventListener('message', function(event) {
			playlistSong = JSON.parse(event.data);
			if (playlistSong.id === CLEAR_PLAYLIST_SONG_ID) {
				$playlist.empty();
			}
			else {
				var li = '<li class="' + playlistSong.indicator + 'Song">';
				if (playlistSong.indicator === "Removable") {
					li += '<input type="button" class="removeButton" value="-" onclick="removeSong(' + playlistSong.index + ');" />';
				}
				li += playlistSong.name + ' - ' + playlistSong.artist + ' (' + playlistSong.duration + ')</li>';
				$playlist.append(li);
				$playlist.scrollTop($playlist.prop('scrollHeight'));
			}
		});
	}
});