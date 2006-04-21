<%--
  Action list
--%>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:forEach var="action" items="${it.actions}">
  <l:task icon="images/24x24/${action.iconFileName}" href="${rootURL}/${it.url}${action.urlName}/" title="${action.displayName}" />
</c:forEach>
