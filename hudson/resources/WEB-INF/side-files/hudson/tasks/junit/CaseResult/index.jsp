<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%@ taglib prefix="h" uri="http://hudson.dev.java.net/" %>

<st:include it="${it.owner}" page="sidepanel.jsp" />
<l:main-panel>
  <c:set var="st" value="${it.status}" />
  <h1 class="${st.cssClass}">
    ${st.message}
  </h1>
  <p style="font-weight:bold">
    ${it.fullName}
  </p>
  <c:if test="${!it.passed}">
    <div style="text-align:right;">
      Failing for the past
      ${h:addSuffix(it.age,'build','builds')}
      (since #${it.failedSince})
    </div>
  </c:if>
  <pre>${it.errorStackTrace}</pre>

  <c:if test="${!empty(it.parent.stdout)}">
    <h3>Standard Output</h3>
    <pre>${it.parent.stdout}</pre>
  </c:if>

  <c:if test="${!empty(it.parent.stderr)}">
    <h3>Standard Error</h3>
    <pre>${it.parent.stderr}</pre>
  </c:if>
</l:main-panel>
<l:footer/>
