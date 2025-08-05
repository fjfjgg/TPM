<%@page import="es.us.dit.lti.entity.ToolKey"%>
<%@page import="java.net.URLEncoder,org.owasp.encoder.Encode"%>
<%@page import="es.us.dit.lti.entity.Tool"%>
<%@page import="java.util.List"%>
<%@page import="es.us.dit.lti.entity.MgmtUserType"%>
<%@page import="es.us.dit.lti.entity.MgmtUser,es.us.dit.lti.persistence.ToolDao"%>
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
<title>Mantenimiento</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="js/maintenance.js"></script>
<script src="../js/move.js"></script>
</head>
<body>
  <%@include file="/WEB-INF/includes/cabecera.jsp" %>
  <div class="h1container dialog">
	<h1>
		<a href="../user/menu.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
		Mantenimiento
	</h1>
	<a href="#"><span id="add" class="material-icons">sync</span></a>
	<h2>Información sin uso</h2>
	<form id="pageForm" method="post">
		<input type="hidden" id="launchId" name="launchId" value="${launchId}" />
		<div class="scroll70">
			<div class="keyfields">
				<span></span>
				<!-- LTI users -->
				<div>
					<span class="keyTitle">
						<a id="users" class="faction" href="deleteunusedusers" 
							title="Borrar"><span class="material-icons">delete</span></a>
					</span>
					<span class="keyTitle"><strong>Usuarios de consumidores</strong></span>
				</div>
				<div>
					<span class="keyTitle" id="users-count">0</span>
				</div>
				<!-- Resource users -->
				<div>
					<span class="keyTitle">
						<a id="resourceUsers" class="faction" href="deleteunusedresourceusers" 
							title="Borrar"><span class="material-icons">delete</span></a>
					</span>
					<span class="keyTitle"><strong>Usuarios de recursos</strong></span>
				</div>
				<div><span class="keyTitle" id="resourceUsers-count">0</span></div>
				
				<!-- Resource Links -->
				<div>
					<span class="keyTitle">
						<a id="resourceLinks" class="faction" href="deleteunusedresourcelinks" 
							title="Borrar"><span class="material-icons">delete</span></a>
					</span>
					<span class="keyTitle"><strong>Enlaces a recursos</strong></span>
				</div>
				<div><span class="keyTitle" id="resourceLinks-count">0</span></div>
				
				<span></span>
				<!-- Contexts -->
				<div>
					<span class="keyTitle">
						<a id="contexts" class="faction" href="deleteunusedcontexts" 
							title="Borrar"><span class="material-icons">delete</span></a>
					</span>
					<span class="keyTitle"><strong>Contextos/cursos</strong></span>
				</div>
				<div><span class="keyTitle" id="contexts-count">0</span></div>
				<!-- Consumers -->			
				<div>
					<span class="keyTitle">
						<a id="consumers" class="faction" href="deleteunusedconsumers" 
							title="Borrar"><span class="material-icons">delete</span></a>
					</span>
					<span class="keyTitle"><strong>Consumidores/LMS</strong></span>
				</div>
				<div><span class="keyTitle" id="consumers-count">0</span></div>
				
			</div>
		</div>
	</form>
	<p><br /></p>
	<h3 id="messages"></h3>
	<h2>Limpiar información borrada</h2>
	<div class="centrado">
		<button id="optimize" title="Optimizar" class="botonGeneral accionp">
			<span class="material-icons">delete_forever</span>
		</button>
	</div>
  </div>	
</body>
</html>
