<%--
  Displays the Subversion change log digest.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="rev" value="${it.revisionMap}" />
<c:choose>
  <c:when test="${empty(rev)}">
    <%-- nothing --%>
  </c:when>
  <c:when test="${f:length(rev)==1}">
    Revision:
    <c:forEach var="r" items="${rev}">${r.value}</c:forEach><%-- just print that one value--%>
    <br>
  </c:when>
  <c:otherwise>
    Revisions
    <ul>
      <c:forEach var="r" items="${rev}">
        ${r.key} : ${r.value}
      </c:forEach>
    </ul>
  </c:otherwise>
</c:choose>
<c:choose>
  <c:when test="${it.emptySet}">
    No changes.
  </c:when>
  <c:otherwise>
    Changes
    <ol>
      <c:forEach var="cs" items="${it.logs}" varStatus="loop">
        <li><c:out value="${cs.msgEscaped}" escapeXml="false" /> (<a href="changes#detail${loop.index}">detail</a>)
      </c:forEach>
    </ol>
  </c:otherwise>
</c:choose>
