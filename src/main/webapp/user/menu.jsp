<%@page import="es.us.dit.lti.Messages"%>
<%@page import="es.us.dit.lti.entity.MgmtUser"%>
<%@page import="es.us.dit.lti.entity.Settings"%>
<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />
<!DOCTYPE html>
<html lang="es">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" >
	<meta name="viewport" content="width=device-width, initial-scale=1" >
	<title>Herramienta de corrección</title>
	<link rel="stylesheet" type="text/css" href="../css/style.css" >
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container">
		<%
		MgmtUser currentUser = (MgmtUser) session.getAttribute("mgmtUser");
		//Vemos si hay algún mensaje de alguna pantalla anterior	
		if (session.getAttribute(Settings.PENDING_MSG_ATTRIB) != null) {
			%>
			<h3><%=text.get(session.getAttribute(Settings.PENDING_MSG_ATTRIB))%></h3>
			<%
			session.setAttribute(Settings.PENDING_MSG_ATTRIB, null);
		}
		%>
	</div>
	
</body>
</html>
