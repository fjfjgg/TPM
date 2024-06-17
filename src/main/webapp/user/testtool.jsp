<%@page import="es.us.dit.lti.SecurityUtil"%>
<%@page import="es.us.dit.lti.entity.Consumer"%>
<%@page import="es.us.dit.lti.entity.ToolKey"%>
<%@page import="es.us.dit.lti.entity.Tool,es.us.dit.lti.entity.Context,es.us.dit.lti.entity.ResourceLink,es.us.dit.lti.ToolSession"%>
<%@page import="es.us.dit.lti.entity.MgmtUserType"%>
<%@page
	import="es.us.dit.lti.entity.MgmtUser,es.us.dit.lti.persistence.ToolDao,org.owasp.encoder.Encode"%>
<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />	
<%
request.setCharacterEncoding("UTF-8");
MgmtUser user = (MgmtUser) session.getAttribute("mgmtUser");
String toolTitle = request.getParameter("toolname");
if (toolTitle == null) {
	toolTitle = (String) session.getAttribute("lasttool");
}
Tool tool = ToolDao.get(toolTitle);
int permisos = ToolDao.getToolUserType(user, tool);
if (permisos <= MgmtUserType.TESTER.getCode()
	&& (request.getParameter("learner")!=null ||
	    request.getParameter("instructor")!=null) ) {
	
	String sessionUser="";
	//User suffixed with test if admin or editor, or tester if not.
	if (permisos==MgmtUserType.ADMIN.getCode()  || permisos==MgmtUserType.EDITOR.getCode() ){
		sessionUser = user.getUsername()+"~test";
	} else if (permisos==MgmtUserType.TESTER.getCode() )  {
		sessionUser = user.getUsername()+"~tester";
	}
	if (request.getParameter("learner")!=null) {
		sessionUser += "L";
	} else {
		sessionUser += "I";
	}
	
	
	//Create fake ToolSession
	ToolSession ts = new ToolSession();
	ts.initTest(tool, (String) session.getAttribute("launchId"), 
			sessionUser, text.get("T_PAGINA_PRUEBAS"),
			request.getParameter("learner")!=null, 
			request.getParameter("instructor")!=null,
			request.getRemoteAddr());
		
	session.setAttribute(ToolSession.class.getName(), ts);
	
	response.sendRedirect("../" + ts.getContinueUrl());
	
} else {

%>
<!DOCTYPE html>
<html lang="es">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" >
	<meta name="viewport" content="width=device-width, initial-scale=1" >
	<title>Prueba de herramientas</title>
	<link rel="stylesheet" type="text/css" href="../css/style.css">
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">
		
		<h1>
			<a href="../user/tools.jsp"><span class="material-icons bcerrar">close</span></a>
			Prueba de herramientas
		</h1>
		<%
		//Cogemos las propiedades de la herramienta seleccionado
		//y las utilizamos como valores por defecto
		if (permisos <= MgmtUserType.TESTER.getCode()) {
			session.setAttribute("lasttool", toolTitle);
		%>

		<h2>
			Probar la herramienta "<%=Encode.forHtml(toolTitle)%>"
		</h2>
		<div class="centrado">
		  <% if (tool.getToolUiConfig().isRedirectMode()) { %>
			<p>
				<a id="test" href="?learner" target="_blank"> <input
					type='button' name='submit'
					value='Probar redirección en nueva ventana como estudiante' /></a>
			</p>
			<p>
				<a id="test" href="?instructor" target="_blank"> <input
					type='button' name='submit'
					value='Probar redirección en nueva ventana como profesor' /></a>
			</p>
		  <% } else { %>
			<p>
				<a id="test" href="?learner" target="_blank"> <input
					type='button' name='submit'
					value='Probar en nueva ventana como estudiante' /></a>
			</p>
			<p>
				<a id="test" href="?instructor" target="_blank"> <input
					type='button' name='submit'
					value='Probar en nueva ventana como profesor' /></a>
			</p>
		  <% } %>
		</div>
	  <% } else {	%>
		<h1>No está autorizado.</h1>
	  <% } %>
	</div>
	<script src="../js/move.js"></script>
</body>
</html>
<%
}
%>
