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
function ifReturnChat(e) {
	var code = e.keyCode ? e.keyCode : e.which;
	if(code === 13 && $chat.val().trim().length !== 0) {
		chat();
	}
}