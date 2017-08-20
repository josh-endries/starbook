<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
	<head>
		<script type="text/javascript" src="/scripts/jquery-1.7.2.min.js"></script>
		<style type="text/css">
			body {
				background-color: #fff;
				color: #000;
			}

			button#bookit {
				background-color: #99d;
				border: 2px solid #99d;
				border-radius: 20px;
				border-style: outset;
				color: white;
				display: inline-block;
				font-family: Georgia, serif;
				font-size: 1.5em;
			}
			button#bookit > span {
				position: relative;
				top: 0.2em;
			}

			.message {
				background-color: #ebb;
				border: 2px solid #ebb;
				padding: 0.25em;
				margin: 0.5em;
			}
			.message > .content {
				background-color: #fff8f8;
				border-bottom-right-radius: 5px;
				border-top-right-radius: 5px;
				font-family: Georgia, serif;
				font-size: 1.5em;
			}
			.message > .content > span {
				padding-left: 0.25em;
			}
			.message > .topic {
				background: #ebb;
				float: left;
				font-family: Arial, sans, sans-serif;
				padding: 0px 0.25em 0px 0.25em;
				text-align: right;
			}
			.message > .topic > .date {
				color: #844;
				font-size: 0.7em;
				margin: -5px 0px 0px 0px;
				min-width: 5em;
				padding: 0px;
			}
			.message > .topic > .name {
				margin: -3px 0px 0px 0px;
				padding: 0px 0px 3px 0px;
			}

			.rounded-corners {
				-moz-border-radius: 10px;
				-webkit-border-radius: 10px;
				-khtml-border-radius: 10px;
				border-radius: 10px;
			}

			td.bumper {
				text-align: left;
				width: 30%;
			}
			td.main {
				height: 5em;
				vertical-align: middle;
				text-align: center;
			}
			td.main textarea {
				background-color: #dde;
				border: 2px solid #dde;
				border-style: inset;
				font-family: Arial, sans, sans-serif;
				margin: 0px;
				padding: 0px;
				width: 100%;
				height: 100%;
			}

			#subscriptionList {
				background-color: #9c9;
				border-radius: 10px;
				margin-top: 0.5em;
				padding: 3px;
				vertical-align: top;
			}
			#subscriptionList > .content {
				background-color: #9c9;
				padding: 3px;
			}
			#subscriptionList > .content > div {
				white-space: nowrap;
			}
			#subscriptionList > .content > div > span {
				margin-right: 1.25em;
			}
			#subscriptionList > .header {
				background-color: #9c9;
				font-family: Arial, serif;
				padding: 3px;
				text-align: center;
			}
			#subscriptionList .even {
				background-color: #ada;
			}
			#subscriptionList > .footer {
				text-align: center;
			}
			#subscriptionList > .footer > button {
				border: 2px solid #8b8;
				border-style: outset;
				background-color: #8b8;
				border-radius: 5px;
			}

			#messageList {
				vertical-align: top;
				width: 100%;
				word-wrap: break-word;
			}

			img.x {
				float: right;
				height: 1em;
				margin-top: 2px;
				width: 1em;
			}
			img.x:hover {
				cursor: pointer;
			}

			#messageContent {
				height: 3em;
			}
			</style>
	</head>
	<body>
		<div style="position: absolute; color: #aaa; font-size: 0.75em;">${address}</div>
		<!-- CSS == epic fail -->
		<table style="width: 100%;"><tr><td class="bumper"></td><td class="main"><textarea id="messageContent" placeholder="Enter a message..."></textarea></td><td class="bumper"><button id="bookit"><span>*</span></button></td></tr></table>
		<div><form method="post" action="/images" enctype="multipart/form-data"><input type="file" name="file"/> <input type="submit"/></form></div>
		<table style="width: 100%">
			<tr>
				<td id="messageList"></td>
				<td style="vertical-align: top">
					<div id="subscriptionList">
						<div class="header">Following</div>
						<div class="content"></div>
						<div class="footer"><button>Add</button></div>
					</div>
				</td>
			</tr>
		</table>
		<div style="width: 100%;">
		</div>
		<div>
			
		</div>
		<script type="text/javascript">
			$.urlParam = function(name) {
				var results = new RegExp('[?&]' + name + '=([^&#]*)').exec(window.location.href);
				if (!results) return 0;
				return results[1] || 0;
			};
			
			var u = $.urlParam('user');
			if (u == 0) {
				document.userName = window.location.host.split('.')[0];
				document.userParameter = '';
			} else {
				document.userName = u;
				document.userParameter = '?user='+u;
			}

			function loadSubscriptions() {
				$.ajax({
					cache: false,
					dataType: "json",
					success: function(data) {
						var container = $("#subscriptionList > .content");
						container.empty();
						var itemNumber = 0; // Need this because we skip the user's self entry, messing up odd/even.
						for (var i=0; i<data.length; i++) {
							var rowClass = (itemNumber % 2 == 0) ? 'even' : 'odd';
							var subscription = data[i];
							if (subscription == document.userName) continue;
							itemNumber++;
							var div = $('<div class="'+rowClass+'"/>');
							var image = $('<img src="/images/x.png" class="x"/>');
							image.data("topic", subscription);
							image.bind('click', function() {
								$.ajax({
									data: { action: "remove", topic: $(this).data("topic") },
									success: function() {
										setTimeout("loadSubscriptions()", 500);
									},
									type: "POST",
									url: "/subscriptions"+document.userParameter
								});
							});
							div.append(image);
							div.append($("<span>"+subscription+"</span>"));
							container.append(div);
						}
					},
					url: "/subscriptions"+document.userParameter
				});
			}

			function loadMessages() {
				var now = new Date();

				$.ajax({
					cache: false,
					dataType: "json",
					success: function(data) {
						if (data.length > 0) $("#messageList").empty();
						for (var i=0; i<data.length; i++) {
							var content = data[i].c;
							var topic = data[i].t;
							var date = new Date(Date.parse(data[i].d));
							var min = (date.getMinutes() < 10) ? "0"+date.getMinutes() : date.getMinutes();
							var displayDate = date.getHours()+":"+min;
							if (Math.abs(now.getTime() - date.getTime()) > 86400000) {
								displayDate = date.getMonth()+"/"+date.getDate()+" "+displayDate;
							}
							$("#messageList").append('<div class="message rounded-corners"><div class="topic"><div class="date">'+displayDate+'</div><div class="name">'+topic+'</div></div><div class="content"><span>'+content+'</span></div></div>');
						}
					},
					url: "/messages"+document.userParameter
				});
			};
			
			$(document).ready(function() {
				setInterval("loadMessages()", 2500);
				loadSubscriptions();
				loadMessages();

				/*
				 * Set up the new subscription button functionality.
				 */
				$("#subscriptionList > .footer > button").bind("click", function() {
					var newTopic = prompt("New topic");
					$.ajax({
						data: { action: "add", topic: newTopic },
						success: function() {
							setTimeout("loadSubscriptions()", 500);
						},
						type: "POST",
						url: "/subscriptions"+document.userParameter
					});
				});

				/*
				 * Set up the new message button functionality.
				 */
				 $("#bookit").bind("click", function() {
					var content = $("#messageContent").val();
					$.ajax({
						data: { content: content },
						success: function() {
							$("#messageContent").val("");
							setTimeout("loadMessages()", 500);
						},
						type: "POST",
						url: "/messages"+document.userParameter
					});
				 });
			});
		</script>
	</body>
</html>
