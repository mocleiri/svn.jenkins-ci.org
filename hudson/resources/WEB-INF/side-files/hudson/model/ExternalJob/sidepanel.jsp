<%--
  Side panel for the build view.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<l:header title="${it.name}" />
<l:side-panel>
  <l:tasks>
    <l:task icon="images/24x24/navigate_up.png" href="${rootURL}/" title="Back to Dashboard" />
    <l:task icon="images/24x24/folders.png" href="." title="Status" />
    <l:isAdmin>
      <l:task icon="images/24x24/garbage.png" href="delete" title="Delete Job" />
      <l:task icon="images/24x24/wrench.png" href="configure" title="Configure" />
    </l:isAdmin>
  </l:tasks>

  <st:include page="buildHistory.jsp" />
</l:side-panel>