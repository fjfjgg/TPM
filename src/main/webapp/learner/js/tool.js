//i18n
let TEXTS = {
	"es": {
		"waitMsg": "Espere mientras se carga su trabajo y se evalúa...",
		"loading": "cargando",
		"errorFileName": 'Nombre de archivo no válido',
		"errorSize": 'Tamaño de archivo excesivo',
		"resultTitle": "Resultado",
		"errorEmptyField": 'Este campo no puede estar vacío',
		"errorEmptyText": 'El texto no puede estar vacío',
		"errorNoFile": 'Debe seleccionar un archivo',
		"errorClosedSession": "La sesión se ha cerrado. Vuelva a iniciar la herramienta.",
		"errorNoResponse": "El servidor no responde. Reinténtelo más tarde.",
		"errorRequest": "Error en la petición. Puede que el archivo sea demasiado grande o el servidor no esté operativo.",
		"closeWindow": "Puede cerrar esta ventana/pestaña ahora."
	},
	"en": {
		"waitMsg": "Please wait while your work is uploaded and evaluated...",
		"loading": "loading",
		"errorFileName": 'Invalid filename',
		"errorSize": 'Excessive file size',
		"resultTitle": "Result",
		"errorEmptyField": 'This field can not be blank',
		"errorEmptyText": 'The text can not be empty',
		"errorNoFile": 'You must select a file',
		"errorClosedSession": "The session has been closed. Restart the tool.",
		"errorNoResponse": "The server does not respond. Try again later.",
		"errorRequest": "Error in the request. The file may be too large or the server may not be operational.",
		"closeWindow": "You can close this window/tab now."
	}
}
let texts = TEXTS.es;

function mergeTexts(moreTexts) {
	for( let lang in TEXTS) {
		if (lang in moreTexts)
			Object.assign(TEXTS[lang], moreTexts[lang]);
	}
}

window.onload = function() {
	//lang
	if (document.documentElement.lang in TEXTS) {
		texts = TEXTS[document.documentElement.lang];
	}
	let file = document.getElementById("attemptfile");
	if (file && inputFileAccept) {
		file.accept = inputFileAccept;
	}
	if (document.getElementById("assess"))
		document.getElementById("assess").onclick=validateAndSend;
	
	let l = document.querySelectorAll("legend");
	for (let x of l) {
		addClose(x);
	}
	l = document.querySelectorAll(".toggleButton a");
	for (let x of l) {
		x.addEventListener('click', toggleHidden);
	}
	
	l = document.getElementById("alogout");
	if (l) {
		l.addEventListener('click', logout);
	}
};

function showLoading() {
	let fsDelivery = document.getElementById("fsdelivery");
  	if (fsDelivery && !fsDelivery.classList.contains('hidden'))
    	fsDelivery.firstElementChild.firstElementChild.click();

	let ta = document.getElementById("parentassessment");
	if (ta && !ta.classList.contains('hidden'))
		ta.classList.add('hidden');
	
	let revoutput = document.getElementById("revoutput");
	if (revoutput) {
		revoutput.innerHTML = "";
	}

	let infoLabel = document.getElementById("result");
	infoLabel.innerHTML = "<p>"+texts.waitMsg
		+ "</p><br /><p><img src='../img/loading.gif' alt='"+texts.loading+"' />" +
		"</p>";
	infoLabel.scrollIntoView();
}

function validateFileInputType(element) {
	let res = true;
	if ((element instanceof HTMLInputElement
		&& element.type === 'file'
		&& element.files && element.files.length)) {
		let validPattern = true;
		if (inputFilePattern) {
			let pattern = new RegExp(inputFilePattern);
			// Ensure each of the input's files' types conform to the above
			validPattern = Array.prototype.every.call(element.files,
				function passesAcceptedFormat(file) {
					return pattern.test(file.name);
				});
			if (!validPattern) {
				element.setCustomValidity(texts.errorFileName);
			}

		}
		let validSize = true;
		if (inputFileSize) {
			validSize = Array.prototype.every.call(element.files,
				function passesAcceptedSize(file) {
					return (file.size <= inputFileSize);
				});
			if (!validSize) {
				element.setCustomValidity(texts.errorSize);
			}
		}
		res = (validPattern && validSize);


	}
	return res;
}

