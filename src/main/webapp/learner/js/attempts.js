"use strict";

//tool.js must be loaded before
if (!texts) {
	console.error("tool.js must be loaded before");
}

mergeTexts({
	"es": {
		"noAttempts": "No se han encontrado intentos anteriores",
		"attemptSearchId": 'Buscar ID',
		"attemptCancelFilter": 'Cancelar filtro',
		"attemptShowOnlyLast": 'Mostrar solo el último',
		"attemptUser": "Usuario",
		"attemptDate": "Fecha",
		"fileName": "Nombre",
		"attemptActions": "Acciones",
		"attemptDownloadCompress": 'Descargar comprimido',
		"fileDownload": 'Descargar',
		"attemptShowResult": 'Mostrar resultado',
		"score": 'Nota',
		"errorCode": 'El ejecutable de evaluación ha fallado. Error:',
		"errorCode-0": 'No guardada',
		"errorCode-1": 'Guardada',
		"errorCode-102": 'Error guardando nota',
		"errorCode-137": 'Error: la evaluación ha tardado más de lo permitido',
		"errorCode-112": 'Error: se ha producido una excepción durante la evaluación',
		"errorCode-111": 'Error: el ejecutable de evaluación no ha podido ejecutarse',
		"errorCode-113": 'Error: la evaluación no se ha realizado por excederse el número de evaluaciones simultáneas'
	},
	"en": {
		"noAttempts": "No previous attempts found",
		"attemptSearchId": 'Search ID',
		"attemptCancelFilter": 'Cancel filter',
		"attemptShowOnlyLast": 'Show only the last',
		"attemptUser": "User",
		"attemptDate": "Date",
		"fileName": "Name",
		"attemptActions": "Actions",
		"attemptDownloadCompress": 'Download compressed',
		"fileDownload": 'Download',
		"attemptShowResult": 'Show result',
		"score": 'Score',
		"errorCode": 'The validator has failed. Error:',
		"errorCode-0": 'Not saved',
		"errorCode-1": 'Saved',
		"errorCode-102": 'Error saving note',
		"errorCode-137": 'Error: Assessment took longer than allowed',
		"errorCode-112": 'Error: An exception occurred during assessment',
		"errorCode-111": 'Error: The assessment executable could not be executed',
		"errorCode-113": 'Error: The assessment has not been carried out due to exceeding the number of simultaneous assessments'
	}
});

function showAttempts() {
	fetch('../learner/listattempts', { method: 'GET' })
		.then(response => {
			if (!response.ok) {
		    	throw new Error(texts.errorClosedSession);
		    }
			return response.json() 
		})
		.then(result => {
			renderAttempts(result);
		})
		.catch(error => {
			let container = document.getElementById("attemptlist");
			if (error instanceof SyntaxError) {
				container.innerHTML="<p class='error'>"+texts.errorClosedSession+"</p>";
			} else {
				container.innerHTML="<p class='error'>"+error+"</p>";
			}
		});
}

function clearSearchId() {
	let i = document.getElementById("searchId");
	if (i) {
		i.value = "";
		renderAttempts();
	}
}

function createAttemptTable() {
	let container = document.getElementById("attemptlist");
	let f = document.createElement("form");
	f.id="fattempts";
	f.method="POST";
	f.action="downloadattempts";
	f.onsubmit=function() {return false;};
	let lid = document.createElement("input");
	lid.type="hidden";
	lid.name="launchId";
	lid.value=document.getElementById("launchId").value;
	f.appendChild(lid);
	let t = document.createElement("table");
	t.id = "tattempts";
	t.innerHTML =  "<thead><tr><th id='idHeader'><input type='text' id='searchId' placeholder='"+texts.attemptSearchId+"' />"
		+"<a class='material-icons haction nopadding' onclick='clearSearchId()' title='"+texts.attemptCancelFilter+"'>cancel</a>"
		+"<a id='onlyLast' class='material-icons haction' title='"+texts.attemptShowOnlyLast+"' onclick='onlyMoreRecent()' >filter_1</a></th>"
		+"<th id='userHeader'>"+texts.attemptUser
		+"</th><th>"+texts.attemptDate
		+"</th><th>"+texts.fileName
		+"</th><th>"+texts.score
		+"</th><th>"+texts.attemptActions
		+" <a id='downloadzip' class='material-icons haction' title='"+texts.attemptDownloadCompress+"' onclick='downloadList()' >download</a>"
		+"<span id='attemptcounter'><span>"
		+"</th></tr></thead>";
	f.appendChild(t);
	container.appendChild(f);
	let routput = document.createElement("div");
	routput.id = "revoutput";
	container.appendChild(routput);
	document.getElementById('searchId').onchange=function() { renderAttempts() };
	return t;
}

function downloadList() {
	let f = document.getElementById("fattempts");
	if (f && f.elements.namedItem("attempt")) {
		f.submit();	
	}
}

function onlyMoreRecent() {
	let tf = document.getElementById("tattempts");
	let counter = document.getElementById("attemptcounter").counter;
	if (tf && tf.tBodies[0]) {
		const attemptMap = new Map();
		const toDelete = [];
		for (const row of tf.tBodies[0].children) {
			let key = row.attempt.id.split('[')[1];
			if (!attemptsDependsOnFilename) {
				key = key.split(']')[0];
			}
			if (attemptMap.has(key)) {
				let previous = attemptMap.get(key);
				if (previous.attempt.timestamp < row.attempt.timestamp) {
					toDelete.push(previous);
					attemptMap.set(key, row);
				} else {
					toDelete.push(row);
				}
			} else {
				attemptMap.set(key,row);
			}
		}
		for (const x of toDelete) {
			x.remove();
			counter--;
		}
	}
	document.getElementById("attemptcounter").counter=counter;
	document.getElementById("attemptcounter").textContent=counter;
} 

