<%--
  Action list
--%>
<c:forEach var="action" items="${it.actions}">
  <l:task icon="images/24x24/${action.iconFileName}" href="${rootURL}/${it.url}${action.urlName}/" title="${action.displayName}" />
</c:forEach>
