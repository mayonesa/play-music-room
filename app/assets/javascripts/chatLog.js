$(function() {
	if (!!EventSource) {
		var $chatLog = $('#chatLog');
		var chatLogEntry = function(chat) {
			var p = '<p id="chatLogItem"';
			if (chat.isSong) {
				p += ' style="color: yellow;"';
			}
			$chatLog.append(p + '>' + chat.author + ': ' + chat.text + '</p>');
		};
		var chat;
		var chatLogSrc = new EventSource(jsRoutes.controllers.MusicRoomController.sseChats(channelId).url);
		chatLogSrc.addEventListener('message', function(event) {
			chat = JSON.parse(event.data);
			chatLogEntry(chat);
			$chatLog.scrollTop($chatLog.prop('scrollHeight'));
		});
	}
});
