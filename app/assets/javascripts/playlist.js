$(function() {
	var $playlist = $('#playlist');
	var playlistAddsSrc = new EventSource(jsRoutes.controllers.MusicRoomController.ssePlaylistAdds(channelId).url);
	playlistAddsSrc.addEventListener('message', function(event) {
		var playableSong = JSON.parse(event.data);
		var liClass;
		if (playableSong.current) {
			liClass = 'currentSong';
		} else {
			liClass = 'playlistItem';
		}
		var li = '<li id="' + playableSong.id + '" class="' + liClass + '">' + playableSong.name + ' - ' + playableSong.artist + ' (' + playableSong.duration + ')</li>';
		$playlist.append(li);
	});
});
function advancePlaylistToNext() {

}