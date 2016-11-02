$(function() {
	if (!!EventSource) {
		var $chatLog = $('#chatLog');
		var $showSongs = $('#showSongs');
		var p;
		var chatLogEntry = function(chat) {
			p = '<p id="chatLogItem" class="' + ((chat.isSong) ? 's' : 'notS') + 'ong"';
			if (chat.isSong && !$showSongs.prop('checked')) {
				p += ' style="display: none;"';
			}
			$chatLog.append(p + '>' + chat.author + ': ' + chat.text + '</p>');
		};
		var chat;
		var songChatDisplay;
		$showSongs.change(function() {
			songChatDisplay = this.checked ? 'block' : 'none';
			$chatLog.children().each(function() {
				if (this.className === 'song') {
					this.style.display = songChatDisplay;
				}
			});
		});
		var chatLogSrc = new EventSource(jsRoutes.controllers.MusicRoomController.sseChats(channelId).url);
		chatLogSrc.addEventListener('message', function(event) {
			chat = JSON.parse(event.data);
			chatLogEntry(chat);
			$chatLog.scrollTop($chatLog.prop('scrollHeight'));
		});
	}
});