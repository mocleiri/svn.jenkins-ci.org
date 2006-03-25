<%--
    displays the build queue.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<l:pane title="Build Queue" width="2">
  <c:choose>
    <c:when test="${f:length(app.queue.items)==0}">
      <tr>
        <td class=pane colspan=2>
          No builds in the queue.
        </td>
      </tr>
    </c:when>
    <c:otherwise>
      <c:forEach var="item" items="${app.queue.items}">
        <tr>
          <td class="pane" width="100%"><a href="${rootURL}/${item.project.url}">
            ${item.project.name}
          </a></td>
          <td class="pane" width=16 align=center valign=middle>
            <a href="${item.project.url}cancelQueue"><img src="${rootURL}/images/16x16/stop.gif" alt="cancel this build" /></a>
          </td>
        </tr>
      </c:forEach>
    </c:otherwise>
  </c:choose>
</l:pane>
