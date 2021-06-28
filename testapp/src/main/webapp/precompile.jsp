<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
<title>InitParam Precompile</title>
</head>
<body>
org.wildfly.jastow.jspc.precompiled=<c:out value="${initParam['org.wildfly.jastow.jspc.precompiled']}"/>
</body>
</html>
