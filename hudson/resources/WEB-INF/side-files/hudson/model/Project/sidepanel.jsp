<%--
  Side panel for the project view.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<l:header title="${it.name}" />
<l:side-panel>
  <l:tasks>
    <l:task icon="images/24x24/up.png" href="${rootURL}/" title="Back to Dashboard" />
    <l:task icon="images/24x24/search.png" href="${rootURL}/${it.url}" title="Status" />
    <l:task icon="images/24x24/folder.png" href="${rootURL}/${it.url}ws/" title="Workspace" />
    <c:if test="${it.hasJavadoc}">
      <l:task icon="images/24x24/help.png" href="${rootURL}/${it.url}javadoc" title="Javadoc" />
    </c:if>
    <l:isAdmin>
      <l:task icon="images/24x24/clock.png" href="${rootURL}/${it.url}build" title="Schedule a build" />
      <l:task icon="images/24x24/edit-delete.png" href="${rootURL}/${it.url}delete" title="Delete Project" />
      <l:task icon="images/24x24/setting.png" href="${rootURL}/${it.url}configure" title="Configure" />
    </l:isAdmin>
    <st:include page="actions.jsp" />
  </l:tasks>

  <st:include page="buildHistory.jsp" />
</l:side-panel>