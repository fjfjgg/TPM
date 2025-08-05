<?xml version="1.0" encoding="UTF-8" ?>
<%@page import="es.us.dit.lti.persistence.IDbUtil"%>
<%@page import="es.us.dit.lti.persistence.SettingsDao"%>
<%@page import="es.us.dit.lti.entity.Tool"%>
<%@page import="java.util.List"%>
<%@page import="es.us.dit.lti.persistence.ToolDao"%>
<%@page import="es.us.dit.lti.entity.Settings"%>
<%@page import="org.owasp.encoder.Encode"%>
<%@page import="es.us.dit.lti.entity.MgmtUserType"%>
<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<jsp:useBean id="mgmtUser" type="es.us.dit.lti.entity.MgmtUser" scope="session"></jsp:useBean>
<%
boolean toolExist = false;
List<Tool> tools = ToolDao.getAll(mgmtUser);
if (!tools.isEmpty()) {
	toolExist = true;
}
boolean updated = false;
//Update if params are received
String param = request.getParameter("appName");
if (param != null) {
	Settings.setAppName(param);
	updated = true;
}
if (updated) {
	param = request.getParameter("defaultCssPath");
	if (param != null) {
		if (param.isEmpty())
			Settings.setDefaultCssPath(null);
		else
			Settings.setDefaultCssPath(param);
	}
	param = request.getParameter("notice");
	if (param != null) {
		if (param.isEmpty())
			Settings.setNotice(null);
		else
			Settings.setNotice(param);
	}
	if (!toolExist) {
		param = request.getParameter("toolsFolder");
		if (param != null) {
			Settings.setToolsFolder(param);
		}
		param = request.getParameter("correctorFilename");
		if (param != null) {
			Settings.setCorrectorFilename(param);
		}
	}
	int paramInt;
	param = request.getParameter("maxUploadSize");
	try {
		paramInt = Integer.parseInt(param);
		Settings.setMaxUploadSize(paramInt);
	} catch (NumberFormatException e) {
		paramInt = 0;
	}
	param = request.getParameter("concurrentUsers");
	try {
		paramInt = Integer.parseInt(param);
		Settings.setConcurrentUsers(paramInt);
	} catch (NumberFormatException e) {
		paramInt = 0;
	}
	SettingsDao.set(); //save in db
	// Restart Dbutil
	String resourceName = getServletContext().getInitParameter("datasourceName");
	IDbUtil appDbUtil = ToolDao.getDbUtil();
	appDbUtil.destroy();
	appDbUtil.init(resourceName);
}
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" >
<meta name="viewport" content="width=device-width, initial-scale=1" >
<title>Ajustes</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="../editor/js/edit.js"></script>
<script src="../js/move.js"></script>
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">
		<h1>
			<a href="../user/menu.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a> 
				Ajustes
		</h1>
		<% 
		//Updated?	
		if (updated) {
			%>
			<h3>Datos guardados</h3>
			<%
		}
		%>
		
		<form method="post" action="" accept-charset="UTF-8">
		<input type="hidden" name="launchId" value="${launchId}" />
		<div class="editfields">
			<div id="lab" title="Nombre de la apliación">Nombre de la aplicación</div>
			<div><input type="text" name="appName" 
					value="<%=Encode.forHtmlAttribute(Settings.getAppName()) %>"
					/></div>
		
			<div id="lab" title="Directorio de las herramientas. Solo se puede editar si no hay herramientas">Directorio de las herramientas</div>
			<div><input type="text" name="toolsFolder" value="<%=Encode.forHtmlAttribute(Settings.getToolsFolder())%>"
			<% if (toolExist) { %>
			 disabled="disabled"
			<% } %> 
			/></div>
		
			<div title="Tamaño máximo de subida (kB)">Tamaño máximo de subida (kB)</div>
			<div><input type="number" name="maxUploadSize" value="<%=Settings.getMaxUploadSize()%>" required="required" /></div>
			
			<div title="Límite de usuarios concurrentes">Usuarios concurrentes</div>
			<div><input type="number" name="concurrentUsers" value="<%=Settings.getConcurrentUsers()%>"  required="required" /></div>

			<div title="Nombre del ejecutable. Solo se puede editar si no hay herramientas">Nombre del ejecutable</div>
			<div><input type="text" name="correctorFilename" value="<%=Encode.forHtmlAttribute(Settings.getCorrectorFilename())%>" 
			<% if (toolExist) { %>
			 disabled="disabled"
			<% } %> 
			/></div>

			<div title="URL de CSS por defecto (pueden ser varias separadas por comas)">Rutas CSS por defecto</div>
			<div class="largeText"><textarea name="defaultCssPath"><%=Settings.getDefaultCssPath()==null? "" : Encode.forHtmlContent(Settings.getDefaultCssPath())%></textarea></div>
			
			<div title="Aviso en formato HTML">Aviso</div>
			<div class="largeText modal-content"><textarea name="notice"><%=Settings.getNotice()==null? "" : Encode.forHtmlContent(Settings.getNotice())%></textarea></div>
			
		</div>
		<div class="centrado">
			<input class="accionp" type="submit" name="submit" value="Guardar" />
		</div>
		</form>
	</div>
</body>
</html>