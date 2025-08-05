<%@page import="es.us.dit.lti.config.ToolUiConfig"%>
<%@page import="es.us.dit.lti.runner.ToolRunnerType"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="mgmtUser" type="es.us.dit.lti.entity.MgmtUser" scope="session" />
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Nueva herramienta</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="../editor/js/edit.js"></script>
<script src="../js/move.js"></script>
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">
		
		<h1>
			<a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
			Nueva herramienta
		</h1>
		
		<form method="post" enctype="multipart/form-data" action="createTool" accept-charset="UTF-8">
			<input type="hidden" name="launchId" value="${launchId}" />
			<div class="editfields">
				<div id="lab" title="Nombre de la herramienta">Nombre</div>
				<div><input type="text" name="toolname"
					value="" required="required"  title="Nombre de la herramienta"/></div>

				<div title="Descripción">Descripción interna</div>
				<div><input type="text" name="description" 
				value="" required="required"  title="Descripción"/></div>

				<div title="Clave para hacer una entrega. Si está vacía no se pedirá nada.">Clave de entrega</div>
				<div><input type="text" name="deliveryPassword"
					value="" title="Clave para hacer una entrega. Si está vacía no se pedirá nada."/></div>

				<div title="Marcar para habilitar">Habilitar</div>
				<div><input id="enabled" type="checkbox" name="enabled" value="true" title="Marcar para habilitar"  /> Sí 
					<span title="Fecha desde que estará habilitada">Desde</span>
					<input id="enabledFrom" type="datetime-local" value='' />
					<input id="enabledFromTs" type="hidden" name="enabledFrom" value='0' />
					<span title="Fecha hasta que estará habilitada">Hasta</span>
					<input id="enabledUntil" type="datetime-local" value='' />
					<input id="enabledUntilTs" type="hidden" name="enabledUntil" value='0' />
				</div>

				<div title="Marcar para enviar calificación">Enviar calificación</div>
				<div><input id="outcome" type="checkbox" name="outcome"
					value="true" title="Marcar para enviar calificación" />Sí</div>

				<div title="Configuración adicional de la interfaz y la entrega">Configuración JSON</div>
				<div><textarea name="jsonconfig" title="Configuración adicional de la interfaz y la entrega"><%=new ToolUiConfig().toString() %></textarea></div>

				<div title="Descripción en formato HTML">Descripción para usuarios</div>
				<div>
					<a id="editDescription" href="templates/description.html"
					class="material-icons a-src-modal" title="Editar como texto" 
						data-name="Descripción para usuarios" data-id="descriptionfile" data-type="html">edit</a>
					<span><input type="file" id="descriptionfile" name="descriptionfile" required="required" title="Descripción en formato HTML"><br /></span>
				</div>
				
				<div title="Zip con recursos extra referenciados en la descripción (ruta relativa 'extra/')">ZIP con recursos extra</div>
				<div>
					<span><input type="file" id="extrazipfile" name="extrazipfile" title="ZIP con recursos referenciados por la descripción (ruta relativa 'extra/')"><br /></span>
				</div>

				<div title="Modo en la que se ejecuta la herramienta">Tipo de herramienta</div>
				<div> <select name="tooltype" id="type" title="Modo en la que se ejecuta la herramienta" >
				<% for (ToolRunnerType t: ToolRunnerType.values()) {
					if (!t.equals(ToolRunnerType.TR_LOCAL) || mgmtUser.isLocal()) {%>
					<option value="<%=t.getCode() %>"><%=text.get(t.toString()) %></option>
				<% } } %>
				</select></div>

				<div title="Ejecutable/configuración del corrector">Ejecutable/configuración</div>
				<div>
					<a id="editCorrector" href="templates/corrector0.txt"
						class="material-icons a-src-modal" title="Editar como texto" 
						data-name="Ejecutable/configuración del corrector" data-id="correctorfile" data-type="json">edit</a>
					<span><input type="file" id="correctorfile" name="correctorfile" required="required" title="Ejecutable/configuración del corrector" ><br /></span>
				</div>

				<div title="Argumentos adicionales que se pasarán al ejecutable">Argumentos extra</div>
				<div><textarea name="extraArgs" title="Argumentos adicionales que se pasarán al ejecutable"></textarea></div>

				<div title="Contador de uso. Si está vacío o con valor incorrecto se pone a 0.">Contador</div>
				<div><input type="text" name="counter" title="Contador de uso. Si está vacío o con valor incorrecto se pone a 0." /></div>
			</div>
			<div class="centrado">
				<input class="accionp" type="submit" name="submit" value="Crear" />
			</div>
		</form>
	</div>
	<div id='src-modal' class='modal dialog h1container'>
	   <h1>
	  	 <span class='modal-close material-icons bcerrar'>close</span>
	  	 <span id='src-modal-caption'></span>
	   </h1>
	   <form id="modalForm">
	   <div id='src-modal-content' class='modal-content'><textarea id="src01"></textarea></div>
	   <div class="centrado">
	   	<button id="saveEditor" title="Guardar" class="botonGeneral accionp"><span class="material-icons">save</span></button>
	   </div>
	   </form>
	</div>
</body>
</html>
