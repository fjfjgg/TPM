"use strict";

//tool.js must be loaded before
if (!texts) {
	console.error("tool.js must be loaded before");
}

mergeTexts({
	"es": {
		"attemptDeleted": "borrado",
		"attemptErrorDelete": "no se ha podido borrar",
		"attemptErrorAccess": "Error de acceso",
		"attemptConfirmDelete": "¿Desea borrar la entrega",
		"attemptSelectUserHelp": "Escriba nombre de usuario, * para todos, una lista de nombres separados por coma o déjelo en blanco para mostrar sus intentos",
		"attemptResendAsInstructor": 'Reenviar como profesor',
		"attemptDelete": "Borrar",
		"userInfo": "Mostrar datos del usuario",
		"testUser": "Usuario de prueba",
		"unknownUser": "Usuario desconocido",
		"nameGiven": "Nombre",
		"nameFamily": "Apellidos",
		"nameFull": "Nombre completo",
		"email": "Correo electrónico",
		"lastScore": "Última nota guardada",
		"getScore": "(solicitar nota)"
	},
	"en": {
		"attemptDeleted": "deleted",
		"attemptErrorDelete": "could not be deleted",
		"attemptErrorAccess": "Access error",
		"attemptConfirmDelete": "Do you want to delete the attempt",
		"attemptSelectUserHelp": "Type username, * for everyone, a comma separated list of names or leave blank to show your attempts",
		"attemptResendAsInstructor": 'Reassess as instructor',
		"attemptDelete": "Delete",
		"userInfo": "Show user data",
		"testUser": "Test user",
		"unknownUser": "Unknown user",
		"nameGiven": "Name",
		"nameFamily": "Family name",
		"nameFull": "Full name",
		"email": "Email",
		"lastScore": "Last saved score",
		"getScore": "(retrieve score)"
	}
});

function deleteAttempt(event) {
	event.preventDefault();
	let element = document.getElementById("revoutput");
	let fila = this.parentNode.parentNode;
	let attemptid = fila.children[0].textContent;
	let borrar = confirm(texts.attemptConfirmDelete+" "+attemptid+"?");
	if (borrar) {
		fetch(this.href, { method: 'DELETE',
			headers: {
    			'X-launch-id': document.getElementById("launchId").value
  			}})
			.then(response => {
				if (!response.ok) {
			    	throw new Error(texts.attemptErrorAccess);
			    }
			})
			.then(() => {
				element.textContent = "";
				let p = document.createElement("p");
				p.className = "avisoborrado";
				p.textContent = attemptid + " " + texts.attemptDeleted;
				element.appendChild(document.createElement("br"));
				element.appendChild(p);
				let attempts = fila.parentNode.parentElement.attempts
				for(let i = 0; i < attempts.length; i++) {
				    if(attempts[i].id == attemptid) {
				        attempts.splice(i, 1);
				        break;
				    }
				}
				fila.parentNode.removeChild(fila);
				let counter = document.getElementById("attemptcounter").counter;
				counter--;
				document.getElementById("attemptcounter").counter=counter;
				document.getElementById("attemptcounter").textContent=counter;
			})
			.catch(error => {
				element.textContent = "";
				let pError = document.createElement("p");
				pError.className = "avisoborrado";
				pError.textContent = attemptid + " " + texts.attemptErrorDelete;
				element.appendChild(document.createElement("br"));
				element.appendChild(pError);
				console.log(error);
			});
	}
}

function reassess(event) {
	event.preventDefault();
	let launchId = document.getElementById("launchId").value;

	let pPassword = document.getElementById("instructorpass");
	if (pPassword)
		pPassword = pPassword.textContent;

	let formData = new FormData();
	formData.append("launchId", launchId);
	if (pPassword)
		formData.append("password", pPassword);
	formData.append("sid", this.href.split('/').at(-1));

	// Mostramos cargando
	showLoading();
	send(formData);
}

function changeUser() {	
	const regName = /<.*?>/g;
	let input = this;
	if (!this) {
		input = document.getElementById("selectUser");
	}
	if (!input || (input.lastvalue && input.lastvalue==input.value && this)) {
		return;
	} else {
		input.lastvalue = input.value;
	}
	
	let usuario = input.value.trim().replace(regName,"");
	if (usuario == "\u2800") {
		input.value = "";
		usuario = "";
	}
	if (usuario) {
		fetch('listattempts', { method: 'POST',
			headers: {
    			'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
  			},
  			body: "userId="+encodeURIComponent(usuario)+"&launchId="+encodeURIComponent(document.getElementById("launchId").value) })
			.then(response => {
				if (!response.ok) {
			    	throw new Error(texts.errorClosedSession);
			    }
				return response.json();
			})
			.then(result => {
				renderAttempts(result);
			})
			.catch(error => {
				let container = document.getElementById("attemptlist");
				if (error instanceof SyntaxError) {
					container.innerHTML="<p class='error'>"+texts.errorClosedSession+"</p>";
				} else {
					container.innerHTML="<p class='error'>Error: "+error+"</p>";
				}
			});
	} else {
		showAttempts();
	}
}

