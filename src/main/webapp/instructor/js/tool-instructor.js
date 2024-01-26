function sendCommand() {
	let launchId = document.getElementById("launchId").value;

	let pPassword = document.getElementById("instructorpass");
	if (pPassword)
		pPassword = pPassword.textContent;
	let pCommand = document.getElementById("command");
	if (pCommand) {
		pCommand = pCommand.value;
		if (pCommand.indexOf("\n") == -1) {
			pCommand += "\n";
		}
	}
	if (!commandFilename) {
		commandFilename = "commandFile.txt"
	}
	let blob = new Blob([pCommand], { type: 'plain/text' });
	let formData = new FormData();
	formData.append("launchId", launchId);
	if (pPassword)
		formData.append("password", pPassword);
	formData.append("upload", blob, commandFilename);

	// Mostramos cargando
	showLoading();
	send(formData);
}

window.addEventListener("load", function() {
	if (enableInstructorCommand) {
		document.getElementById("show").onclick= function() {
			this.nextElementSibling.classList.remove("hidden"); 
			this.className = "hidden"; }; 
		document.getElementById("show").style.cursor="pointer";
		document.getElementById("sendComamnd").onclick=sendCommand;
	}
});
