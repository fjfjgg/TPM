<%@page import="es.us.dit.lti.entity.MgmtUserType,org.owasp.encoder.Encode,es.us.dit.lti.entity.Tool"%>
<%@page import="es.us.dit.lti.persistence.ToolDao,java.util.List"%>
<%@page import="es.us.dit.lti.entity.MgmtUser,es.us.dit.lti.entity.Settings,es.us.dit.lti.persistence.MgmtUserDao"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="mgmtUser" type="es.us.dit.lti.entity.MgmtUser" scope="session" />
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" >
<meta name="viewport" content="width=device-width, initial-scale=1" >
<link rel="stylesheet" type="text/css" href="../css/style.css">
<title>Asociar usuarios</title>
<script src="../js/move.js"></script>
</head>
<%
request.setCharacterEncoding("UTF-8");
String toolname = request.getParameter("toolname");
Tool tool = ToolDao.get(toolname);
if (tool != null &&
	(mgmtUser.getType()==MgmtUserType.SUPER || ToolDao.getToolUserType(mgmtUser, tool) == MgmtUserType.ADMIN.getCode())) {
	session.setAttribute("lasttool", toolname);

	List<String> usernameList = MgmtUserDao.getNamesForAssociate(tool.getSid());
%>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp"%>
	<div class="h1container dialog">
		<h1>
			<a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
			Asociar usuarios
		</h1>

		<p>Seleccione los usuarios que desea asociar a la 
			herramienta <strong><%=Encode.forHtml(toolname)%></strong></p>
		<form method="post" action="associate">
			<input type="hidden" name="launchId" value="${launchId}" />
			<div class="scroll50">
			<%
			if (usernameList.isEmpty()) { %>
				<p>No hay usuarios.</p>
				<%
			} else { %>
				<select name='username' multiple='multiple' required='required'>
				<%
				for (String uname: usernameList) {
					out.println("<option value='" + Encode.forHtmlAttribute(uname)
					+ "'>" + Encode.forHtml(uname) + "</option>");
				} %>
				</select>
				<%
			}
			%>
			</div>
			<div class="centrado">
				<p>
					<label for="type">Tipo de usuario:</label>
					<%
					if (mgmtUser.getType()==MgmtUserType.SUPER) {
					%>
					<input type="radio" name="type" value="ADMIN" required />${text.ADMIN }
					<%
					}
					%>
					<input type="radio" name="type" value="EDITOR" required />${text.EDITOR }
					<input type="radio" name="type" value="TESTER" required />${text.TESTER }
				</p>
				<blockquote>Un usuario como máximo podrá ser de su tipo
					o inferior para esta herramienta.</blockquote>
				<input class="accionp" type='submit' name='submit' value='Asignar' />
			</div>
		</form>
	</div>
</body>
</html>
<%
}
%>