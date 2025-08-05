<%@page import="es.us.dit.lti.entity.MgmtUserType,org.owasp.encoder.Encode,es.us.dit.lti.entity.Tool"%>
<%@page
	import="es.us.dit.lti.entity.MgmtUser,es.us.dit.lti.persistence.MgmtUserDao,es.us.dit.lti.persistence.ToolDao,java.util.List"%>
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="mgmtUser" type="es.us.dit.lti.entity.MgmtUser" scope="session"></jsp:useBean>
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Desasociar usuarios</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="../js/move.js"></script>
</head>
<%
request.setCharacterEncoding("UTF-8");
String toolname = request.getParameter("toolname");
Tool tool = ToolDao.get(toolname);
if (tool != null && ToolDao.getToolUserType(mgmtUser, tool) == MgmtUserType.ADMIN.getCode()) {
	session.setAttribute("lasttool", toolname);
	
	List<String> users = MgmtUserDao.getNamesForDisassociate(mgmtUser, tool.getSid());
%>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">
		<h1>
			<a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
			Desasociar usuarios
		</h1>
		<p>Seleccione los usuarios que desea desasociar de la
		 herramienta <strong><%=Encode.forHtml(toolname)%></strong></p>
		<form method="post" action="disassociate">
			<input type="hidden" name="launchId" value="${launchId}" />
			<%
			if (users.isEmpty()) { %>
				<p>No hay usuarios.</p> 
			<%
			} else { %>
			<div class="scroll70">
				<select name='username' multiple='multiple' required='required'>
				<%
				for (String un: users) { %>
					<option value='<%=Encode.forHtmlAttribute(un)%>'><%=Encode.forHtml(un)%></option>
				<% }  %>
				</select>
			</div>
			<div class='centrado'><input class='accionp' type='submit' name='submit' value='Desasociar'/></div>
			<% } %>
		</form>
	</div>
</body>
</html>
<%
}
%>