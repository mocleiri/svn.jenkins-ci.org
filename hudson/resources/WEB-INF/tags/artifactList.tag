<%@ attribute name="build" type="hudson.model.Build" required="true" %>
<%@ attribute name="caption" required="true" %>
<%@ attribute name="baseURL" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="f" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:set var="artifacts" value="${build.artifacts}" />
<c:if test="${!build.building && !empty(artifacts)}">
  <t:summary icon="package.gif">
    <c:choose>
      <c:when test="${f:length(artifacts)<17}">
        <%-- if not too many, just list them --%>
        ${caption}<br>
        <ul>
          <c:forEach var="f" items="${artifacts}">
            <li>
              <a href="${baseURL}artifact/${f}">${f.fileName}</a>
              <a href="${baseURL}artifact/${f}?fingerprint"><img src="${rootURL}/images/16x16/fingerprint.gif" alt="[fingerprint]"></a>
            </li>
          </c:forEach>
        </ul>
      </c:when>
      <c:otherwise>
          <%-- otherwise use a tree view --%>
        <a href="${baseURL}artifact/">${caption}</a>
      </c:otherwise>
    </c:choose>
  </t:summary>
</c:if>
