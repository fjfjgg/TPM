<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@taglib prefix="e" uri="owasp.encoder.jakarta"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Error</title>
<style type="text/css">
#error-container { margin: auto; line-height: 1.6em; text-align: center; font-family: 'Impact', sans-serif; }
#error-container .error-title { text-transform: uppercase; }
#error-container a:link, #error-container a:visited{ color: #881536; }
#error-container a:hover, #error-container a:active{ color: #F8B505; }
#error-container .error-title h2 { font-size: 4em; }
#error-container .error-title h3 { font-size: 2em; color: #797979; }
</style>
</head>
<body>
	<div id="error-container">
		<div class="error-title">
			<h2>Error</h2>

		</div>
		<p>${e:forHtmlContent(param.errorMessage!=null ? param.errorMessage : errorMessage)}</p>
		<p>
			<script>
				if (history.length > 1) {
					document.write("<a href='javascript:history.back()'>Volver</a>");
				}
			</script>
		</p>
	</div>
</body>
</html>