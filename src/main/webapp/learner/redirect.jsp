<%@page import="es.us.dit.lti.ToolSession"%>
<%@page import="java.io.FileInputStream,java.io.IOException"%>
<%@page import="java.nio.charset.StandardCharsets,org.apache.commons.io.output.WriterOutputStream"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
pageContext.setAttribute("ts", ts);
%>
<!DOCTYPE html>
<html lang="es">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<title>Redirector</title>
	<script src="js/redirect.js"></script>
</head>
<body>
	<div id="textoProgreso" class="${ts.launchId}">
	<%
	//Info proporcionada en el proyecto
	try (FileInputStream br = new FileInputStream(ts.getTool().getDescriptionPath());) {
		WriterOutputStream wos = WriterOutputStream.builder().setWriter(out)
				   .setCharset(StandardCharsets.UTF_8).get();
		br.transferTo(wos);
		wos.flush();
	} catch (IOException e) {
		out.println("<p><b> Error cargando la descripción. </b></p>");
	}
	%>
	</div>
</body>
</html>
