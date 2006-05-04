<%--
  Side panel for the build view.
--%>
<l:header title="${it.parent.name} build &#x23;${it.number}" />
<l:side-panel>
  <l:tasks>
    <c:set var="buildUrl" value="${h:buildUrlDecompose(pageContext.request)}" />
    <l:task icon="images/24x24/up.gif" href="${rootURL}/${it.parent.url}" title="Back to Project" />
    <l:task icon="images/24x24/search.gif" href="${buildUrl.baseUrl}/" title="Status" />
    <l:task icon="images/24x24/notepad.gif" href="${buildUrl.baseUrl}/changes" title="Changes" />
    <l:task icon="images/24x24/terminal.gif" href="${buildUrl.baseUrl}/console" title="Console Output" />
    <st:include page="actions.jsp" />
    <c:if test="${it.previousBuild!=null}">
      <l:task icon="images/24x24/previous.gif" href="${buildUrl.previousBuildUrl}" title="Previous Build" />
    </c:if>
    <c:if test="${it.nextBuild!=null}">
      <l:task icon="images/24x24/next.gif" href="${buildUrl.nextBuildUrl}" title="Next Build" />
    </c:if>
  </l:tasks>
</l:side-panel>
