<%@taglib prefix="test" uri="https://www.viralpatel.net/jsp/taglib/substr"%>
<html>
<head>
	<title>JSP Custom Taglib example: Substr function</title>
</head>
<body>
	SUBSTR(GOODMORNING, 1, 6) is 
	<font color="blue">
	<test:substring input="GOODMORNING" start="1" end="6"/>
	</font>
</body>
</html>
