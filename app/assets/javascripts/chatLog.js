$(function() {
	var $chatLog = $('#chatLog');
	var chat;
	var chatLogSrc = new EventSource(jsRoutes.controllers.MusicRoomController.sseChats(channelId).url);
	chatLogSrc.addEventListener('message', function(event) {
		chat = JSON.parse(event.data);
		$chatLog.append('<p>' + chat.author + ' (' + chat.timestamp + '): ' + chat.text +  '</p>');
	});
});