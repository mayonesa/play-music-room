$(function() {
	if (!!EventSource) {
		var $chatLog = $('#chatLog');
		var chat;
		var chatLogSrc = new EventSource(jsRoutes.controllers.MusicRoomController.sseChats(channelId).url);
		chatLogSrc.addEventListener('message', function(event) {
			chat = JSON.parse(event.data);
			$chatLog.append('<p id="chatLogItem">' + chat.author + ': ' + chat.text +  '</p>');
			$chatLog.scrollTop($chatLog.prop('scrollHeight'));
		});
	}
});