async function obtenerUsuarios() {
	let i = document.createElement("input");
	let dl = document.createElement("datalist");
	let iparent = document.getElementById("attempts");
	i.type="text";
	i.placeholder=texts.attemptUser;
	i.id="selectUser";
	i.selectBoxOptions="";
	i.title=texts.attemptSelectUserHelp;
	i.onchange = changeUser;
	i.onkeydown = function(e) { if(e.key == "Enter") this.onchange();};
	i.style.width='calc(100% - 38px)';
	i.setAttribute('list','users');
	dl.id="users";
	
	iparent.insertBefore(dl,document.getElementById("attemptlist"));
	iparent.insertBefore(i,dl);
	fetch("users", { method: 'GET' })
		.then(response => {
			if (!response.ok) {
		    	throw new Error(texts.attemptErrorAccess);
		    }
			return response.json();
		})
		.then(result => {
			let optionList = [ "\u2800" ]; //empty option
			for (let u in result) {
				let text = result[u].sourceId+" <"+result[u].nameFull+">";
				let dlu = document.createElement("option");
				dlu.value=text;
				dl.appendChild(dlu);
				optionList.push(text);
			}
			i.selectBoxOptions=optionList.join(",");
			createEditableSelect(i); 
		})
		.catch(error => {
			console.log(error);
		});
}

function activarAcciones() {
	let actions = document.querySelectorAll(".attemptoutput");
	for (const a of actions) {
		a.onclick = showOutput;
	}
	actions = document.querySelectorAll(".attemptactions");

	for (const a of actions) {
		let enableReassess = false;
		let href = null;
		for (const o of a.children) {
			if (o.classList.contains('filedownload')) {
				enableReassess = true;
				href = o.href;
			}
			if (o.classList.contains('attemptoutput')) {
				if (!href) {
					href = o.href.replace("/output", "");
				}
			}
			if (!href) {
				let attempt = a.parentNode.attempt;
				if(attempt) {
					href = "attempt/" + encodeURIComponent(attempt.userId) + "/" + attempt.sid;
				} else {
					href = "../error404.html";
				}
			}
		}
		let ar = document.createElement("a");
		ar.classList.add("faction");
		if (enableReassess) {
			ar.classList.add("attemptreassess");
			ar.href = href;
			ar.title = texts.attemptResendAsInstructor;
			ar.innerHTML = "<span class='material-icons'>redo</span></a>";
			ar.onclick = reassess;
		} else {
			ar.classList.add("fempty");
			ar.innerHTML = "<span class='material-icons'>block</span></a>";
		}
		a.appendChild(ar);
		
		ar = document.createElement("a");
		ar.classList.add("faction");
		ar.classList.add("attemptdelete");
		ar.href = href;
		ar.title = texts.attemptDelete;
		ar.innerHTML = "<span class='material-icons'>delete</span></a>";
		ar.onclick = deleteAttempt;
		a.appendChild(ar);
		
		ar = document.createElement("a");
		ar.classList.add("faction");
		ar.classList.add("userinfo");
		let hrefcomp = href.split("/")
		ar.href = "attemptdetail/"+hrefcomp[hrefcomp.length-1];
		ar.title = texts.userInfo;
		ar.innerHTML = "<span class='material-icons'>contact_page</span></a>";
		ar.onclick = getUserInfo;
		a.appendChild(ar);
	}
	if (!document.getElementById('selectUser')) {
		obtenerUsuarios();
	}
}


/* Combobox*/	
var selectBoxIds = 0;
var currentlyOpenedOptionBox = null;
var activeOption;

function selectBox_showOptions() {
	let numId = this.id.replace(/[^\d]/g, '');
	let optionDiv = document.getElementById('selectBoxOptions' + numId);
	if (optionDiv.style.display == 'block') {
		optionDiv.style.display = 'none';
		this.textContent="expand_more";
	} else {
		optionDiv.style.display = 'block';
		this.textContent="expand_less";
		if (currentlyOpenedOptionBox && currentlyOpenedOptionBox != optionDiv)
			currentlyOpenedOptionBox.style.display = 'none';
		currentlyOpenedOptionBox = optionDiv;
	}
}

