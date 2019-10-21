<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
<title>Tag Example</title>
</head>
<body>
<% 
  boolean b = true; 
  pageContext.setAttribute("b",new Boolean(b));
%>
<c:if test="${b}">
b is TRUE
</c:if>
</body>
</html>
