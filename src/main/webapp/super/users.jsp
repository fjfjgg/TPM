<%@page import="es.us.dit.lti.persistence.MgmtUserDao"%>
<%@page import="es.us.dit.lti.entity.MgmtUser"%>
<%@page import="es.us.dit.lti.entity.Settings"%>
<%@page import="java.util.List,org.owasp.encoder.Encode"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Lista de usuarios</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="js/users.js"></script>
<script src="../js/move.js"></script>
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">
		
		<h1>
			<a href="../user/menu.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a> 
			Usuarios
		</h1>
		<%
		//Vemos si hay algún mensaje de alguna pantalla anterior	
		if (session.getAttribute(Settings.PENDING_MSG_ATTRIB) != null) {
			%>
			<h3><%=text.get(session.getAttribute(Settings.PENDING_MSG_ATTRIB))%></h3>
			<%
			session.setAttribute(Settings.PENDING_MSG_ATTRIB, null);
		}
		%>
		<a href="../super/createuser.jsp"> <span id="add" class="material-icons">add</span></a>
		<h2>Lista de usuarios</h2>
		
		<form method="post" action="">
			<input type="hidden" name="launchId" value="${launchId}" />
			<div class="scroll70">
				<table aria-label="usuarios" id="users">
					<tr>
						<th scope="col"></th>
						<th scope="col">Usuario</th>
						<th scope="col">Nombre</th>
						<th scope="col">Correo</th>
						<th scope="col">Tipo</th>
						<th scope="col">Ejecución Local</th>
					</tr>
				<%
				List<MgmtUser> users = MgmtUserDao.getAll();
				if (users==null || users.isEmpty()) { %>
					<tr><td colspan="6"> No hay usuarios.</td></tr>
				<%  
				} else {
					for (MgmtUser user: users) { %>
					<tr class='userentry'>
						<td class='seleccionar'><input type='radio' name='username' 
							value='<%=Encode.forHtmlContent(user.getUsername())%>' required ></td>
						<td><%=Encode.forHtml(user.getUsername())%></td>
						<td><%=Encode.forHtml(user.getNameFull())%></td>
						<td><%=Encode.forHtml(user.getEmail())%></td>
						<td><%=text.get(user.getType().toString())%></td>
						<td><%=user.isLocal()? "&check;" : "" %></td>
					</tr>
						<%
					}
				}
				%>
				</table>
			</div>
			<div class="centrado">
				<input type='submit' name='submit' class="accionp"
					value='Editar' formaction='createuser.jsp' />
				<input type='submit' name='submit' class="accionp"
					value='Eliminar' formaction='deleteuser' /> 
			</div>
		</form>
	</div>
</body>
</html>