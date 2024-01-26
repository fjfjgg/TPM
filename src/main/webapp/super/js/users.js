"use strict";
window.addEventListener("load", function() {
	let r = document.querySelectorAll(".userentry");
	for (let n of r) {
		//Seleccionamos radio de su misma fila
		n.onclick = function() {
			let radio = this.children[0].children[0];
			radio.checked = true;
		};
	}
	let f = document.forms[0];
	f.onsubmit=function(event) { 
		return confirm('¿Desea ' + event.submitter.value.toLowerCase() + ' el usuario seleccionado?'); 
	};
});