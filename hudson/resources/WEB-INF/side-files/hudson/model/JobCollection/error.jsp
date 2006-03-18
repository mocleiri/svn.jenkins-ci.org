<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="sidepanel.jsp" />

<l:main-panel>
  <h1>Error</h1>
  <p><c:out value="${message}" /></p>
</l:main-panel>
<l:footer/>
