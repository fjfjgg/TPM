"use strict";
function configModal() {
	// Modales
	let smodal = document.getElementById('src-modal');
	if (!smodal) {
		return;
	}
	// Código
	let enlaces = document.getElementsByClassName('a-src-modal');
	for (let ei of enlaces) {
		ei.setAttribute('disabled', 'disabled');
		ei.addEventListener('click', showModal);
	}

	//Cerrar haciendo clien en X o pulsando ESC
	// Get the <span> element that closes the modal
	let spans = document.getElementsByClassName("modal-close");
	for (let si of spans) {
		si.onclick = closeModal;
	}

	// Handle ESC key (key code 27)
	document.addEventListener('keyup', closeModal);

	let modalSrcContent = document.getElementById("src-modal-content");
	modalSrcContent.style.height = "auto";
}

function showModal(e) {
	e.preventDefault();
	e.stopPropagation();

	let captionText = document.getElementById("src-modal-caption");
	let saveEditor = document.getElementById("saveEditor");
	document.getElementById('src-modal').style.display = "block";
	captionText.innerHTML = "Editar clave";
	//id of row
	let rowId = this.id.substring(5);

	if (saveEditor) {
		saveEditor.dataset.id = rowId;
		saveEditor.dataset.action = this.id;
		saveEditor.onclick = saveKeyEditor;
	}
	actualiza(rowId, this.id);

	return false;
}

function closeModal(e) {
	let smodal = document.getElementById('src-modal');
	if (e instanceof KeyboardEvent) {
		if (e.code == 'Escape') {
			smodal.style.display = "none";
		}
	} else {
		smodal.style.display = "none";
	}
}

async function saveKeyEditor(e) {
	e.preventDefault();
	let targetId = this.dataset.id;
	let oldKey = document.getElementById("key-" + targetId);
	let duplicate = this.dataset.action.startsWith('dup');
	let info = document.getElementById("info-modal");
	if (info.textContent) {
		info.textContent = "Enviando...";
		//Wait a litte
		await new Promise(r => setTimeout(r, 500));
	}

	let valid = true;
	let secret = document.getElementById("formSecret").value;
	let key = document.getElementById("formKey").value;
	if (oldKey) {
		if (valid) {
			if (key) {
				if (!secret) {
					//Secreto requerido si hay clave
					valid = false;
				}
			} else if (duplicate) {
				//Clave requerida si es duplicado
				valid = false;
			}
		}
	}
	let modalForm = document.getElementById("modalForm");
	if(!valid) {
		if (modalForm) {
			info.textContent = "Revise los errores";
			modalForm.reportValidity();
		}
	} else {
		//AJAX request
		let urlParams = new URLSearchParams();
		urlParams.append("launchId", document.getElementById("launchId").value);
		urlParams.append("toolname", document.getElementById("toolname").value);
		if (oldKey.textContent != "" && !duplicate) {
			urlParams.append("oldkey", oldKey.textContent);
		}
		if (key) {
			urlParams.append("key", key);
		}
		urlParams.append("secret", secret);
		urlParams.append("enabled", document.getElementById("formEnabled").checked);
		let aux = document.getElementById("formConsumer").value;
		if (aux) {
			urlParams.append("consumer", aux);
		}
		aux = document.getElementById("formContext").value;
		if (aux) {
			urlParams.append("context", aux);
		}
		aux = document.getElementById("formResourceLink").value;
		if (aux) {
			urlParams.append("link", aux);
		}
		aux = document.getElementById("formAddress").value;
		if (aux) {
			urlParams.append("address", aux);
		}
		fetch('editkey', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
			},
			body: urlParams})
			.then(response => {
				if (!response.ok) {
					return response.text().then(text => { throw new Error(text) })
				}
				return response.text();
			})
			.then(result => {
				if (!result.includes("DOCTYPE")) {
					info.textContent = result;
				}
				setTimeout(function() {modalForm.submit() }, 200);
			})
			.catch(error => {
				if (error instanceof SyntaxError) {
					info.textContent = "Error: sesión cerrada";
				} else if (!error.message.includes("DOCTYPE")) {
					info.textContent = error;
				} else {
					console.log("error with html");
				}
			});
	}
}
/*Actualización de la página en caso de éxito*/
function actualiza(rowId, action) {
	let key = document.getElementById("key-" + rowId).textContent;
	let generated = false;
	let duplicate = action.startsWith('dup');
	if (duplicate || key == null || key == "") {
		document.getElementById("formKeyPrefix").textContent = document.getElementsByName("toolname")[0].value + "_";
		document.getElementById("formKey").value = generateRandom(8);
		if (duplicate) {
			document.getElementById("formKey").required = "required";
		} else {
			document.getElementById("formKey").removeAttribute("required");
		}
		generated = true;
	} else {
		let key_prefix = key.substring(0, key.indexOf('_') + 1);
		let key_suffix = key.substring(key.indexOf('_') + 1);

		document.getElementById("formKeyPrefix").textContent = key_prefix;
		document.getElementById("formKey").value = key_suffix;
	}
	let secret = document.getElementById("secret-" + rowId).textContent;
	if (duplicate || secret == null || secret == "") {
		document.getElementById("formSecret").value = generateRandom(16);
		generated = true;
	} else {
		document.getElementById("formSecret").value = secret;
	}
	let info = document.getElementById("info-modal");
	if (generated) {
		info.textContent = "Se han generado claves y/o secretos aleatorios";
	} else {
		info.textContent = "";
	}
	document.getElementById("formEnabled").checked = (document.getElementById("enabled-" + rowId).textContent == "check");
	document.getElementById("formConsumer").value = document.getElementById("consumer-" + rowId).textContent;
	document.getElementById("formContext").value = document.getElementById("context-" + rowId).textContent;
	document.getElementById("formResourceLink").value = document.getElementById("rl-" + rowId).textContent;
	document.getElementById("formAddress").value = document.getElementById("address-" + rowId).title;
	centerDiv(document.getElementById('src-modal'));
}

function generateRandom(length) {
	let alphabet = "0123456789abcdefghijklmnñopqrstuvwxyzABCDEFGHIJKLMNNOPQRSTUVWXYZ!@#$%^&*().-_/\|+¿?¡=:,;^{}";
	let random = "";
	for (let i = 0; i <= length; i++) {
		let randomNumber = Math.floor(Math.random() * alphabet.length);
		random += alphabet.substring(randomNumber, randomNumber + 1);
	}
	return random;
}

window.addEventListener("load", function() {
	configModal();
});