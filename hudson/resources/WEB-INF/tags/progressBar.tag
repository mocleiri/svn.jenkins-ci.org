<%--
  Progress bar. @pos (0-100) specifies the current position
--%>
<%@attribute name="pos" required="true" %>
<%@attribute name="href" required="false" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<table class="progress-bar"
  <c:if test="${href!=null}">
    style="cursor:pointer"
    onclick="window.location='${href}'"
        <%-- note that this won't take effect in browsers that don't let JavaScript change status bar, like Firefox. --%>
    onmouseover="window.status='${href}';return true;"
    onmouseout="window.status=null;return true;"
  </c:if>
>
  <c:choose>
    <c:when test="${pos<0}">
      <tbody><tr style="background-image:url(${rootURL}/images/progress-unknown.gif)"><td></td></tr></tbody>
    </c:when>
    <c:otherwise>
        <tbody><tr>
          <td class="progress-bar-done" style="width:${pos}%;"></td>
          <td class="progress-bar-left" style="width:${100-pos}%"></td>
        </tr>
      </tbody>
    </c:otherwise>
  </c:choose>
</table>

