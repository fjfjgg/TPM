<%@page import="es.us.dit.lti.persistence.MgmtUserDao"%>
<%@page import="es.us.dit.lti.entity.MgmtUser"%>
<%@page language="java" contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="user" class="es.us.dit.lti.entity.MgmtUser" scope="page" />
<%
boolean editMode = false;
final String username = request.getParameter("username");
final String formAction;
if (username != null) {
	MgmtUser userAux = MgmtUserDao.get(username);
	if (userAux != null) {
		user.setUsername(username);
		user.setEmail(userAux.getEmail());
		user.setExecutionRestrictions(userAux.getExecutionRestrictions());
		user.setLocal(userAux.isLocal());
		user.setNameFull(userAux.getNameFull());
		user.setType(userAux.getType());
	}
}
if (user.getUsername() == null) {
	formAction = "adduser";
} else {
	formAction = "edituser";
	editMode = true;

}
pageContext.setAttribute("formAction", formAction);
pageContext.setAttribute("editMode", editMode);
%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" >
<meta name="viewport" content="width=device-width, initial-scale=1" >
<title>Registro de usuarios</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="../user/js/pass.js"></script>
<script src="js/adduser.js"></script>
<script src="../js/move.js"></script></head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">
		<h1>
			<a href="../super/users.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a> 
				Datos del usuario
		</h1>
		<form method="post" id="addForm" action="${formAction}" accept-charset="UTF-8">
			<input type="hidden" name="launchId" value="${launchId}" />
			<div class="editfields">
				<div><label for="username">Nombre del usuario</label></div>
				<div>
				<% if (editMode) { %>
					<input type="hidden" name="username" value="${user.username}" />
					<span>${user.username}</span>					
				<% } else { %>
					<input type="text" name="username" id="username" value="" placeholder="Nombre del usuario" required="required"  />
				<% } %>
				</div>
				<div><label for="fullname">Nombre completo</label></div>
				<div>
					<input type="text" name="fullname" id="fulname" value="${user.nameFull}" placeholder="Nombre completo" />
				</div>
				<div><label for="username">Correo electrónico</label></div>
				<div>
					<input type="email" name="email" id="email" value="${user.email}" placeholder="Correo electrónico" pattern="[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$" />
			    </div>
				<div><label for="password">Contraseña</label></div>
				<div>
					<input type="password" name="password" id="password" value="" 
			    		placeholder="Contraseña" ${editMode ? "": "required='required'"} autocomplete="new-password" /> 
				</div>
				<div><label for="password2">Repita la contraseña</label></div>
				<div>
					<input type="password" id="password2" value="" placeholder="Repita la contraseña" ${editMode? "" : "required='required'"} />
				</div>
				<div><label for="type" title="Los administradores pueden crear, editar, asignar y borrar herramientas. Los usuarios editores sólo pueden editar los herramientas que le hayan sido asignados por un administrador. Los usuarios probadores solo pueden probar herramientas de otros usuarios.">Tipo de usuario</label></div>
				<div>
				   		<input id="rbt1" type="radio" name="type" value="admin" ${user.type.code==0 ? "checked='checked'" : ""} />
						<label for="rbt1">Administrador</label> 
						<input id="rbt2" type="radio" name="type" value="editor" ${user.type.code==1 ? "checked='checked'" : ""} />
						<label for="rbt2">Editor</label> 
						<input id="rbt3" type="radio" name="type" value="tester" ${user.type.code==2 ? "checked='checked'" : ""} />
						<label for="rbt3">Probador</label> 
				</div>
				<div><label for="is_local">Ejecutar herramientas localmente</label></div>
				<div id="is_local">
					<input type="checkbox" name="is_local" value="true" ${user.local ? "checked='checked'" : ""}/>
				</div>
				<div><label for="username">JSON con restricciones de ejecución</label></div>
				<div>
					<input type="text" name=exe_restrictions value="${user.executionRestrictions}" id="exe_restrictions"
						placeholder="JSON con restricciones de ejecución" />
				</div>
			</div>
			<div class="centrado">
				<input class="accionp" type="submit" id="adduser" name="submit" value='${editMode ? "Modificar" : "Añadir"}' />
			</div>
		</form>

	</div>
</body>
</html>