$(function() {
	if (!!EventSource) {
		var KILL_SONG_ID = -888;
		var $player = $('audio');
		var player = $player[0];
		var songPlay, playUrl, startOnSecs;
		var songInfoSrc = new EventSource(jsRoutes.controllers.MusicRoomController.sseSongInfos(channelId).url);
		var stopDownloading = function() {
			player.removeAttribute('src');
			player.load();
		};
		var stop = function() {
			currentSongId = null;
			stopDownloading();
			player.pause();
		};

		songInfoSrc.addEventListener('message', function(event) {
			songPlay = JSON.parse(event.data);
			if (songPlay.songId === KILL_SONG_ID) {
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
