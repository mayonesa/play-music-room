function eventSourceJson(url, f) {
	new EventSource(url.url).addEventListener('message', function(event) {
		f(JSON.parse(event.data));
	});
}
	