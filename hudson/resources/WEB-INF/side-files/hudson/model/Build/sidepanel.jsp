<%--
  Side panel for the build view.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<l:header title="${it.parent.name} build #${it.number}" />
<l:side-panel>
  <l:tasks>
    <l:task icon="images/24x24/up.gif" href="${rootURL}/${it.parent.url}" title="Back to Project" />
    <l:task icon="images/24x24/search.gif" href="${rootURL}/${it.url}" title="Status" />
    <l:task icon="images/24x24/notepad.gif" href="${rootURL}/${it.url}changes" title="Changes" />
    <l:task icon="images/24x24/terminal.gif" href="${rootURL}/${it.url}console" title="Console Output" />
    <st:include page="actions.jsp" />
    <c:if test="${it.previousBuild!=null}">
      <l:task icon="images/24x24/previous.gif" href="${rootURL}/${it.previousBuild.url}" title="Previous Build" />
    </c:if>
    <c:if test="${it.nextBuild!=null}">
      <l:task icon="images/24x24/next.gif" href="${rootURL}/${it.nextBuild.url}" title="Next Build" />
    </c:if>
  </l:tasks>
</l:side-panel>
