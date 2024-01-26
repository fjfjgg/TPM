function inmediateUpload() {
	let progress = document.getElementById("textoProgreso");
	let blob = new Blob([""], { type: 'plain/text' });
	let formData = new FormData();
	formData.append("launchId", progress.className);
	formData.append("upload", blob, "empty");
	let xmlhttp = new XMLHttpRequest();
	// Preparamos la función que se ejecutará cuando acabe la petición async
	xmlhttp.onreadystatechange = function() {
		if (xmlhttp.readyState == xmlhttp.DONE && xmlhttp.status == 200) {
			// Actualizamos el resultado
			if (xmlhttp.responseText === "") {
				errorRedirect();
			} else {
				// Iniciamos la redirección
				try {
					initRedirect(JSON.parse(xmlhttp.responseText));
				} catch (e) {
					console.log(e);
					errorRedirect();
				}
			}
		}
	};
	xmlhttp.onerror = function() {
		// Actualizamos el resultado
		progress.innerHTML = "Error en la redirección.";
	};
	xmlhttp.open("POST", "../learner/assess", true);
	xmlhttp.send(formData);
}

function errorRedirect() {
	document.getElementById("textoProgreso").innerHTML = "Error cargando la página.";
}

function redirect(xhr, data) {
	let loc = xhr.getResponseHeader("X-Location"); //custom
	if (!loc) {
		if (xhr.getResponseHeader("Content-Type").startsWith("application/json")) {
			let j = JSON.parse(xhr.responseText);
			if ('redirectURI' in j) {
				loc = j.redirectURI;
			}
		} else if (xhr.responseURL) {
			loc = xhr.responseURL;
		}
	}
	if (loc) {
		console.log("Redirigir a loc", loc);
		if (data.base) {
			if (!loc.startsWith("/") && loc.indexOf('http://') === -1 && loc.indexOf('https://') === -1) {
				loc = data.base + loc;  //Relativa
				console.log("Usando base");
			}
		}
		window.location.href = loc;
	} else {
		console.log("Redirigir a ", data.url);
		window.location.href = data.url;
	}
}


function initRedirect(data) {
	//{url: "https://servidor/alias/echo.php?hola=test", base: "https://servidor/alias/"
	//  headers: { xxx: "valor"}, oauth2: {token_type: "Bearer", access_token: "valor"}, content: "", redirect: true}
	//Si content está vacío se hace GET, sino POST. Si content es string, se mete tal cual, sino se supone que es un objeto con
	// parámetros y sus valores.
	//console.log(data);
	if (data.redirect) {
		let xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function() {
			if (xhr.readyState == xhr.DONE) {
				if (xhr.status == 200) {
					redirect(xhr, data);
				} else { //Error
					errorRedirect();
				}
			}
		};
		if (data.content) {
			xhr.open("POST", data.url, true);
		} else {
			xhr.open("GET", data.url, true);
		}
		xhr.withCredentials = true;
		for (let h in data.headers) {
			xhr.setRequestHeader(h, data.headers[h]);
		}
		if (data.oauth2) {
			if (data.oauth2.token_type.toLowerCase() === "bearer") {
				xhr.setRequestHeader("Authorization", "Bearer "+data.oauth2.access_token);
			}
		}
		let contentText = "";
		if (data.content) {
			if (typeof data.content == "string") {
				contentText = data.content;
			} else {
				xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
				for (let key in data.content)
					contentText += encodeURIComponent(key) + '=' + encodeURIComponent(data.content[key]) + "&";
			}
			xhr.send(contentText);
		} else {
			xhr.send();
		}
	} else {
		//No se utilizan headers ni texto
		window.location.href = data.url;
	}
}

window.addEventListener("load", inmediateUpload);
