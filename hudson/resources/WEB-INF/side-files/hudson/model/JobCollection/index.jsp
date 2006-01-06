<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
  response.setHeader("X-Hudson",
      getServletConfig().getServletContext().getAttribute("version").toString());
%>
<jsp:include page="sidepanel.jsp" />

<l:main-panel>
  <t:projectView jobs="${it.jobs}" />
</l:main-panel>
<l:footer/>
