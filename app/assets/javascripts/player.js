$(function() {
	if (!!EventSource) {
		var $player = $('audio');
		$player.on('ended', function() { currentSongId = null; });
		var songPlay;
		var songInfoSrc = new EventSource(jsRoutes.controllers.MusicRoomController.sseSongInfos(channelId).url);
		songInfoSrc.addEventListener('message', function(event) {
			songPlay = JSON.parse(event.data);
			currentSongId = songPlay.songId;
			$player.attr('src', jsRoutes.controllers.MusicRoomController.playSong(currentSongId).url + '#t=' + songPlay.startTimeInSecs);
			onSongPlay();
		});
	} else {
		$('#playerContainer').html("Sorry. This browser doesn't seem to support Server-Sent Events. Check <a href='http://html5test.com/compare/feature/communication-eventSource.html'>html5test</a> for browser compatibility.");
	}
});
