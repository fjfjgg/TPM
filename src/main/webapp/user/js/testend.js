
function sendAlert() {
	window.parent.postMessage("end");
}

window.addEventListener("load", function() {
	sendAlert();
});