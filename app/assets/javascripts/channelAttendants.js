$(function() {
	if (!!EventSource) {
		var $attendants = $('#attendants');
		var attendanceUpdate, id, p;
		var attendanceSrc = new EventSource(jsRoutes.controllers.MusicRoomController.sseRoomAttendance(channelId).url);
		attendanceSrc.addEventListener('message', function(event) {
			attendanceUpdate = JSON.parse(event.data);
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