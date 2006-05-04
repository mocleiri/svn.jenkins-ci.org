<st:include it="${it.build}" page="sidepanel.jsp" />
<l:main-panel>
  <h1>Build #${it.build.number}</h1>
  <p>
    This build is already tagged as <tt>${it.tagName}</tt>
  </p>
  <pre><c:out value="${it.workerThread.log}" escapeXml="true" /></pre>
</l:main-panel>
<l:footer/>