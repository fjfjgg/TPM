"use strict";
function muestraOpcional() {
  let l = document.getElementById("is_local");
  let e = document.getElementById("exe_restrictions");
  if (this.value=="admin") {
    l.style.visibility="visible";
    e.style.visibility="visible";
  } else {
    l.style.visibility="hidden";
    e.style.visibility="hidden";
  }
}

function checkUsername() {
	let res = false;
	const username = document.getElementById("username");
	username.value = username.value.trim();
	const pattern = /^[^\\\/\:\*\?\"\'\<\>\|\=\,\ \~\$]+$/;
	if (username.value.match(pattern)) {
		username.setCustomValidity('');
		res = true;
	} else {
		username.setCustomValidity('Caracteres prohibidos: \\/:*?"<>\'|=,~$ y espacio');
	}
	return res;
}

window.addEventListener("load", function() {
	let rbs = document.querySelectorAll("input[name=type]");
	for (let i of rbs) {
	  i.onclick = muestraOpcional;
	  if (i.checked) {
		  muestraOpcional.call(i);
	  }
	}
	let badd = document.getElementById("add");
	badd.onclick=function () { checkpassword(password,password2); };
	let p2 = document.getElementById("password2");
	p2.oninput=function () { checkpassword(password,this); };
	let u = document.getElementById("username");
	u.oninput=checkUsername;
});