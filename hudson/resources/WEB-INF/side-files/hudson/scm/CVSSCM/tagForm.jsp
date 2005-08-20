<%--
  Displays the form to choose the tag name.

  This belongs to a build view.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<c:set var="it" scope="request" value="${build}" />

<st:include it="${it}" page="sidepanel.jsp" />
<l:main-panel>
  <t:buildCaption>Build #${it.number}</t:buildCaption>

  <form action="${pageContext.request.requestURL}&post=true" method="get">
    <p>Choose the CVS tag name for this build:</p>
    <input type="text" value="hudson-${it.number}" />
    <input type="submit" />
  </form>
</l:main-panel>
<l:footer/>