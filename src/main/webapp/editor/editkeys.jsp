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
<title>Claves de la herramienta</title>
<link rel="stylesheet" type="text/css" href="../css/style.css">
<script src="js/editkeys.js"></script>
<script src="../js/move.js"></script>
</head>
<body>
  <%@include file="/WEB-INF/includes/cabecera.jsp" %>
  <div class="h1container dialog">
	<%
	//Cogemos las propiedades de la herramienta seleccionado
	//y las utilizamos como valores por defecto
	request.setCharacterEncoding("UTF-8");
	String toolTitle = request.getParameter("toolname");
	Tool tool = ToolDao.get(toolTitle);
	
	if (tool == null || toolTitle == null || ToolDao.getToolUserType(mgmtUser, tool) > MgmtUserType.EDITOR.getCode()) {
	%>
	<h1><a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
		Error
	</h1>
	<h2>No tiene acceso.</h2>
<%
	} else {
		List<ToolKey> list = ToolDao.getAllPossibleToolKeys(tool);
		session.setAttribute("lasttool", toolTitle);
		pageContext.setAttribute("list", list);
		pageContext.setAttribute("tool", tool);
%>
	<h1>
		<a href="../user/tools.jsp" accesskey="x"><span class="material-icons bcerrar">close</span></a>
		Claves de la herramienta
	</h1>
	<p>Herramienta habilitada:  <span class="material-icons">${tool.enabled ? 'check' : 'block'}</span></p>
	<h2>Claves y restricciones</h2>
	<div class="scroll70">
		<div class="keyfields">
			<div><strong>Clave y<br />Secreto</strong></div>
			<div title="Permitido o bloqueado"><strong><span class="material-icons">flaky</span></strong></div>
			<div title="Consumidor al que se restringe esta clave"><strong>Consumidor</strong></div>
			<div title="Contexto al que se restringe esta clave"><strong>Contexto</strong></div>
			<div title="Enlace al que se restringe esta clave"><strong>Enlace</strong></div>
			<div title="Expresión regular de la IP remota a la que se restringe esta clave"><strong>IP</strong></div>
			<div><strong></strong></div>
		
			<c:forEach items="${list}" var="tk" varStatus="row">
			<%-- Se usarán los keyTitle y keyId para indicar al servidor la fila, el servidor verificará que son coherentes:
			 por el contexto debe pertenecer al consumidor, el rl al contexto y herramienta. Esto también se usará para crear un formulario --%>
			 
			<%-- Se puede aprovechar algo similar para poder borrar los datos de un rl/context/consumer de esta herramienta --%>
			<div>
				<span class="keyTitle"><strong id="key-${row.index}"><e:forHtml value="${tk.key}" /></strong></span><br />
				<span><small id="secret-${row.index}"><e:forHtml value="${tk.secret}" /></small></span>
			</div>
			
			<div>
				<span class="keyTitle" id="enabled-${row.index}"><span class="material-icons">${tk.enabled ? 'check' : 'block'}</span></span>
			</div>
			
			<div>
				<span class="keyId" id="consumer-${row.index}"><e:forHtml value="${ tk.consumer.guid}" /></span><br />
				<span class="keyTitle"><e:forHtml value="${ tk.consumer.name }" /></span>
			</div>
			
			<div>
				<span class="keyId" id="context-${row.index}"><e:forHtml value="${ tk.context.contextId}" /></span><br />
				<span class="keyTitle"><e:forHtml value="${ tk.context.title }" /></span>
			</div>
			
			<div>
				<span class="keyId" id="rl-${row.index}"><e:forHtml value="${ tk.resourceLink.resourceId }" /></span><br />
				<span class="keyTitle"><e:forHtml value="${ tk.resourceLink.title }" /></span>
			</div>
			
			<div>
				<span id="address-${row.index}" class="material-icons ${empty tk.address ? 'hidden' : ''}" 
					title="${tk.address}">lan</span>				
			</div>
			
			<div>
				<%-- Con el row.index podemos identificar los elementos de una misma fila --%>
				<span class="keyTitle">
					<a id="edit-${row.index}" class="a-src-modal faction fileoutput" href="" 
						title="Editar clave"><span class="material-icons">edit</span></a>
					<a id="dupl-${row.index}" class="a-src-modal faction fileoutput" href="" 
						title="Duplicar clave"><span class="material-icons">content_copy</span></a>
				</span>
			</div>
			
			</c:forEach>
		</div>
	</div>
  </div>
	<div id='src-modal' class='modal h1container dialog'>
		<h1>
			<span class='modal-close material-icons bcerrar'>close</span> <span
				id='src-modal-caption'></span>
		</h1>
		<h3 id="info-modal"></h3>
		<form id="modalForm" method="post">
			<div id='src-modal-content'>
				<div class="editfields">
					<input type="hidden" id="launchId" name="launchId" value="${launchId}" />
					<input type="hidden" id="toolname" name="toolname" value="${e:forHtmlAttribute(param.toolname)}" />
					<div title="Clave de la herramienta LTI (key). Se muestra la clave sin el prefijo asociado '${e:forHtmlAttribute(param.toolname)}'. Déjelo en blanco para borrar la clave.">
						Clave</div>
					<div>
						<span id="formKeyPrefix"></span><input type="text" id="formKey" name="key" value=""/>
					</div>
					
					<div title="Secreto de la herramienta LTI (secret)">Secreto</div>
					<div><input type="text" id="formSecret" name="secret" value="" required="required" /></div>
					
					<div title="Seleccione si desea habilitar la herramienta LTI">Habilitar</div>
					<div><input id="formEnabled" type="checkbox" name="enabled" value="true" /> Sí </div>
					
					<div id="consumer" title="Consumidor al que se restringe esta clave">Consumidor</div>
					<div><input type="text" id="formConsumer" value="" disabled="disabled" /></div>
					
					<div id="context" title="Contexto al que se restringe esta clave">Contexto/curso</div>
					<div><input type="text" id="formContext" value="" disabled="disabled" /></div>
					
					<div id="resourceLink" title="Enlace al que se restringe esta clave">Enlace</div>
					<div><input type="text" id="formResourceLink" value="" disabled="disabled" /></div>

					<div id="address" title="Expresión regular de la IP remota a la que se restringe esta clave">Patrón de dirección remota</div>
					<div><input type="text" id="formAddress" value="" /></div>
				</div>
			</div>
			<div class="centrado">
				<button id="saveEditor" title="Guardar" class="botonGeneral accionp">
					<span class="material-icons">save</span>
				</button>
			</div>
		</form>
		<% } %>
	</div>
</body>
</html>
