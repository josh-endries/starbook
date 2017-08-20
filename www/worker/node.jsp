<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
	<head>
		<meta http-equiv="refresh" content="5"/>
	</head>
	<body>
		<div>Worker ${address}</div>
		<table style="vertical-align: top">
			<tr>
				<td>Subscribed Topics</td>
				<td>Stored Messages</td>
			</tr>
			<tr>
				<td>
					<div>
						<c:forEach var="topic" items="${subscribedTopics}">
							<div>${topic}</div>
						</c:forEach>
					</div>
				</td>
				<td>
					<div>
						<c:forEach var="message" items="${messages}">
							<div>${message}</div>
						</c:forEach>
					</div>
				</td>
			</tr>
		</table>
		<div>Neighbors:</div>
		<table>
			<c:forEach var="entry" items="${neighbors}">
			<tr><td><c:out value="${entry.key}"/></td><td><c:out value="${entry.value}"/></td></tr>
			</c:forEach>
		</table>
	</body>
</html>
