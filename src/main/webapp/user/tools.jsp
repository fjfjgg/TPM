<%@page import="es.us.dit.lti.entity.Tool,java.util.List"%>
<%@page import="es.us.dit.lti.entity.Settings"%>
<%@page import="es.us.dit.lti.entity.MgmtUserType,org.owasp.encoder.Encode"%>
<%@page	import="es.us.dit.lti.entity.MgmtUser,es.us.dit.lti.persistence.ToolDao"%>
<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<jsp:useBean id="mgmtUser" type="es.us.dit.lti.entity.MgmtUser" scope="session"></jsp:useBean>
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />
<% response.addHeader("Cache-Control", "no-cache"); %>	
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" >
<meta name="viewport" content="width=device-width, initial-scale=1" >
<title>Herramientas</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="../user/js/tools.js"></script>
<script src="../js/move.js"></script>
</head>
<%
List<Tool> tools = ToolDao.getAll(mgmtUser);
String lasttool = (String) session.getAttribute("lasttool");

boolean tieneAdministrados = false;
%>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">
		<h1>
			<a href="../user/menu.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
			Herramientas
		</h1>
		<% 
		//Comprobamos si hay algún mensaje de alguna pantalla anterior	
		if (session.getAttribute(Settings.PENDING_MSG_ATTRIB) != null) {
			%>
			<h3><%=text.get(session.getAttribute(Settings.PENDING_MSG_ATTRIB))%></h3>
			<%
			session.setAttribute(Settings.PENDING_MSG_ATTRIB, null);
		}
		if (mgmtUser.getType() == MgmtUserType.ADMIN) {
		%>
		<a href="../admin/createtool.jsp"> <span id="add" class="material-icons">add</span></a>
		<%
		}
		%>
		<h2>Lista de herramientas</h2>
		<form id="formulario" method="post" action="" accept-charset="UTF-8">
			<input type="hidden" name="launchId" value="${launchId}" />
			<div class="scroll50"><table aria-label="herramientas" id="tools">
				<tr>
					<th scope="col"></th>
					<th scope="col">Nombre</th>
					<th scope="col">Permisos</th>
				</tr>
			<%
			if (tools.isEmpty()) { %>
				<tr><td colspan="3">No tiene herramientas.</td></tr>
			<% 
			} else {
				for (Tool tool: tools) {
					boolean selected = false;
					if (!tieneAdministrados || tool.getName().equals(lasttool)) {
						selected = true;
						tieneAdministrados = true;
						if (lasttool == null)
							session.setAttribute("lasttool", tool.getName());
					}%>
					<tr><td class='seleccionar'>
						<input type='radio' class='type<%=tool.getUserTypeCode()%>' name='toolname' 
							value='<%=Encode.forHtmlAttribute((tool.getName()))%>'
							required <%=selected? "checked" : "" %> ></td>
						<td class='toolname'><%=Encode.forHtml(tool.getName())%></td>
						<td><%=text.get(MgmtUserType.fromInt(tool.getUserTypeCode()).toString())%></td>
					</tr>
				<% }
			}
			%>
			</table></div>
			<br />
			<div class="centrado">
				<% if (mgmtUser.getType() == MgmtUserType.SUPER) {%>
				<input type='button' id='binfo' value='Información' class="accionp" disabled="disabled" accesskey="i"/>
				<input type='button' id='bdelete' value='Borrar' class="accionp" disabled="disabled"/>
				<br />
				<input type='button' id='bassign' value='Asociar usuarios' class="accionp" disabled="disabled"/>
				<input type='button' id='bunassign' value='Desasociar' class="accionp" disabled="disabled"/>
				<% } else if (mgmtUser.getType() == MgmtUserType.ADMIN) {%>
				<input type='button' id='binfo' value='Información' class="accionp" disabled="disabled" accesskey="i"/>
				<input type='button' id='bkeys' value='Claves' class="accionp" disabled="disabled" accesskey="c"/>
				<input type='button' id='bedit' value='Editar' class="accionp" disabled="disabled" accesskey="e"/>
				
				<input type='button' id='bdelete' value='Borrar' class="accionp" disabled="disabled"/>
				<input type='button' id='bdeletedata' value='Borrar datos' class="accionp" disabled="disabled"/>
				<input type='button' id='bdownload'  value='Descargar' class="accionp" disabled="disabled"/>
				
				<input type='button' id='bassign' value='Asociar usuarios' class="accionp" disabled="disabled"/>
				<input type='button' id='bunassign' value='Desasociar' class="accionp" disabled="disabled"/>
				<input type='button' id='bdisable' value='Deshabilitar sesiones' class="accionp" disabled="disabled"/>
				
				<input type='button' id='btest' value='Probar' class="accionp" accesskey="p"/><br />
				<% } else if (mgmtUser.getType() == MgmtUserType.EDITOR) {%>
				<input type='button' id='binfo' value='Información' class="accionp" disabled="disabled" accesskey="i"/>
				<input type='button' id='bkeys' value='Claves' class="accionp" disabled="disabled" accesskey="c"/>
				<input type='button' id='bedit' value='Editar' class="accionp" disabled="disabled" accesskey="e"/>
				
				<input type='button' id='bdownload' value='Descargar' class="accionp" disabled="disabled"/>
				<input type='button' id='bdeletedata' value='Borrar datos' class="accionp" disabled="disabled"/>
				<input type='button' id='bdisable' value='Deshabilitar sesiones' class="accionp" disabled="disabled"/>
				
				<input type='button' id='btest' value='Probar' class="accionp" accesskey="p"/><br />
				<% } else { %>
				<input type='button' id='btest' value='Probar' class="accionp" accesskey="p"/><br />
				<% } %>
			</div>
		</form>
	</div>
</body>
</html>
