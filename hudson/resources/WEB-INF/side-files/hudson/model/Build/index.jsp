<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:include page="sidepanel.jsp" />
<l:main-panel>
<h1>
  <img src="buildStatus" width="32" height="32" border="0">
  Build #${it.number}
  (<i:formatDate value="${it.timestamp.time}" type="both" dateStyle="medium" timeStyle="medium"/>)
</h1>

<c:if test="${it.result!=null}">
  <h2>Build Artifacts</h2>
  <ul>
    <c:forEach var="f" items="${it.artifacts}">
      <li><a href="artifact/${f}">${f}</a></li>
    </c:forEach>
  </ul>
</c:if>

<c:set var="set" value="${it.changeSet}" />
<h2>Changes</h2>
<c:choose>
  <c:when test="${f:length(set)==0}">
    No changes.
  </c:when>
  <c:otherwise>
    <ol>
      <c:forEach var="cs" items="${set}">
        <li><c:out value="${cs.msg}" escapeXml="true" /> (<a href="changes#detail${cs.index}">detail</a>)
      </c:forEach>
    </ol>
  </c:otherwise>
</c:choose>
</l:main-panel>
<l:footer/>
