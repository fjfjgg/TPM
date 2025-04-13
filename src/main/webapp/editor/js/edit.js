/**
 * Funciones para editar
 */
function textAreaAdjust(element) {
  element.style.height = "1px";
  element.style.height = (12+element.scrollHeight)+"px";
}

function checkToolName() {
	this.value = this.value.trim();
	const pattern = /^[^\\\/\:\*\?\"\'\<\>\|\=\,\ \~\$]+$/;
	if (this.value.match(pattern)) {
		this.setCustomValidity('');
	} else {
		this.setCustomValidity('Caracteres prohibidos: \\/:*?"<>\'|=,~$ y espacio');
	}
}

function checkValidJsonConfig() {
  try {
    JSON.parse(this.value);
    this.setCustomValidity('');
    return true;
  } catch (error) {
    this.setCustomValidity('JSON inválido');
    return false;
  }
}

/////////// Visualización de ficheros

/*Petición HTTP GET */
function ajaxGetTxt(url, fexito, ferror) {
    let xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {
                fexito(xhr.responseText);
            } else {
                ferror(xhr.status);
            }
        }
    };
    xhr.open("GET", url, true);
    xhr.send();
}

/*Actualización de la página en caso de éxito*/
function actualiza(txt) {
	let smodal = document.getElementById('src-modal');
    let modalSrc = document.getElementById("src01");
    if (modalSrc.value != undefined) {
		modalSrc.value = txt;
	} else {
		modalSrc.innerHTML="";
		modalSrc.appendChild(document.createTextNode(txt));
	}
	textAreaAdjust(modalSrc);
	centerDiv(smodal);     
}

/*Actualización de la página en caso de error*/
function actualizaError(status) {
    document.getElementById("src01").innerHTML="Error Status: "+status;
}

function saveTextEditor(e) {
	e.preventDefault();
	let targetId = this.dataset.id;
	let element = document.getElementById(targetId);
	let valid = true;
	if (element) {
		let modalSrc = document.getElementById("src01");
		if (this.dataset.type == "json") {
			//Validamos JSON
			valid = checkValidJsonConfig.bind(modalSrc).call();
		}
		if (valid) {
			//Eliminamos lo que contiene el padre y sustituimos por Editado y textarea oculta
			let parent = element.parentElement
			parent.innerHTML = "";
			parent.appendChild(document.createTextNode("Editado"));
			let ta = document.createElement("textarea");
			ta.id=targetId;
			ta.name=targetId;
			ta.style.display="none";
			ta.value=modalSrc.value;
			parent.appendChild(ta);
		}
	}
	if (valid) {
		closeModal();
	} else {
		let modalForm = document.getElementById("modalForm");
		if (modalForm) {
			modalForm.reportValidity();
		}
	}
}

function showModal(e) {
	e.preventDefault();
    e.stopPropagation();
    
    let captionText = document.getElementById("src-modal-caption");
    let saveEditor = document.getElementById("saveEditor");
    document.getElementById('src-modal').style.display = "block";
    captionText.textContent = this.dataset.name;
    if (saveEditor) {
		saveEditor.dataset.id=this.dataset.id;
		saveEditor.onclick=saveTextEditor;
		saveEditor.dataset.origin=this;
		saveEditor.dataset.type=this.dataset.type;
	}
	// si existe un textarea oculto con el id adecuado, los datos se cargan de él,
	// si no se piden con ajax
	let obtener = true;
	if (this.dataset.id) {
		let targetInput = document.getElementById(this.dataset.id);
		if (targetInput && targetInput.style.display == "none") {
			obtener = false;
			actualiza(targetInput.value);
		}
	}
	if (obtener) {
		ajaxGetTxt(this.getAttribute("href"), actualiza, actualizaError);
	}
    
    return false;
}

function closeModal(e) {
	let smodal = document.getElementById('src-modal');
	if (e instanceof KeyboardEvent){
  		if (e.code == 'Escape') {
            smodal.style.display = "none";
        }
	} else {
		smodal.style.display = "none";
	}
}

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
    for (let si of spans ) {
        si.onclick = closeModal;
    }

    // Handle ESC key (key code 27)
    document.addEventListener('keyup', closeModal); 

    let modalSrcContent = document.getElementById("src-modal-content");
    modalSrcContent.style.height="auto";
}

function changeType() {
	//Detección de si hay que cambiar el template
	let editLink = document.getElementById("editCorrector");
	let obtener = false
	if (!editLink) return;
	if (editLink && editLink.href.includes("templates") && editLink.dataset.id) {
		//Detección de si seguimos usando input file
		// si no existe un textarea oculto con el id adecuado
		let targetInput = document.getElementById(editLink.dataset.id);
		if (!targetInput || targetInput.style.display != "none") {
			obtener = true;
		}
		
	}
	//Cambiar URL y tipo según valor
	switch (this.value) {
		case "1":
			if (obtener) {
				editLink.href = "templates/corrector1.sh";
			}
			editLink.dataset.type = "sh";
			break;
		case "2":
			if (obtener) {
				editLink.href = "templates/corrector2.json";
			}
			editLink.dataset.type = "json";
			break;
		case "3":
			if (obtener) {
				editLink.href = "templates/corrector3.json";
			}
			editLink.dataset.type = "json";
			break;
		default:
			if (obtener) {
				editLink.href = "templates/corrector0.txt";
			}
			editLink.dataset.type = "text";
			break;
	}

}

function changeDateTime() {
	let other = document.getElementById(this.id+"Ts");
	if (other) {
		if (this.value) {
			let d = new Date(this.value);
			other.value=d.getTime();
		} else {
			other.value=0;
		}
	}
}

window.addEventListener("load", function() {
	for(let ta of document.querySelectorAll('textarea')) {
		textAreaAdjust(ta);
	}
	let u = document.getElementById("toolname");
	if (u) {
		u.oninput=checkToolName;
	}
	u = document.getElementsByName("jsonconfig");
	for (let jc of u) {
		jc.onchange = checkValidJsonConfig;
	}
	configModal();
	u = document.getElementById("type");
	if (u) {
		u.onchange=changeType;
		changeType.bind(u).call();
	}
	u = document.getElementById("enabledFrom");
	let uTs = document.getElementById("enabledFromTs");
	if (u && uTs) {
		if (uTs.value > 0) {
			let d = new Date();
			d.setTime(uTs.value);
			d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
			u.value = d.toISOString().slice(0,16);
		}
		u.onchange=changeDateTime;
	}
	u = document.getElementById("enabledUntil");
	uTs = document.getElementById("enabledUntilTs");
	if (u && uTs) {
		if (uTs.value > 0) {
			let d = new Date();
			d.setTime(uTs.value);
			d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
			u.value = d.toISOString().slice(0,16);
		}
		u.onchange=changeDateTime;
	}
	
});