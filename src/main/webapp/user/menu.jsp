<%@page import="es.us.dit.lti.Messages"%>
<%@page import="es.us.dit.lti.entity.MgmtUser"%>
<%@page import="es.us.dit.lti.entity.Settings"%>
<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %> 
<jsp:useBean id="text" type="es.us.dit.lti.MessageMap" scope="session" />
<fmt:setLocale value="${text.locale}"/>
<fmt:setBundle basename="messages"/>
<!DOCTYPE html>
<html lang="es">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" >
	<meta name="viewport" content="width=device-width, initial-scale=1" >
	<title>Herramienta de corrección</title>
	<link rel="stylesheet" type="text/css" href="../css/style.css" >
</head>
<body>
	<%@include file="/WEB-INF/includes/cabecera.jsp" %>
	<div class="h1container">
		<c:if test="${sessionScope.pendingMessage != null}">
			<h3><fmt:message key="${sessionScope.pendingMessage}"/></h3>
			<c:remove var="pendingMessage" scope="session"/>
		</c:if>
	</div>
</body>
</html>
