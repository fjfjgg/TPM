function checkpassword(password1, password2) {
	if (password1.value != password2.value) {
		password1.setCustomValidity('Las contraseñas deben coincidir.');
	} else if (password1.value.length < 12) {
		password1.setCustomValidity('La nueva contraseña debe tener al menos 12 caracteres.');
	} else {
		// input is valid -- reset the error message
		password1.setCustomValidity('');
	}
}

window.addEventListener("load", function() {
	let p1 = document.getElementById("password");
	let p2 = document.getElementById("password2"); 
	p2.oninput = function() { checkpassword(p1,p2); };
	document.getElementById("submit").onclick = function() {checkpassword(p1,p2)};
});