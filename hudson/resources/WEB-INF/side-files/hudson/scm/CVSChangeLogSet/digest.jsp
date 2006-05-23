<%--
  Displays the CVS change log.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<ol>
  <c:forEach var="cs" items="${it.logs}">
    <li><c:out value="${cs.msgEscaped}" escapeXml="false" /> (<a href="changes#detail${cs.index}">detail</a>)
  </c:forEach>
</ol>
