var player = $("audio");
if (!!EventSource) {
	var songInfoSrc = new EventSource(jsRoutes.controllers.MusicRoomController.sseSongInfos(channelId).url);
	songInfoSrc.addEventListener('message', function(event) {
		currentSongId = event.data;
		player.attr("src", jsRoutes.controllers.MusicRoomController.playSong(currentSongId).url);
		alreadyVoted = false;
	});
} else {
	$("#playerContainer").html("Sorry. This browser doesn't seem to support Server sent event. Check <a href='http://html5test.com/compare/feature/communication-eventSource.html'>html5test</a> for browser compatibility.");
}
player.on("ended", function() {
	currentSongId = null;
});
