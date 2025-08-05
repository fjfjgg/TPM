<%@page import="java.nio.charset.StandardCharsets"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="es.us.dit.lti.runner.ToolRunnerType"%>
<%@page import="es.us.dit.lti.entity.Tool"%>
<%@page import="es.us.dit.lti.entity.MgmtUserType"%>
<%@page
	import="es.us.dit.lti.entity.MgmtUser,es.us.dit.lti.persistence.ToolDao,org.owasp.encoder.Encode"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="e" uri="owasp.encoder.jakarta"%>
<jsp:useBean id="mgmtUser" type="es.us.dit.lti.entity.MgmtUser" scope="session" />
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Editar herramienta</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="js/edit.js"></script>
<script src="../js/move.js"></script>
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">
	<%
		request.setCharacterEncoding("UTF-8");
		String toolname = request.getParameter("toolname");
		Tool tool = ToolDao.get(toolname);
		int toolPermission = MgmtUserType.UNKNOWN.getCode();
		if (tool != null)
			toolPermission = ToolDao.getToolUserType(mgmtUser, tool);
		
		if (tool == null || toolPermission > MgmtUserType.EDITOR.getCode()) {
	%>
		<h1><a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
			Error
		</h1>
		<h2>No tiene acceso.</h2>
	<%
	} else {
		session.setAttribute("lasttool", toolname);
		String downloadCorrector = "download?toolname=" + URLEncoder.encode(toolname,StandardCharsets.UTF_8) + "&type=corrector";
		String downloadDescription = "download?toolname=" + URLEncoder.encode(toolname,StandardCharsets.UTF_8) + "&type=description";
		String downloadExtraZip = "download?toolname=" + URLEncoder.encode(toolname,StandardCharsets.UTF_8) + "&type=extra";
		boolean extraZipExists = false;
		java.io.File f = new java.io.File(tool.getExtraZipPath());
		if(f.exists() && !f.isDirectory()) { 
		    extraZipExists = true;
		}
		pageContext.setAttribute("tool", tool);
	%>
	<h1>
		<a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
		Editar herramienta
	</h1>
	<h2>Introduzca los datos de la herramienta "<%=Encode.forHtml(toolname)%>"</h2>
	<form method="post" enctype="multipart/form-data" action="editTool" accept-charset="UTF-8">
		<input type="hidden" name="launchId" value="${launchId}" />
		<div class="editfields">
		<%
		if (toolPermission <= MgmtUserType.ADMIN.getCode()) {
		%>
			<div id="lab" title="Nombre de la herramienta">Nuevo nombre
				<input type="hidden" name="oldname" value="${e:forHtmlAttribute(param.toolname)}" />
			</div>
			<div><input type="text" name="toolname" id="toolname"
				value="${e:forHtmlAttribute(tool.name)}" required="required" /></div>
		<% } else { %>
			<div id="lab" title="Nombre de la herramienta">Nombre
				<input type="hidden" name="oldname" value="${e:forHtmlAttribute(param.toolname)}" />
			</div>
			<div><input type="text" value="${e:forHtmlAttribute(tool.name)}" disabled="disabled" /></div>
		<% } %>
			<div title="Descripción">Descripción interna</div>
			<div><input type="text" name="description" value="${e:forHtmlAttribute(tool.description)}" required="required" /></div>
			
			<div title="Clave para hacer una entrega. Si está vacía no se pedirá nada.">Clave de entrega</div>
			<div><input type="text" name="deliveryPassword" value="${e:forHtmlAttribute(tool.deliveryPassword)}" /></div>
			
			<div title="Marcar para habilitar">Habilitar</div>
			<div><input id="enabled" type="checkbox" name="enabled" value="true" ${tool.enableable ? "checked" : "" } /> Sí 
				<span title="Fecha desde que estará habilitada">Desde</span>
				<input id="enabledFrom" type="datetime-local" value='' />
				<input id="enabledFromTs" type="hidden" name="enabledFrom" value='${tool.enabledFrom != null ? tool.enabledFrom.timeInMillis : "" }' />
				<span title="Fecha hasta que estará habilitada">Hasta</span>
				<input id="enabledUntil" type="datetime-local" value='' />
				<input id="enabledUntilTs" type="hidden" name="enabledUntil" value='${tool.enabledUntil != null ? tool.enabledUntil.timeInMillis : "" }' />
			</div>
				
			<div title="Marcar para enviar calificación">Enviar calificación</div>
			<div><input id="outcome" type="checkbox" name="outcome" value="true" ${tool.outcome ? "checked" : "" } /> Sí </div>
			
			<div title="Configuración adicional de la interfaz y la entrega">Configuración JSON</div>
			<div class="largeText"><textarea name="jsonconfig">${e:forHtmlContent(tool.jsonConfig)}</textarea></div>
			
			<div title="Descripción en formato HTML">Descripción para usuarios</div>
			<div>
				<a id="downDescription" href="<%=downloadDescription%>"
					class="material-icons" target="_blank" title="Descargar" download>download</a>
				<a id="editDescription" href="<%=downloadDescription%>"
					class="material-icons a-src-modal" title="Editar como texto" 
					data-name="Descripción para usuarios" data-id="descriptionfile" data-type="html">edit</a>
				<span><input type="file" id="descriptionfile" name="descriptionfile"><br /></span>
			</div>

			<div title="Zip con recursos extra referenciados en la descripción (ruta relativa 'extra/')">ZIP con recursos extra</div>
			<div>
				<% if (extraZipExists) { %>
				<a id="downExtraZip" href="<%=downloadExtraZip%>"
					class="material-icons" target="_blank" title="Descargar" download>download</a>
				<% } %>
				<span><input type="file" id="extrazipfile" name="extrazipfile" title="ZIP con recursos referenciados por la descripción (ruta relativa 'extra/')"><br /></span>
			</div>

			<div title="Modo en la que se ejecuta la herramienta">Tipo de herramienta</div>
			<div>
				<% if (toolPermission <= MgmtUserType.ADMIN.getCode()) { %>
				<select name="tooltype" id="type">
				<% for (ToolRunnerType t: ToolRunnerType.values()) {
					if (!t.equals(ToolRunnerType.TR_LOCAL) || mgmtUser.isLocal()) {%>
					<option value="<%=t.getCode() %>" <%=tool.getToolType()==t ? "selected" : ""%> > <%=text.get(t.toString()) %></option>
				<% } } %>
				</select>
				<% } else { %>
					<%=text.get(tool.getToolType().toString()) %>
				<% } %>
			</div>
			
			<div title="Ejecutable/configuración del corrector">Ejecutable/configuración</div>
			<div>
				<a id="downCorrector" href="<%=downloadCorrector%>" class="material-icons"
					target="_blank" title="Descargar" download>download</a> 
				<a id="editCorrector" href="<%=downloadCorrector%>"
					class="material-icons a-src-modal" title="Editar como texto" 
					data-name="Ejecutable/configuración del corrector" data-id="correctorfile" data-type="json">edit</a>
				<span><input type="file" id="correctorfile" name="correctorfile"><br /></span>
			</div>
			
			<div title="Argumentos adicionales que se pasarán al ejecutable">Argumentos extra</div>
			<div class="largeText"><textarea name="extraArgs">${e:forHtmlContent(tool.extraArgs)}</textarea></div>
			
			<div title="Contador de uso. Si está vacío o con valor incorrecto no se modifica">Contador</div>
			<div><input type="text" name="counter" placeholder="${tool.counter}" /></div>
			
		</div>
		<div class="centrado">
			<input class="accionp" type="submit" name="submit" value="Guardar" />
			<% if (toolPermission <= MgmtUserType.ADMIN.getCode()) { %>
			<input class="accionp" type="submit" name="duplicate" 
				title="Crear una nueva herramienta a partir de los datos actuales. Debe cambiar el nombre y editar descripción y ejecutable."
				value="Guardar como nuevo" formaction="../admin/createTool" />
			<% } %>
		</div>
		</form>
	</div>
	<div id='src-modal' class='modal h1container dialog'>
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
	<% } %>
	</div>
</body>
</html>