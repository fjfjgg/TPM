<%@page import="es.us.dit.lti.entity.MgmtUser"%>
<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@taglib prefix="e" uri="owasp.encoder.jakarta"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:useBean id="mgmtUser" type="es.us.dit.lti.entity.MgmtUser"
	scope="session"></jsp:useBean>
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Datos personales</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="js/pass.js"></script>
<script src="../js/move.js"></script>
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div id="loginDiv" class="h1container dialog">
		<h1>
			<a href="../user/menu.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a> 
			Datos personales
		</h1>
		<div class="editfields">
			<div>Nombre del usuario: </div>
			<div>${e:forHtmlAttribute(mgmtUser.username)}</div>
			<div>Nombre completo: </div>
			<div>${e:forHtmlAttribute(mgmtUser.nameFull)}</div>
			<div>Correo electrónico: </div>
			<div>${e:forHtmlAttribute(mgmtUser.email)}</div>
			<div>Tipo de usuario: </div>
			<c:set var = "usertype" scope = "page" value = "${mgmtUser.type.toString()}"/>
			<div>${text[usertype]}</div>
		</div>
		<h2>Cambiar contraseña</h2>
		<form method="post" action="change">
			<input type="hidden" name="launchId" value="${launchId}" />
			<input type="password" name="oldpassword" value="" placeholder="Contraseña actual" required="required"/>
			<input type="password" name="password" id="password" value="" placeholder="Nueva contraseña" required="required"/>
			<input type="password" id="password2"  value="" placeholder="Repita la nueva contraseña" required="required"/>
			<div class="centrado">
			<input id="submit" type="submit" class="accionp" value="Cambiar" />
		</div>
		</form>
	</div>
</body>
</html>