function selectOptionValue(e) {
	let parentNode = this.parentNode.parentNode;
	let textInput = parentNode.getElementsByTagName('INPUT')[0];
	if (e.ctrlKey) {
		let newValue = textInput.value;
		if (newValue == this.textContent) {
			newValue = "";
		} else if (newValue.indexOf(", "+this.textContent)>=0) {
			//remove
			newValue = newValue.replace(", "+this.textContent, "");
		} else if (newValue.indexOf(this.textContent+", ")>=0) {
			//remove
			newValue = newValue.replace(this.textContent+", ", "");
		} else {
			//add
			if (textInput.value && textInput.value.trim()) {
				newValue += ", ";
			} 
			newValue += this.textContent;	
		}
		textInput.value = newValue;
		if (e.altKey) {
			selectBox_showOptions.call(parentNode.querySelector(".selectBoxArrow"));
			textInput.onchange();
		}
	} else {
		textInput.value = this.textContent;
		selectBox_showOptions.call(parentNode.querySelector(".selectBoxArrow"));
		textInput.onchange();
	}
}

function highlightSelectBoxOption() {
	if (this.style.backgroundColor == '#316AC5') {
		this.style.backgroundColor = '';
		this.style.color = '';
	} else {
		this.style.backgroundColor = '#316AC5';
		this.style.color = '#FFF';
	}

	if (activeOption) {
		activeOption.style.backgroundColor = '';
		activeOption.style.color = '';
	}
	activeOption = this;

}

function createEditableSelect(dest) {
	dest.className = 'selectBoxInput';
	let div = document.createElement('DIV');
	div.style.styleFloat = 'left';
	div.style.width = 'calc(100% - 4px)';
	div.style.position = 'relative';
	div.id = 'selectBox' + selectBoxIds;
	let parent = dest.parentNode;
	parent.insertBefore(div, dest);
	div.appendChild(dest);
	div.className = 'selectBox';
	div.style.zIndex = 10000 - selectBoxIds;

	let img = document.createElement('A');
	img.innerHTML = "expand_more";
	img.className = 'genericButtonMI selectBoxArrow material-icons';

	img.onclick = selectBox_showOptions;
	img.id = 'arrowSelectBox' + selectBoxIds;

	div.appendChild(img);

	let optionDiv = document.createElement('DIV');
	optionDiv.id = 'selectBoxOptions' + selectBoxIds;
	optionDiv.className = 'selectBoxOptionContainer';
	optionDiv.style.width = '100%';
	div.appendChild(optionDiv);

	if (dest.selectBoxOptions) {
		let options = dest.selectBoxOptions.split(',');
		let optionsTotalHeight = 0;
		let optionArray = new Array();
		for (let no of options) {
			let anOption = document.createElement('DIV');
			anOption.className = 'selectBoxAnOption';
			anOption.onclick = selectOptionValue;
			anOption.style.width = optionDiv.style.width.replace('px', '') - 2 + 'px';
			anOption.onmouseover = highlightSelectBoxOption;
			anOption.appendChild(document.createTextNode(no));
			
			optionDiv.appendChild(anOption);
			optionsTotalHeight = optionsTotalHeight + anOption.offsetHeight;
			optionArray.push(anOption);
		}
		if (optionsTotalHeight > optionDiv.offsetHeight) {
			for (let no of optionArray) {
				no.style.width = optionDiv.style.width.replace('px', '') - 22 + 'px';
			}
		}
		optionDiv.style.display = 'none';
		optionDiv.style.visibility = 'visible';
	}

	selectBoxIds = selectBoxIds + 1;
}

let cache = {};

function getUserInfo(event) {
	event.preventDefault();
	let userId = this.parentNode.parentNode.children[1].textContent;
	let hrefcomp = this.href.split("/")
	let attemptId = hrefcomp[hrefcomp.length-1];
	
	if(cache[attemptId] !== undefined) {
		showUserInfo(userId, attemptId, cache[attemptId]);
	} else {
		fetch(this.href, { method: 'GET' })
			.then(response => {
				if (!response.ok) {
					throw new Error('Error de acceso');
				}
				return response.json();
			})
			.then(json => {
				cache[attemptId] = json;
				showUserInfo(userId, attemptId, json);
			})
			.catch(error => {
				document.getElementById("revoutput").innerHTML = "<p>" + texts.errorNoResponse + "</p>";
				console.log(error);
			});
	}
	
}