function renderAttempts(attempts) {
	let t = document.getElementById('tattempts');
	if (!t) {
		t = createAttemptTable();
	}
	if (!attempts) {
		attempts = t.attempts;
	}
	const search = document.getElementById('searchId').value.trim();
	const regex = new RegExp(search);
	let counter = 0;
	let counterDownload = 0;
	let tbody;
	if (t.tBodies.length == 1) {
		tbody = t.tBodies[0];
	} else {
		tbody = document.createElement("tbody");
		t.appendChild(tbody);
	}
	tbody.innerHTML = "";
	for (const attempt of attempts) {
		if ((attempt.id == search || regex.test(attempt.id)) ) {
			counter++;
			if (attempt.withFile || attempt.withOutput) {
				counterDownload++;
			}
			let row = document.createElement("tr");
			let cell = document.createElement("td");
			let input = document.createElement("input");
			cell.className="attemptid";
			cell.appendChild(document.createTextNode(attempt.id));
			input.type="hidden";
			input.name="attempt";
			input.value=encodeURIComponent(attempt.userId) + "/" + attempt.sid;
			cell.appendChild(input);
			row.appendChild(cell);
			cell = document.createElement("td");
			cell.className="attemptuser";
			cell.appendChild(document.createTextNode(attempt.userId));
			row.appendChild(cell);
			cell = document.createElement("td");
			cell.className="attemptdate";
			cell.appendChild(document.createTextNode(new Date(attempt.timestamp).toLocaleString()));
			row.appendChild(cell);
			cell = document.createElement("td");
			cell.className="filename";
			cell.appendChild(document.createTextNode(attempt.fileName));
			row.appendChild(cell);
			cell = document.createElement("td");
			cell.className="score";
			if (attempt.errorCode>100) {
				cell.appendChild(document.createTextNode(""));
			} else {
				cell.appendChild(document.createTextNode((attempt.score*0.1).toFixed(1)));
			}
			if (texts['errorCode-'+attempt.errorCode]) {
				cell.title=texts['errorCode-'+attempt.errorCode]
			} else {
				cell.title=texts['errorCode']+attempt.errorCode
			}
			if (attempt.errorCode>100) {
				let warningIcon = document.createElement("span");
				warningIcon.className='material-icons';
				warningIcon.appendChild(document.createTextNode('error'));
				cell.appendChild(warningIcon);
			}
			row.appendChild(cell);
			cell = document.createElement("td");
			cell.className="attemptactions";
			let ad =document.createElement("a");
			if (attempt.withFile) {
				ad.className='faction filedownload';
				ad.href="attempt/" + encodeURIComponent(attempt.userId) + "/" + attempt.sid;
				ad.title=texts.fileDownload;
				ad.download=attempt.name
				ad.innerHTML = "<span class='material-icons'>download</span>";	
			} else {
				ad.className='faction fempty';
				ad.title='';
				ad.innerHTML = "<span class='material-icons'>block</span>";
			}
			cell.appendChild(ad);
			ad =document.createElement("a");
			if (attempt.withOutput) {
				ad.className='faction attemptoutput';
				ad.href="attempt/" + encodeURIComponent(attempt.userId) + "/output/" +  attempt.sid;
				ad.title=texts.attemptShowResult;
				ad.innerHTML = "<span class='material-icons'>visibility</span>";
			} else {
				ad.className='faction fempty';
				ad.title='';
				ad.innerHTML = "<span class='material-icons'>block</span>";
			}
			cell.appendChild(ad);
			row.appendChild(cell);
			row.attempt=attempt;
			tbody.appendChild(row);
		}
	}
	if (counter == 0) {
		tbody.innerHTML = "<tr class='tnone'><td>"+texts.noAttempts+"</td></tr>";
		document.getElementById("downloadzip").classList.add("hidden");
		document.getElementById("attemptcounter").textContent='';
	} else {
		document.getElementById("attemptcounter").textContent=counter;
		if (counterDownload == 0) {
			document.getElementById("downloadzip").classList.add("hidden");
		} else {
			document.getElementById("downloadzip").classList.remove("hidden");
		}
	} 
	document.getElementById("attemptcounter").counter=counter;
	
	//Salvamos datos en el contenedor
	t.attempts = attempts;
	activarAcciones();
	//Scroll al final
	let f = document.getElementById("fattempts");
	f.scrollTop = f.scrollHeight;
}

function activarAcciones() {
	let actions = document.querySelectorAll(".attemptoutput");
	for (const a of actions) {
		a.onclick=showOutput;
	}
}

function showOutput(event) {
	event.preventDefault();
	let element = document.getElementById("revoutput");
	let attemptid = this.parentNode.parentNode.children[0].textContent;
	let fs = document.createElement("fieldset");
	fs.id = "output";
	fs.className='infocontainer';
	let l = document.createElement("legend");
	l.appendChild(document.createTextNode(attemptid));
	fs.appendChild(l);
	let divr = document.createElement("div");
	divr.className = "resizer";
	let ifr = document.createElement("iframe");
	ifr.className = "resized outputframe";
	ifr.src = this.href;
	ifr.addEventListener("load", resizeFullHeight);
	divr.appendChild(ifr);
	fs.appendChild(divr);
	element.textContent="";
	element.appendChild(fs);
	addClose(element.firstChild.firstChild);
}

window.addEventListener("load", function() {
	let elem = document.getElementById("showattempts");
	if (elem) {
		elem.addEventListener("click", function() {
			let t = document.getElementById('tattempts');
			if (!t) {
				showAttempts();
			}
		});
	}
});
