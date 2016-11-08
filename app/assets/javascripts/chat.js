function chat() {
  $.post(jsRoutes.controllers.MusicRoomController.chat(channelId),
  	{ 
  		"text": $chat.val()
  	},
  	function() { 
  		$chat.val("");
			if ($chat.attr("placeholder")) { 
				$chat.removeAttr("placeholder");
			}
  	});
}
function ifReturnChat(e) {
	var code = e.keyCode ? e.keyCode : e.which;
	if(code === 13 && $chat.val().trim().length !== 0) {
		chat();
	}
}