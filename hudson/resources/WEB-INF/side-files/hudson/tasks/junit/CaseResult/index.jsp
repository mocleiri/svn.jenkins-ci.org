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
    <pre><c:out value="${it.parent.stdout}" /></pre>
  </c:if>

  <c:if test="${!empty(it.parent.stderr)}">
    <h3>Standard Error</h3>
    <pre><c:out value="${it.parent.stderr}" /></pre>
  </c:if>
</l:main-panel>
<l:footer/>
