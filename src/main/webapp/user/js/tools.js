function enviar(destino) {
	let r = document.getElementsByName("toolname");
	let checked = false;
	for (let i=0, length=r.length; i< length; i++) {
		if(r[i].checked) {
			checked = true;
			break;
		}
	}
	if (checked) {
		let f = document.getElementById("formulario");
		f.action=destino;
		f.submit();
	} else {
		alert("Seleccione una herramienta.");
	}
	return true;
	
}

function confirmarBorrado() {
	let borrar = confirm("¿Desea borrar la herramienta seleccionada?")
	if (borrar)
		enviar('../admin/deletetool');
}

function confirmarBorradoDatos() {
	let borrar = confirm("¿Desea borrar los datos de la herramienta seleccionada?")
	if (borrar)
		enviar('../admin/deletetooldata');
}


function cambiaModo(event) {
	let elemento = event.target;
	if (document.getElementById("binfo"))
		document.getElementById("binfo").disabled = (elemento.className == "type2");
	if (document.getElementById("bassign"))
		document.getElementById("bassign").disabled = (elemento.className != "type0");
	if (document.getElementById("bunassign"))
		document.getElementById("bunassign").disabled = (elemento.className != "type0");
	if (document.getElementById("bedit"))
		document.getElementById("bedit").disabled = (elemento.className == "type2");
	if (document.getElementById("bkeys"))
		document.getElementById("bkeys").disabled = (elemento.className == "type2");
	if (document.getElementById("bdownload"))
		document.getElementById("bdownload").disabled = (elemento.className == "type2");
	if (document.getElementById("bdelete"))
		document.getElementById("bdelete").disabled = (elemento.className != "type0");
	if (document.getElementById("bdeletedata"))
		document.getElementById("bdeletedata").disabled = (elemento.className == "type2");
	if (document.getElementById("bdisable"))
		document.getElementById("bdisable").disabled = (elemento.className == "type2");
}

window.addEventListener("load", function () {
	//Asignamos acciones a los botones
	if (document.getElementById("binfo"))
		document.getElementById("binfo").onclick=function() {enviar('../editor/infotool.jsp');};
	if (document.getElementById("bassign"))
		document.getElementById("bassign").onclick=function() {enviar('../admin/associate.jsp');};
	if (document.getElementById("bunassign"))
		document.getElementById("bunassign").onclick=function() {enviar('../admin/disassociate.jsp');};
	if (document.getElementById("bedit"))
		document.getElementById("bedit").onclick=function() {enviar('../editor/edittool.jsp');};
	if (document.getElementById("bkeys"))
		document.getElementById("bkeys").onclick=function() {enviar('../editor/editkeys.jsp');};
	if (document.getElementById("bdownload"))
		document.getElementById("bdownload").onclick=function() {enviar('../editor/downloadtool');};
	if (document.getElementById("bdelete"))
		document.getElementById("bdelete").onclick=confirmarBorrado;
	if (document.getElementById("bdeletedata"))
		document.getElementById("bdeletedata").onclick=confirmarBorradoDatos;
	if (document.getElementById("bdisable"))
		document.getElementById("bdisable").onclick=function() {enviar('../editor/disabletoolsessions');};
	if (document.getElementById("btest"))
		document.getElementById("btest").onclick=function() {enviar('../user/testtool.jsp');};	

	//Buscamos elemento radio seleccionado
	let r = document.getElementsByName("toolname");
	for (let ri of r) {
		ri.onchange = cambiaModo;
		if(ri.checked) {
			cambiaModo({target: ri});
			ri.scrollIntoView(false);
		}
	}
	r = document.querySelectorAll(".toolname");
	for (let n of r) {
		//Seleccionamos radio de su misma fila
		n.onclick= function() {
			let radio = this.previousElementSibling.children[0];
			radio.checked=true;
			cambiaModo({target: radio});
		};
	}
});
