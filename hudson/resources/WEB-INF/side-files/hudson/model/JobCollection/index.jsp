<%
  response.setHeader("X-Hudson",
      getServletConfig().getServletContext().getAttribute("version").toString());
%>
<jsp:include page="sidepanel.jsp" />

<l:main-panel>
  <div id=view-message>
    ${it.viewMessage}
  </div>
  <t:projectView jobs="${it.jobs}" showViewTabs="true" />
</l:main-panel>
<l:footer/>
