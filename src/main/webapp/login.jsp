<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:choose>
	<c:when test="${sessionScope.mgmtUser == null}">
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Acceso</title>
<link rel="stylesheet" type="text/css" href="css/style.css">
<script src="js/move.js"></script>
</head>
<body>
	<div id="loginDiv" class="h1container dialog">
		<h1>Acceso</h1>
		<form method="post" action="do/login">
			<label for="user"><b>Usuario</b></label> <input type="text"
				name="user" value="" placeholder="Escribe tu usuario" required /> <label
				for="pass"><b>Contraseña</b></label> <input type="password"
				name="pass" value="" placeholder="Escribe tu contraseña" required />
			<div class="centrado">
				<input class="accionp" type="submit" name=submit value="Acceder" />
			</div>
		</form>
	</div>
</body>
</html>
	</c:when>
	<c:otherwise>
		<c:redirect url="user/menu.jsp" />
	</c:otherwise>
</c:choose>
