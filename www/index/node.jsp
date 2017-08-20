<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
	<head>
		<meta http-equiv="refresh" content="5"/>
	</head>
	<body>
		<div>${address}<c:if test="${leader}"> (leader)</c:if></div>
		<div>Users:</div>
		<table>
			<c:forEach var="entry" items="${users}">
			<tr><td><c:out value="${entry.key}"/></td><td><c:out value="${entry.value}"/></td></tr>
			</c:forEach>
		</table>
		<div>Neighbors:</div>
		<table>
			<c:forEach var="entry" items="${neighbors}">
			<tr><td>${entry.key}</td><td><c:out value="${entry.value}"/></td></tr>
			</c:forEach>
		</table>
	</body>
</html>