<%--
  Side panel for the build view.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<l:header title="${it.parent.name} run #${it.number}" />
<l:side-panel>
  <l:tasks>
    <l:task icon="images/24x24/navigate_up.gif" href="${rootURL}/${it.parent.url}" title="Back to Job" />
    <l:task icon="images/16x16/console_network.gif" href="." title="Console Output" />
    <c:if test="${it.hasArtifacts}">
      <l:task icon="images/24x24/folders.gif" href="artifacts-index" title="Artifacts" />
    </c:if>
    <c:if test="${it.previousBuild!=null}">
      <l:task icon="images/24x24/navigate_left.gif" href="${rootURL}/${it.previousBuild.url}" title="Previous Run" />
    </c:if>
    <c:if test="${it.nextBuild!=null}">
      <l:task icon="images/24x24/navigate_right.gif" href="${rootURL}/${it.nextBuild.url}" title="Next Run" />
    </c:if>
  </l:tasks>
</l:side-panel>