function showUserInfo(userId, attemptId, result) {
	let element = document.getElementById("revoutput");
	let fs = document.createElement("fieldset");
	fs.className='infocontainer';
	fs.id='outputuser';
	let l = document.createElement("legend");
	l.appendChild(document.createTextNode(userId));
	fs.appendChild(l);
	element.innerHTML="";
	element.appendChild(fs);
	let container = fs;
	if ((typeof result)=="string") {
		container.appendChild(document.createTextNode(result));
	} else if (result instanceof Object && 'resourceUser' in result) {
		let listContainer;
		result.resourceUser.user.attemptId = attemptId;
		let rus = [ result.resourceUser ];
		if ('originalResourceUser' in result) {
			result.originalResourceUser.user.attemptId = attemptId;
			result.originalResourceUser.user.original = true;
			listContainer=document.createElement("ul");
			container.appendChild(listContainer);
			container = listContainer;
			container.style.listStyle="square";
			rus.push(result.originalResourceUser);
		}
		
		for (let ui in rus) {
			if (rus.length > 1) {
				container = document.createElement("li");
				container.style.padding="10px";
				container.style.margin="10px";
				container.style.border="1px solid black";
				listContainer.appendChild(container);
			}
			
			if (rus[ui].user.consumer) {
				container.appendChild(document.createTextNode(
					rus[ui].user.consumer.name + " (" + rus[ui].user.consumer.guid + "), "
					+ rus[ui].user.consumer.version));
				container.appendChild(document.createElement("br"));
			}
			if (rus[ui].resourceLink) {
				let title = "[";
				if (rus[ui].resourceLink.context) {
					title += rus[ui].resourceLink.context.title + ": ";
				}
				title += rus[ui].resourceLink.title + "]";
				let te = document.createElement("em");
				te.appendChild(document.createTextNode(title));
				container.appendChild(te);
				container.appendChild(document.createElement("br"));
			} 
			container.appendChild(document.createTextNode( 
				texts.nameGiven + ": " + rus[ui].user.nameGiven ));
			container.appendChild(document.createElement("br"));
			container.appendChild(document.createTextNode( 
				texts.nameFamily + ": " +  rus[ui].user.nameFamily ));
			container.appendChild(document.createElement("br"));
			container.appendChild(document.createTextNode( 
				texts.nameFull + ": " +  rus[ui].user.nameFull ));
			container.appendChild(document.createElement("br"));
			container.appendChild(document.createTextNode( 
				texts.email + ": " +  rus[ui].user.email ));
			container.appendChild(document.createElement("br"));
			//Scores?
			if ( 'resultSourceId' in rus[ui] && outcomeEnabled ) {
				container.appendChild(document.createTextNode( 
					texts.lastScore + ": "));
				if (rus[ui].user.score) {
					container.appendChild(createScoreElement(rus[ui].user.score));
				} else {
					let scoreContainer = document.createElement("a");
					scoreContainer.onclick=function() { getScore(scoreContainer, rus[ui].user)};
					scoreContainer.appendChild(document.createTextNode(texts.getScore));
					scoreContainer.style.cursor="pointer";
					container.appendChild(scoreContainer);
				}
			}
		}
		
	} else {
		container.appendChild(document.createTextNode(texts.unknownUser));
	}
	
	addClose(element.firstChild.firstChild);
}  

function createScoreElement(scores) {
	let element = null;
	if (scores instanceof Array) {
		if (scores.length == 0) {
			element=document.createTextNode("--");
		} else if (scores.length == 1) {
			element=document.createTextNode(scores[0].score);
		} else {
			element=document.createElement("ul");
			element.style.listStyle="disc";
			for (let si in scores) {
				let container = document.createElement("li");
				element.appendChild(container);
				container.appendChild(document.createTextNode(scores[si].contextTitle+" > "+scores[si].resourceTitle+": "));
				if (scores[si].score) {
					container.appendChild(document.createTextNode(scores[si].score));
				} else {
					container.appendChild(document.createTextNode("--"));
				}
			}
		}
	} else {
		element=document.createTextNode(JSON.stringify(score));
	}
	return element
} 


function getScore(element, userData) {
	let parent = element.parentNode;
	let body = "launchId="+encodeURIComponent(document.getElementById("launchId").value);
	console.log(userData);
	if (userData.attemptId) {
		body += "&attemptId="+userData.attemptId;
	}
	if (userData.original) {
		body += "&original="+userData.original;
	}
	if (userData) {
		fetch('../instructor/readscore', { method: 'POST',
			headers: {
    			'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
  			},
  			body: body })
			.then(response => {
				if (!response.ok) {
			    	throw new Error(texts.errorClosedSession);
			    }
				return response.json() 
			})
			.then(result => {
				userData.score=result;
				parent.replaceChild(createScoreElement(result), element);
			})
			.catch(error => {
				if (error instanceof SyntaxError) {
					parent.innerHTML+="<p class='error'>"+texts.errorClosedSession+"</p>";
				} else {
					parent.innerHTML+="<p class='error'>Error: "+error+"</p>";
				}
			});
	} else {
		userData.score="---";
		parent.replaceChild(document.createTextNode("---"), element);
	}
	
	
}

