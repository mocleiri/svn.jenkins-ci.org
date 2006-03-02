<%-- Show files in the workspace --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>

<st:include page="sidepanel.jsp" />
<l:main-panel>
  <div class=dirTree>
  <%-- parent path --%>
    <div class=parentPath>
      <form action="." method="get">
        <a href="${rootURL}/${it.url}ws/"><img src="${rootURL}/images/32x32/folder-open.png" class=rootIcon /></a>
        <c:forEach var="p" items="${parentPath}">
          <a href="${p.href}">${p.title}</a>
          /
        </c:forEach>
        <input type="text" name="path">
        <input type="image" src="${rootURL}/images/16x16/go-next.png" style="vertical-align:middle"/>
      </form>
    </div>
    <ul>
      <c:forEach var="f" items="${files}">
        <li><img src="${rootURL}/images/16x16/${f[0].iconName}"/>
        <c:forEach var="t" items="${f}" varStatus="st"
          ><a href="${t.href}">${t.title}</a
          ><c:if test="${!st.last}">/</c:if
        ></c:forEach>
      </c:forEach>
    </ul>
  </div>
</l:main-panel>
<l:footer/>
