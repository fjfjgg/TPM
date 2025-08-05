<%@page import="java.time.Instant"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@page import="java.util.Locale"%>
<%@page import="es.us.dit.lti.Messages"%>
<%@page import="es.us.dit.lti.config.ToolUiConfig"%>
<%@page import="es.us.dit.lti.ToolSession"%>
<%@page import="es.us.dit.lti.entity.Tool"%>
<%@page import="es.us.dit.lti.entity.Settings,java.io.IOException"%>
<%@page import="java.io.FileInputStream"%>
<%@page
	import="es.us.dit.lti.entity.MgmtUser,es.us.dit.lti.persistence.ToolDao,org.owasp.encoder.Encode"%>
<%@page import="java.nio.charset.StandardCharsets,org.apache.commons.io.output.WriterOutputStream"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@taglib prefix="e" uri="owasp.encoder.jakarta"%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %> 
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />
<%
try {
	ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
	Tool tool = null;
	if (ts != null)
		tool = ts.getTool();
	
if (tool != null && tool.getName() != null) {
	pageContext.setAttribute("ts", ts);
	String toolTitle = tool.getName();
	ToolUiConfig tui = tool.getToolUiConfig();
	String resourceTitle = "";
	if (ts.getContext().getTitle()!=null && ts.getResourceLink().getTitle()!=null) {
		resourceTitle = ts.getContext().getTitle() + ": " + ts.getResourceLink().getTitle();
	} else if (ts.getContext().getTitle()!=null) {
		resourceTitle = ts.getContext().getTitle();
	} else if (ts.getResourceLink().getTitle()!=null) {
		resourceTitle = ts.getResourceLink().getTitle();
	}
	if (!tool.isEnabled() || !ts.getToolKey().isEnabled()) {
		if (ts.getLtiReturnUrl() != null && !ts.getLtiReturnUrl().isEmpty()) {
			response.sendRedirect(ts.getLtiReturnUrl());
		} else {
			out.println("<p>" + text.get("T_AVISO_DESHABILITADA") + ".</p>");
		}
		return;
	}
	//File maximum size
	int inputFileSize = Settings.getMaxUploadSize();
	if (tui.getInputFileSize() == null || tui.getInputFileSize() == 0 || tui.getInputFileSize() > Settings.getMaxUploadSize()) {
		tui.setInputFileSize(inputFileSize);
	} else {
		inputFileSize = tui.getInputFileSize();
	}
%>

<fmt:setLocale value="${text.getLocale()}"/>
<fmt:setBundle basename="messages"/>

<!DOCTYPE html>
<html lang="${text.language}">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1" >
	<%
	if (Settings.getAppName()== null || Settings.getAppName().isEmpty()) {
		out.println("<title>"+text.get("T_NOMBRE_LTI")+"</title>");
	} else {
		out.println("<title>"+Settings.getAppName()+"</title>");
	}
	//Default CSS
	String cssPaths = Settings.getDefaultCssPath();
	if (!tui.isIgnoreConsumerCss() && ts.getConsumer().getCssPath()!=null && 
			!ts.getConsumer().getCssPath().isEmpty()) {
		cssPaths = ts.getConsumer().getCssPath();
	}
	String titulo = text.get("T_NOMBRE_LTI");
	if (resourceTitle!=null && !resourceTitle.isEmpty()) {
		titulo = resourceTitle;
	}
	if (cssPaths != null) {
	  for (String cssString : cssPaths.split(",")) {	%>
	<link rel="stylesheet" type="text/css" href="<%=Encode.forHtmlAttribute(cssString)%>" />
	<%
	  }
	}
	%>
	<link rel="stylesheet" type="text/css" href="../css/material-icons.css" />
	<link rel="stylesheet" type="text/css" href="css/tool.css" />
	<script>
	var inputFileSize=<%=inputFileSize*1024 %>;
	var inputFileAccept="<%=Encode.forJavaScriptBlock(tui.getInputFileAccept()) %>";
	var inputFilePattern=<%=((tui.getInputFilePattern()==null) ? "null" : ("'"+Encode.forJavaScriptBlock(tui.getInputFilePattern())+"'")) %>;
	var textFilename="<%=Encode.forJavaScriptBlock(tui.getTextFilename()) %>";
	<%if (tool.getEnabledUntil()!=null && tool.isEnabledByDate()) { 
		Long timeLeft = tool.getTimeLeft(); //Time left	%>
	var startDate=new Date();
	var serverStartDate=new Date("<%= DateTimeFormatter.ISO_INSTANT.format(Instant.now()) %>");
	var expirationDate=new Date("<%= DateTimeFormatter.ISO_INSTANT.format(tool.getEnabledUntil().toInstant()) %>");
	var timeLeft=<%= (timeLeft==null ? "null" : timeLeft) %>;
	<% } %>
	</script>
	<script src="js/tool.js"></script>
	<%if (tui.isShowAttempts()) { %>
	<link rel="stylesheet" type="text/css" href="css/attempts.css" />
	<script src="js/attempts.js"></script>
	<script>
	var attemptsDependsOnFilename=<%=tui.isMaxAttemptsDependsOnFilenames()%>;
	</script>
	<% } %>
</head>
<body>
	<div id="content">
		<div id="pageTitleDiv" class="pageTitle">
			<div id="pageTitleBar">
				<% if (!ts.isFrameMode()) { %>
		  		<h1 id="pageTitleHeader"><span id="pageTitleText">
		  			<%=Encode.forHtmlContent(titulo) %></span></h1>
		  		<% }
				   if (Settings.getAppName()== null || Settings.getAppName().isEmpty()) { %>
		  		<p><fmt:message key="T_NOMBRE_LTI"/> - Departamento de Ingeniería Telemática - Universidad de Sevilla</p>
		  		<% } else { %>
		  		<p><%=Settings.getAppName() %></p>
		  		<% } %>
			</div>
			<%
			//Is there a notice?
			String notice = es.us.dit.lti.entity.Settings.getNotice();
			if (notice != null && !notice.isEmpty() ) {
				%>
			<div id="notice"><%=notice %></div>
				<%
			}	
			%>
		</div>
		<div id="containerdiv" class="container">
			<%
			if (tool.getDeliveryPassword() != null && !tool.getDeliveryPassword().isEmpty() && tui.isPasswordProtected()
					&& !tool.getDeliveryPassword().equals(request.getParameter("password"))) {
				session.removeAttribute("extraFileAuthorized");
			%>
			<div>
				<form id="fsdelivery" action="" method="post">
					<input type="hidden" name="launchId" value="${ts.launchId}" />
					<% if (request.getParameter("password")!=null) { %>
						<p><fmt:message key="T_ERROR_AUTORIZACION"/></p>
					<% } %>
					<p>
						<input type="password" name="password" autocomplete="off"
							placeholder="<fmt:message key="T_CLAVE_ENTREGA"/>" required autofocus/>
					</p>
					<p>
						<button class="genericButton">
							<span class="material-icons">send</span>
						</button>
					</p>
				</form>
			</div>	
			<%
			} else {
				session.setAttribute("extraFileAuthorized", true);
			%>
			<fieldset id="description" class="infocontainer" data-toggle="parentdescription">
				<legend><fmt:message key="T_DESCRIPCION"/></legend>
				<%
				try (FileInputStream br = new FileInputStream(tool.getDescriptionPath());) {
					WriterOutputStream wos = WriterOutputStream.builder().setWriter(out)
							   .setCharset(StandardCharsets.UTF_8).get();
					br.transferTo(wos);
					wos.flush();
				} catch (IOException e) {
					%>
					<p><strong><fmt:message key="T_ERROR_DESCRIPCION"/></strong></p>
					<%
				}
				%>
			</fieldset>
			<%if (tui.isShowAttempts()) { %>
				<fieldset id="attempts" class="infocontainer hidden" data-toggle="parentattempts">
					<legend><fmt:message key="T_INTENTOS_ANTERIORES"/></legend>
					<div id="attemptlist"></div>
				</fieldset>
			<%} %>
			<p id="parentdescription" class="toggleButton hidden">
					<a id="showdescription" title="<fmt:message key="T_MOSTRAR_DESCRIPCION"/>" class="genericButton" data-toggle="description">
					<span class="material-icons">description</span></a></p>	
			<%if (tui.isShowAttempts()) { %>
				<p id="parentattempts" class="toggleButton">
					<a id="showattempts" title="<fmt:message key="T_MOSTRAR_INTENTOS"/>" class="genericButton"
						data-toggle="attempts">
					<span class="material-icons" accessKey="p">restore</span></a></p>								
			<%} %>
			<p id="parentassessment" class="toggleButton hidden">
					<a id="showassessment" title="<fmt:message key="T_MOSTRAR_RESULTADOS"/>" class="genericButton" data-toggle="resultoutput">
					<span class="material-icons">assessment</span></a></p>
			<p id="parentdelivery" class="toggleButton hidden">
					<a id="showdelivery" title="<fmt:message key="T_MOSTRAR_FORMULARIO"/>" class="genericButton" data-toggle="fsdelivery">
					<span class="material-icons">plagiarism</span></a></p>
			<div id="result"></div>
			<%if ( tui.getMaxAttempts() != 0 && tool.isEnabledByDate() ) { %>
			<form id="formdelivery">
				<input type="hidden" id="launchId" value="${ts.launchId}" />
				<fieldset id="fsdelivery" data-toggle="parentdelivery">
					<legend> ${e:forHtml(! empty ts.tool.toolUiConfig.legendForm ? ts.tool.toolUiConfig.legendForm : text.T_ENTREGA)} </legend>
					<p>
					<% if (tool. getEnabledUntil() != null) { %>
					<b><fmt:message key="T_HABILITADA_HASTA"/>: </b> <fmt:formatDate type="both" value="${ts.tool.enabledUntil.time}"/><br />
					<% } %>
					<%if (tui.isEnableSendText()) { %>
					<fmt:message key="T_ESCRIBA_TEXTO"/>
					<%} else { %>
					<fmt:message key="T_SELECCIONA_ARCHIVO"/>
					<%} %>
					<span class="material-icons">plagiarism</span>. <fmt:message key="T_ADVERTENCIA_TAM"/> <%=inputFileSize%>kB.</p>
					<%if (tool.getDeliveryPassword() != null && !tool.getDeliveryPassword().isEmpty()) { 
						if (tui.isPasswordProtected()) { %>
					<input type="hidden" id="password" value="${ts.tool.deliveryPassword}" />
						<%											
						} else {
						%>
					<p>	
						<fmt:message key="T_CLAVE_ENTREGA"/>: 
						<input type="password" id="password" autocomplete="off" required/>
					</p>
					<%} }%>
					<p>
					<%if (tui.isEnableSendText()) { %>
					<textarea id="attempttext" rows="10" cols="70"></textarea>
					<%} else { %>
					<input type="file" id="attemptfile" />
					<%} %>
					</p>
					<p>
					<a id="assess" title="${e:forHtmlAttribute(! empty ts.tool.toolUiConfig.sendButtonText ? ts.tool.toolUiConfig.sendButtonText : text.T_BOTON_ENTREGAR)}"
						 class="genericButton" >
						<span class="material-icons">plagiarism</span></a>
					</p>
				</fieldset>
			</form>
			<%} else {%>
				<input type="hidden" id="launchId" value="${ts.launchId}" />
			<%}%>
		<%
		}
		%>
		</div>
		<p id="plogout" class="">
			<a id="alogout" title="<fmt:message key="T_SALIR"/>" class="genericButton" href="${ts.ltiReturnUrl}"> <span
				class="material-icons" accesskey="x">logout</span></a>
		</p>
	</div>
</body>
</html>
<%
	} else {
		response.sendRedirect("../error404.html");
	}
} catch (Exception e) {
	e.printStackTrace();
}
%>
