<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Projects names"
  description="Multiple projects can be specified like 'abc, def'">
  <input class="setting-input" name="upstreamProjects"
    type="text" value="${instance.upstreamProjectsValue}">
</s:entry>