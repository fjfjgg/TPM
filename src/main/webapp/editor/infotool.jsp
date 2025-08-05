<%@page import="java.nio.charset.StandardCharsets"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.io.File"%>
<%@page import="org.apache.commons.io.FileUtils"%>
<%@page import="es.us.dit.lti.entity.Tool"%>
<%@page import="java.util.List"%>
<%@page import="es.us.dit.lti.entity.MgmtUserType"%>
<%@page import="es.us.dit.lti.entity.MgmtUser,es.us.dit.lti.persistence.ToolDao"%>
<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@taglib prefix="e" uri="owasp.encoder.jakarta"%>
<jsp:useBean id="mgmtUser" type="es.us.dit.lti.entity.MgmtUser"	scope="session" />
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Información de la herramienta</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="js/edit.js"></script>
<script src="../js/move.js"></script>
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">

	<%
	//Cogemos las propiedades de la herramienta seleccionado
			//y las utilizamos como valores por defecto
			request.setCharacterEncoding("UTF-8");
			String toolTitle = request.getParameter("toolname");
			Tool tool = ToolDao.get(toolTitle);
			
			if (tool == null || toolTitle == null || ToolDao.getToolUserType(mgmtUser, tool) > MgmtUserType.EDITOR.getCode()) {
	%>
		<h1><a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
			Error
		</h1>
		<h2>Ha habido un problema comprobando si tiene acceso.</h2>
	</div>

<%
} else {
		String downloadCorrector = "../editor/download?toolname=" + URLEncoder.encode(toolTitle,StandardCharsets.UTF_8) + "&type=corrector";
		String downloadDescription = "../editor/download?toolname=" + URLEncoder.encode(toolTitle,StandardCharsets.UTF_8) + "&type=description";
		String downloadExtraZip = "download?toolname=" + URLEncoder.encode(toolTitle,StandardCharsets.UTF_8) + "&type=extra";
		boolean extraZipExists = false;
		java.io.File f = new java.io.File(tool.getExtraZipPath());
		if(f.exists() && !f.isDirectory()) { 
		    extraZipExists = true;
		}
		int lastOcc = toolTitle.length();
		session.setAttribute("lasttool", toolTitle);
		File description = new File(tool.getDescriptionPath());
		File corrector = new File(tool.getCorrectorPath());
		File extraZip = new File(tool.getExtraZipPath());
%>
	
	<h1>
		<a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
		Información de la herramienta
	</h1>
	<h2>Datos</h2>
	<div class="editfields">
		<div id="lab" title="Nombre">Nombre</div>
		<div><e:forHtml value="<%=toolTitle%>" /></div>
		
		<div title="Descripción">Descripción interna</div>
		<div><e:forHtml value="<%=tool.getDescription()%>" /></div>
		
		<div title="Clave para hacer una entrega. Si está vacía no se pedirá nada.">Clave de entrega</div>
		<div><e:forHtml value="<%=tool.getDeliveryPassword()%>" /></div>
		
		<div title="Indica si la herramienta está habilitada">Habilitar</div>
		<div><%=tool.isEnableable() ? "Sí" : "No"%>
			<span title="Fecha desde que estará habilitada"> &nbsp;&nbsp;&nbsp;Desde:
			<input id="enabledFrom" type="datetime-local" value='' disabled="disabled" /></span>
			<input id="enabledFromTs" type="hidden" value='<%=tool.getEnabledFrom() != null ? tool.getEnabledFrom().getTimeInMillis() : "" %>' />
			<span title="Fecha hasta que estará habilitada"> Hasta:
			<input id="enabledUntil" type="datetime-local" value='' disabled="disabled"/></span>
			<input id="enabledUntilTs" type="hidden" name="enabledUntil" value='<%=tool.getEnabledUntil() != null ? tool.getEnabledUntil().getTimeInMillis() : "" %>' />
		</div>
		
		<div  title="Indica si se permite el envío de calificaciones">Enviar calificación</div>
		<div><%=tool.isOutcome() ? "Sí" : "No" %></div>
		
		<div title="Configuración adicional de la interfaz y la entrega">Configuración JSON</div>
		<div class="largeText"><textarea disabled="disabled"><e:forHtmlContent value="<%=tool.getJsonConfig()%>" /></textarea></div>
		
		<div title="Descripción en formato HTML">Descripción para usuarios</div>
		<div><a id="downDescription" href="<%=downloadDescription%>" class="material-icons"
			target="_blank" title="Descargar" download>download</a>
			<a id="seeDescription" href="<%=downloadDescription%>" class="a-src-modal material-icons"
			title="Mostrar" data-name="Descripción para usuarios">visibility</a>
			<%=description.length() %> bytes 
		</div>
		
		<div title="Zip con recursos extra referenciados en la descripción (ruta relativa 'extra/')">ZIP con recursos extra</div>
		<div>
			<% if (extraZipExists) { %>
			<a id="downExtraZip" href="<%=downloadExtraZip%>"
				class="material-icons" target="_blank" title="Descargar" download>download</a>
				<%=extraZip.length() %> bytes 
			<% } %>
		</div>
		
		
		<div title="Modo en la que se ejecuta la herramienta">Tipo de herramienta</div>
		<div> <%=text.get(tool.getToolType().toString())%></div>
		
		<div title="Ejecutable/configuración del corrector">Ejecutable/configuración</div>
		<div><a id="downCorrector" href="<%=downloadCorrector%>" class="material-icons"
			target="_blank" title="Descargar" download>download</a>
			<% if (corrector.length() < 100*1024) { %>
			<a id="seeCorrector" href="<%=downloadCorrector%>" class="a-src-modal material-icons"
			title="Mostrar" data-name="Ejecutable/configuración del corrector">visibility</a>
			<% } %>
			<%=corrector.length() %> bytes
		</div>
			
		<div title="Argumentos adicionales que se pasarán al ejecutable">Argumentos extra</div>
		<div class="largeText"><textarea disabled="disabled"><e:forHtmlContent value="<%=tool.getExtraArgs()%>" /></textarea></div>
		
		<div title="Contador de uso">Contador</div>
		<div><%=tool.getCounter()%></div>
		
		<div title="Tamaño de los datos">Datos</div>
		<%
		File dir = new File(tool.getToolDataPath());
				if (dir.exists() && dir.isDirectory()) {
		%>
		<div><%=FileUtils.sizeOfDirectory(new File(tool.getToolDataPath()))%> bytes</div>
		<%
		} else { 
			dir.mkdirs();
		%> 
		<div>No existen.</div>	
		<% } %>
	</div>
	<h2>Usuarios asignados</h2>
	<%
	List<MgmtUser> users = ToolDao.getUsers(tool);
	%>
	<div class="toolusers scroll50">
		<header>Nombre</header>
		<header>Tipo</header>
	<%
	for (MgmtUser u: users) {
	%>
		<div><e:forHtml value="<%=u.getUsername()%>" /></div>
		<div><%=text.get(u.getType().toString())%></div>
	<%} %>		

	</div>
	</div>
	<div id='src-modal' class='modal h1container dialog'>
	   <h1>
	  	 <span class='modal-close material-icons bcerrar'>close</span>
	  	 <span id='src-modal-caption'></span>
	   </h1>
	   <div id='src-modal-content' class='modal-content'><pre><code id="src01"></code></pre></div>
	</div>
<% } %>
</body>
</html>
