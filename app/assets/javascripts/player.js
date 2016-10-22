$(function() {
	if (!!EventSource) {
		var KILL_SONG_SECS = -888;
		var $player = $('audio');
		var player = $player[0];
		$player.on('ended', function() { currentSongId = null; });
		var songPlay;
		var songInfoSrc = new EventSource(jsRoutes.controllers.MusicRoomController.sseSongInfos(channelId).url);
		var playUrl;
		var startOnSecs;
		songInfoSrc.addEventListener('message', function(event) {
			songPlay = JSON.parse(event.data);
			startOnSecs = songPlay.startTimeInSecs;
			if (startOnSecs === KILL_SONG_SECS) {
				currentSongId = null;
				player.removeAttribute('src');
				player.load();
				player.pause();
			} else {
				currentSongId = songPlay.songId;
				playUrl = jsRoutes.controllers.MusicRoomController.playSong(currentSongId).url;			
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
