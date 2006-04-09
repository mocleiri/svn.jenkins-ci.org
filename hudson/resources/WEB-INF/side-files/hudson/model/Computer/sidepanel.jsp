<%--
  Side panel for a slave.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<l:header title="${it.displayName}" />
<l:side-panel>
  <l:tasks>
    <l:task icon="images/24x24/up.gif" href="${rootURL}/" title="Back to Dashboard" />
  </l:tasks>
</l:side-panel>