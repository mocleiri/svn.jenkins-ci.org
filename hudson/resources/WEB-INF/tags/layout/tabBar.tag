<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<table cellpadding=0 cellspacing=0 id="viewList">
  <c:set var="tab" value="${tabs}" />
  <%-- dummy row to get spacing right --%>
  <tr style="height:3px;">
    <td style="height:3px; padding:0px"></td>
    <c:set scope="request" var="tabIndex" value="0" />
    <c:set scope="request" var="tabPass" value="pass1" />
    <jsp:doBody />
  </tr>
  <tr>
    <td style="border: none; border-bottom: 1px solid #bbb;">&nbsp;</td>
    <c:set scope="request" var="tabIndex" value="0" />
    <c:set scope="request" var="tabPass" value="pass2" />
    <jsp:doBody />
    <td class="filler">&nbsp;</td>
  </tr>
</table>
