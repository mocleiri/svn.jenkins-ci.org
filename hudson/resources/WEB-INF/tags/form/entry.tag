<%--
  One entry
--%>
<%@attribute name="title" required="true" %>
<%@attribute name="description" required="false" %>
<%@attribute name="help" %><%-- if present, URL to the help screen --%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>

<tr>
  <td class="setting-leftspace">&nbsp;</td>
  <td class="setting-name">
    ${title}
  </td>
  <td>
    <jsp:doBody />
  </td>
  <c:if test="${help!=null}">
    <td>
      <img src="${rootURL}/images/16x16/help.gif" alt="[?]" class="help-button" helpURL="${rootURL}${help}">
    </td>
  </c:if>
</tr>
<c:if test="${help!=null}">
  <tr><td></td><td colspan="2"><div class="help">Loading...</div></td><td></td></tr>
</c:if>
<c:if test="${description!=null && f:length(description)>0}">
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td class="setting-description">
      ${description}
    </td>
  </tr>
</c:if>