$(function() {
	if (!!EventSource) {
		var $attendants = $('#attendants');
		var id, p;
		eventSourceJson(jsRoutes.controllers.MusicRoomController.sseRoomAttendance(channelId), function(attendanceUpdate) {
			id = attendanceUpdate.id;
			if (attendanceUpdate.action === "Add") {
				var p = '<p id="' + id + '" class="attendant';
				if (id === channelId) {
					p += ' me';
				}
				$attendants.append(p + '">' + attendanceUpdate.name + '</p>');
				$attendants.scrollTop($attendants.prop('scrollHeight'));
			} else {
				$attendants.children('#' + id).remove();
			}
		});
	}
});