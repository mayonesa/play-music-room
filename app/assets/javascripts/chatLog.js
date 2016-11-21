$(function() {
	if (!!EventSource) {
		var $chatLog = $('#chatLog');
		var $showSongs = $('#showSongs');
		var p, songChatDisplay;
		var chatLogEntry = function(chat) {
			p = '<p id="chatLogItem" class="' + ((chat.isSong) ? 's' : 'notS') + 'ong"';
			if (chat.isSong && !$showSongs.prop('checked')) {
				p += ' style="display: none;"';
			}
			$chatLog.append(p + '>' + chat.author + ': ' + chat.text + '</p>');
		};
		$showSongs.change(function() {
			songChatDisplay = this.checked ? 'block' : 'none';
			$chatLog.children().each(function() {
				if (this.className === 'song') {
					this.style.display = songChatDisplay;
				}
			});
		});
		eventSourceJson(jsRoutes.controllers.MusicRoomController.sseChats(channelId), function(chat) {
			chatLogEntry(chat);
			$chatLog.scrollTop($chatLog.prop('scrollHeight'));
		});
	}
});