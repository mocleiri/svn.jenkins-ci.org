<st:include it="${it.build}" page="sidepanel.jsp" />
<l:main-panel>
  <h1>Build #${it.build.number}</h1>
  <p>
    Tagging "<tt>${it.workerThread.tagName}</tt>" is in progress.
  </p>
  <pre><c:out value="${it.workerThread.log}" escapeXml="true" /></pre>
  <c:if test="${!it.workerThread.alive}">
    <form method="get" action="clearError">
      <input type=submit value="Clear error to retry" />
    </form>
  </c:if>
</l:main-panel>
<l:footer/>