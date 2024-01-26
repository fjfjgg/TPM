"use strict";

async function sendPost(element, url) {
	let info = document.getElementById("messages");
	if (info && info.textContent) {
		info.textContent = "Enviando...";
		//Wait a litte
		await new Promise(r => setTimeout(r, 500));
	}
	//AJAX request
	let urlParams = new URLSearchParams();
	urlParams.append("launchId", document.getElementById("launchId").value);
	
	return await fetch(url, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
		},
		body: urlParams
	}).then(resp => {
		if (!resp.ok) {
			return resp.text().then(text => { throw new Error(text) })
		}
		return resp.text();
	}).then(result => {
		if (result) {
			info.textContent = "OK";
			element.lastResult=true;
		} else {
			info.textContent = "KO";
			element.lastResult=false;
		}
		//console.log("Processed result");
	}).catch(error => {
		if (error instanceof SyntaxError) {
			info.textContent = "Error: sesión cerrada";
		} else if (!error.message.includes("DOCTYPE")) {
			info.textContent = error;
		} else {
			console.log("error with html");
		}
	});
}

async function deleteUnused(event) {
	//console.log("Delete unused: ", this.id, this.href);
	event.preventDefault();
	await sendPost(this, this.href);
	if (this.lastResult) {
		this.style.visibility = "hidden";
		let count = document.getElementById(this.id+'-count');
		if (count) {
			count.textContent = "0";
		}
	}
}

function getUnused() {
	let info = document.getElementById("messages");
	if (info && info.textContent) {
		info.textContent = "";
	}
	//AJAX request
	fetch('getunused', {
		method: 'GET'
	}).then(resp => {
		if (!resp.ok) {
			return resp.text().then(text => { throw new Error(text) })
		}
		return resp.json();
	}).then(result => {
		renderNumbers(result);
	}).catch(error => {
		if (error instanceof SyntaxError) {
			info.textContent = "Error: sesión cerrada";
		} else if (!error.message.includes("DOCTYPE")) {
			info.textContent = error;
		} else {
			console.log("error with html");
		}
	});
}

function renderNumbers(json) {
	for (let p in json) {
		let count = document.getElementById(p+'-count');
		if (count) {
			count.textContent = json[p];
		}
		let actionElement = document.getElementById(p);
		if (actionElement) {
			if (json[p] > 0) {
				actionElement.style.visibility = "visible";
			} else {
				actionElement.style.visibility = "hidden";
			}
		}
	}
}

async function optimize() {
	await sendPost(this, this.id);
	if (this.lastResult) {
		this.disabled=true;
	}
}

window.addEventListener("load", function() {
	let rbs = document.querySelectorAll(".faction");
	for (let i of rbs) {
	  i.onclick = deleteUnused;
	  i.style.visibility="hidden";
	}
	let aux = document.getElementById("add");
	if (aux) {
		aux.onclick = getUnused;
	}
	aux = document.getElementById("optimize");
	if (aux) {
		aux.onclick = optimize;
	}
	getUnused();
});