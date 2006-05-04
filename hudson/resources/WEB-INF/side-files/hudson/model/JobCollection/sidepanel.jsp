<%--
  Side panel for the build view.
--%>
<l:header title="Hudson">
  <link rel="alternate" title="Hudson:${it.viewName} (all builds)" href="rssAll" type="application/rss+xml" />
  <link rel="alternate" title="Hudson:${it.viewName} (failed builds)" href="rssFailed" type="application/rss+xml" />
</l:header>
<l:side-panel>
  <l:tasks>
    <l:isAdmin>
      <l:task icon="images/24x24/new-package.gif" href="newJob" title="New Job" />
      <l:task icon="images/24x24/setting.gif" href="${rootURL}/configure" title="Configure" />
      <l:task icon="images/24x24/refresh.gif" href="${rootURL}/reload" title="Reload Config" />
      <st:include page="sidepanel2.jsp" />
    </l:isAdmin>
  </l:tasks>
  <t:queue />
  <t:executors />
</l:side-panel>
