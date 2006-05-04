<%--
  Side panel for the build view.
--%>
<l:header title="${it.name}">
  <st:include page="rssHeader.jsp" />
</l:header>
<l:side-panel>
  <l:tasks>
    <l:task icon="images/24x24/up.gif" href="${rootURL}/" title="Back to Dashboard" />
    <l:task icon="images/24x24/search.gif" href="." title="Status" />
    <l:isAdmin>
      <l:task icon="images/24x24/edit-delete.gif" href="delete" title="Delete Job" />
      <l:task icon="images/24x24/setting.gif" href="configure" title="Configure" />
    </l:isAdmin>
    <st:include page="actions.jsp" />
  </l:tasks>

  <st:include page="buildHistory.jsp" />
</l:side-panel>