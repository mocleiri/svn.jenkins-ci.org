<%--
  Displays the console output
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<jsp:include page="sidepanel.jsp" />
<l:main-panel>
  <t:buildCaption>Changes</t:buildCaption>
  <st:include page="index.jsp" it="${it.changeSet}" />
</l:main-panel>
<l:footer/>