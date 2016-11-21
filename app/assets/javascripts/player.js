$(function() {
	if (!!EventSource) {
		var STOP = "stop";
		var $player = $('audio');
		var player = $player[0];
		var playUrl, startOnSecs;
		var stopDownloading = function() {
			player.removeAttribute('src');
			player.load();
		};
		var stop = function() {
			currentSongId = null;
			stopDownloading();
			player.pause();
		};

		eventSourceJson(jsRoutes.controllers.MusicRoomController.sseSongInfos(channelId), function(songPlay) {
			if (songPlay === STOP) {
				stop();
			} else {
				currentSongId = songPlay.songId;
				playUrl = jsRoutes.controllers.MusicRoomController.playSong(currentSongId).url;			
				startOnSecs = songPlay.startTimeInSecs;
				if (startOnSecs !== 0) {
					playUrl += '#t=' + startOnSecs;
				}
				$player.attr('src', playUrl);
				onSongPlay();
			}
		});
	} else {
		$('#playerContainer').html("Sorry. This browser doesn't seem to support Server-Sent Events. Check <a href='http://html5test.com/compare/feature/communication-eventSource.html'>html5test</a> for browser compatibility.");
	}
});
