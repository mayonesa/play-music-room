$(function() {
	if (!!EventSource) {
		var CLEAR = "clear";
		var $playlist = $('#playlist');
		eventSourceJson(jsRoutes.controllers.MusicRoomController.ssePlaylistAdds(channelId), function(playlistSong) {
			if (playlistSong === CLEAR) {
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