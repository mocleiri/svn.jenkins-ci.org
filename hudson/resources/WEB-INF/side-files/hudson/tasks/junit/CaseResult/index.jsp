<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include it="${it.ownerBuild}" page="sidepanel.jsp" />
<l:main-panel>
  <h1>
    <c:choose>
      <c:when test="${it.passed}">
        <span style="color:#204A87">Passed</span>
      </c:when>
      <c:otherwise>
        <span style="color:#A40000">Failed</span>
      </c:otherwise>
    </c:choose>
  </h1>
  <p style="font-weight:bold">
    ${it.fullName}
  </p>
  <pre>${it.errorStackTrace}</pre>

  <c:if test="${!empty(it.owner.stdout)}">
    <h3>Standard Output</h3>
    <pre>${it.owner.stdout}</pre>
  </c:if>

  <c:if test="${!empty(it.owner.stderr)}">
    <h3>Standard Error</h3>
    <pre>${it.owner.stderr}</pre>
  </c:if>
</l:main-panel>
<l:footer/>
