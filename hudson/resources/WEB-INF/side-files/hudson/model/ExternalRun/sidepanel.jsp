<%--
  Side panel for the build view.
--%>
<l:header title="${it.parent.name} run #${it.number}" />
<l:side-panel>
  <l:tasks>
    <l:task icon="images/24x24/up.gif" href="${rootURL}/${it.parent.url}" title="Back to Job" />
    <l:task icon="images/24x24/terminal.gif" href="." title="Console Output" />
    <c:if test="${it.hasArtifacts}">
      <l:task icon="images/24x24/package.gif" href="artifacts-index" title="Artifacts" />
    </c:if>
    <st:include page="actions.jsp"/>
    <c:if test="${it.previousBuild!=null}">
      <l:task icon="images/24x24/previous.gif" href="${rootURL}/${it.previousBuild.url}" title="Previous Run" />
    </c:if>
    <c:if test="${it.nextBuild!=null}">
      <l:task icon="images/24x24/next.gif" href="${rootURL}/${it.nextBuild.url}" title="Next Run" />
    </c:if>
  </l:tasks>
</l:side-panel>
