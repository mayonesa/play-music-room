function chat() {
    $.post(jsRoutes.controllers.MusicRoomController.chat(channelId),
    	{ 
    		"text": $chat.val()
    	},
    	function() { 
    		$chat.val("");
    	}
    );
}
function ifReturnChat() {
	if(event.keyCode == 13) {
		chat();
	}
}