function validateAndSend() {

	let valid = true;
	let file = document.getElementById("attemptfile");
	let text = document.getElementById("attempttext");
	let pPassword = document.getElementById("password");

	if (pPassword != null) {
		if (pPassword.value === "") {
			pPassword.setCustomValidity(texts.errorEmptyField);
			valid = false;
		} else {
			pPassword.setCustomValidity('');
		}
	}
	if (text) {
		if (text.value.trim() != '') {
			if (inputFileSize) {
				if (text.value.length <= inputFileSize ) {
					text.setCustomValidity('');
				} else {
					text.setCustomValidity(texts.errorSize);
				}
			}
		} else {
			valid = false;
			text.setCustomValidity(texts.errorEmptyText);
		}
	} else if (file.value != '') {
		if (!validateFileInputType(file)) {
			valid = false;
		} else {
			file.setCustomValidity('');
		}
	} else {
		valid = false;
		file.setCustomValidity(texts.errorNoFile);
	}
	
	if (valid) {
		let launchId = document.getElementById("launchId").value;

		let formData = new FormData();
		formData.append("launchId", launchId);
		if (pPassword)
			formData.append("password", pPassword.value);
		if (text) {
			if(textFilename) {
				formData.append("upload", new Blob([text.value], { type: 'plain/text' }), textFilename);
			} else {
				formData.append("upload", new Blob([text.value], { type: 'plain/text' }), "textFilename.txt");
			}
		} else {
			formData.append("upload", file.files[0]);	
		}
		
		// Mostramos cargando
		showLoading();
		send(formData);
	} else {
		document.getElementById("formdelivery").reportValidity();
		//Muestra los errores
	}
}

function send(formData) {
  
  let xmlhttp = new XMLHttpRequest();
  // Preparamos la función que se ejecutará cuando acabe la petición async
  xmlhttp.onreadystatechange = function() {
    if (xmlhttp.readyState == 4) {
	  if (xmlhttp.status == 200) {
        if (xmlhttp.responseText === "") {
          createResult("<p class='error'>"+texts.errorClosedSession+"</p>");
        } else {
          createResult(xmlhttp.responseText);
        }
      } else {
        createResult("<p class='error'>"+texts.errorNoResponse+"</p>");
	  }
    }
  };
  xmlhttp.onerror = function() {
    createResult("<p class='error'>"+texts.errorRequest+"</p>");
  };
  xmlhttp.open("POST", "../learner/assess", true);
  xmlhttp.send(formData);
}

function createResult(text) {
	let element = document.getElementById("result");
	let fs = document.createElement("fieldset");
	fs.className='infocontainer';
	fs.id="assessment";
	fs.dataset.toggle='parentassessment';
	let l = document.createElement("legend");
	l.appendChild(document.createTextNode(texts.resultTitle));
	fs.appendChild(l);
	element.innerHTML="";
	element.appendChild(fs);
	fs.innerHTML+=text;
	addClose(element.firstChild.firstChild);
	if (typeof changeUser == "function") {
		changeUser();
	} else if (typeof showAttempts == "function") {
		showAttempts();
	}
	element.scrollIntoView();
}

function addClose(legend, onclick) {
  let a = document.createElement("a");
  a.className="material-icons closefs nopadding";
  if (onclick) {
	a.onclick = onclick;
  } else {
    a.onclick=toggleHidden;
  }
  a.textContent="close";
  legend.insertBefore(a,legend.firstChild);
}

function toggleHidden(event) {
	let element = event.currentTarget;
	let toHide;
	let toShow;
	let scroll = false;
	if (element.classList.contains('closefs')) {
		toHide = element.parentNode.parentNode;
		toShow = document.getElementById(toHide.dataset.toggle);
	} else {
		toHide = element.parentNode;
		toShow = document.getElementById(element.dataset.toggle);
		scroll = true;
	}
	if (toShow) {
		toShow.classList.toggle('hidden');
		toHide.classList.toggle('hidden');
		if (scroll)
			toShow.scrollIntoView();
	} else {
		toHide.parentNode.removeChild(toHide);
	}
}

window.addEventListener("load", function() {
	//cuenta atrás
	if (typeof timeLeft !== 'undefined') {
		let countdown = document.getElementById("countdown");
		if (!countdown) {
			let countdownParent = document.getElementById("fsdelivery");
			if (countdownParent) {
				countdown = document.createElement("p");
				countdown.id = "countdown";
				countdownParent.appendChild(countdown);
			}
		}
		if (countdown) {
			let x = setInterval(function() {
				let now = new Date().getTime();
				let distance = (startDate.getTime() + timeLeft - now) / 1000;
				let hours = Math.floor(distance / (60 * 60));
				let minutes = Math.floor((distance % (60 * 60)) / 60);
				let seconds = Math.floor(distance % 60);
				if (hours > 6) {
					clearInterval(x);  //no poner contador cuando queda tanto tiempo
				} else if (distance < 0) {
					clearInterval(x);
					countdown.innerHTML = " 0s";
					countdown.style.color = "red";
					let b = document.getElementById("assess");
					if (b) {
						b.classList.add("disabled");
						b.replaceWith(b.cloneNode(true)); //remove event listeners
					}
				} else if (hours > 0)
					countdown.innerHTML = hours + "h " + minutes + "m " + seconds + "s ";
				else if (minutes > 0)
					countdown.innerHTML = minutes + "m " + seconds + "s ";
				else
					countdown.innerHTML = seconds + "s ";
			}, 1000);
		}
	}
});

function logout(event) {
	let xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (xmlhttp.readyState == 4) {
			console.log("logout");
		}
	};
	xmlhttp.open("GET", "../tools", false);
	xmlhttp.send();
	//try to close windows
	window.close();
	if (this.getAttribute("href")=="") {
		event.preventDefault();
		document.write(texts.closeWindow);
	}
}