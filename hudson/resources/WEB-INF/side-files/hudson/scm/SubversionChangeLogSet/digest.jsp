<%--
  Displays the Subversion change log digest.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<ol>
  <c:forEach var="cs" items="${it.logs}" varStatus="loop">
    <li><c:out value="${cs.msgEscaped}" escapeXml="false" /> (<a href="changes#detail${loop.index}">detail</a>)
  </c:forEach>
</ol>
