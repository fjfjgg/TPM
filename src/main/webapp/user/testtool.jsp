<%@page import="java.net.URI"%>
<%@page import="es.us.dit.lti.entity.Tool,es.us.dit.lti.ToolSession"%>
<%@page import="es.us.dit.lti.entity.MgmtUserType,es.us.dit.lti.entity.MgmtUser"%>
<%@page	import="es.us.dit.lti.persistence.ToolDao,org.owasp.encoder.Encode"%>
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
		String outcomeSrvUrl = null;
		if (tool.isOutcome()) {
			//Test outcome service
			URI uriBase = new URI(request.getRequestURL().toString());
			outcomeSrvUrl = uriBase.resolve("../testoutcome").toString();
		}
		ts.initTest(tool, (String) session.getAttribute("launchId"), 
					sessionUser, text.get("T_PAGINA_PRUEBAS"),
					request.getParameter("learner")!=null, 
					request.getParameter("instructor")!=null,
					request.getRemoteAddr(), outcomeSrvUrl);
					
		if (request.getParameter("framed") != null) {
			ts.setLtiReturnUrl(response.encodeRedirectURL(request.getRequestURL().toString() + "?end"));
			ts.setFrameMode(true);
			ts.setPresentationDocumentTarget("frame");
		}
		session.setAttribute(ToolSession.class.getName(), ts);

		response.sendRedirect("../" + ts.getContinueUrl());

	} else if (request.getParameter("end") != null) {
		out.print("<!DOCTYPE html>\n<html lang='es'>"
		+ "<head><script src='js/testend.js'></script></head><body><h1>Finalizado</h1></body></html>");
	} else {
	%>
<!DOCTYPE html>
<html lang="es">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Prueba de herramientas</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<link rel="stylesheet" type="text/css" href="css/testtool.css">
<script src='js/testtool.js'></script>
<script src="../js/move.js"></script>
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container dialog">
		<h1>
			<a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
			Prueba de herramienta: "<%=Encode.forHtml(toolTitle)%>"
		</h1>
		<%
		//Cogemos las propiedades de la herramienta seleccionado
		//y las utilizamos como valores por defecto
		if (permisos <= MgmtUserType.TESTER.getCode()) {
			session.setAttribute("lasttool", toolTitle);
		%>
		<div class="centrado">
		  <% if (tool.getToolUiConfig().isRedirectMode()) { %>
			<h2>Modo Redirección</h2>
		  <% } %>
			<p>
				<a id="testl" href="?learner" target="_blank" tabindex="-1">
					<input type='button' class="accionp" name='submit' autofocus
					value='En ventana - estudiante' /></a>
				
				<a id="testlf" href="?learner&framed" class="a-tool-modal" tabindex="-1"
					data-name="<%=Encode.forHtml(toolTitle)%> - Estudiante"> 
					<input type='button' class="accionp" value='En marco - estudiante' /></a>
			</p>
			<p>
				<a id="testi" href="?instructor" target="_blank" tabindex="-1">
					<input type='button' class="accionp" name='submit'
					value='En ventana - profesor' /></a>
				
				<a id="testif" href="?instructor&framed" class="a-tool-modal"
					data-name="<%=Encode.forHtml(toolTitle)%> - Profesor" tabindex="-1"> 
					<input type='button' class="accionp" value='En marco - profesor' /></a>
			</p>
		</div>
	  </div>
	  <div id='tool-modal' class='modal h1container dialog'>
		<h1>
			<span class='modal-close material-icons bcerrar'>close</span> <span
				id='tool-modal-caption'></span>
		</h1>
		<div id='tool-modal-content' class='modal-content'>
			<div class='resizer'>
				<iframe id='toolframe' class="resized" src='' seamless='seamless' title="Test Tool Iframe"></iframe>
			</div>
		</div>
		<%
		} else {
		%>
		<h1>No está autorizado.</h1>
		<% } %>
	</div>
</body>
</html>
<%
}
%>
