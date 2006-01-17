<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ attribute name="name" required="true" type="java.lang.String" %>
<%@ attribute name="href" required="true" type="java.lang.String" %>
<%@ attribute name="active" required="true" type="java.lang.Boolean" %>
<c:choose>
  <c:when test="${tabPass=='pass1'}">
    <%-- in the 1st pass we draw the dummy top row to get the 'dent' right --%>
    <c:choose>
      <c:when test="${active}">
        <td class=active rowspan=2>${name}</td>
        <c:set scope="request" var="activeIndex" value="${tabIndex}" />
      </c:when>
      <c:otherwise>
        <td style="height:3px; padding:0px"></td>
      </c:otherwise>
    </c:choose>
  </c:when>
  <c:otherwise>
    <%-- in the 2nd pass we draw the real tabs --%>
    <c:if test="${tabIndex!=activeIndex}">
      <td class="inactive
        <c:choose>
          <c:when test="${tabIndex<activeIndex}">noRight</c:when>
          <c:when test="${tabIndex>activeIndex}">noLeft</c:when>
        </c:choose>
        ">
          <a href="${href}">${name}</a>
      </td>
    </c:if>
  </c:otherwise>
</c:choose>
<c:set scope="request" var="tabIndex" value="${tabIndex+1}" />
