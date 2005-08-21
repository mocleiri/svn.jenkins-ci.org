<%--
  One entry
--%>
<%@attribute name="title" required="true" %>
<%@attribute name="description" required="false" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>

<tr>
  <td class="setting-name">
    ${title}
  </td>
  <td>
    <jsp:doBody />
  </td>
</tr>
<c:if test="${description!=null && f:length(description)>0}">
  <tr>
    <td>&nbsp;</td>
    <td class="setting-description">
      ${description}
    </td>
  </tr>
</c:if>