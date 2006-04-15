<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<%@ taglib prefix="h" uri="http://hudson.dev.java.net/" %>
<%--
  Edit View Page
--%>
<st:include page="sidepanel.jsp" />

<l:main-panel>
  <s:form method="post" action="configSubmit">
    <s:entry title="Description" help="/help/view-config/description.html">
      <textarea class="setting-input" name="description"
        rows="5" style="width:100%">${it.viewMessage}</textarea>
    </s:entry>
    <s:entry title="Jobs">
      <c:forEach var="job" items="${app.jobs}">
        <input type="checkbox" name="${job.name}"
          <c:if test="${h:containsJob(it,job)}">checked</c:if>>
        ${job.name}
        <br>
      </c:forEach>
    </s:entry>

    <s:block>
      <input type="submit" name="Submit" value="OK" />
    </s:block>
  </s:form>
</l:main-panel>
<l:footer/>
