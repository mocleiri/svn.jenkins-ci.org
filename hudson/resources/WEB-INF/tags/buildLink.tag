<%--
  Link to a build. Used by fingerprint/index.jsp
--%>
<%@ attribute name="jobName" type="java.lang.String" %>
<%@ attribute name="job" type="hudson.model.Job" required="true" %>
<%@ attribute name="number" type="java.lang.Integer" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="h" uri="http://hudson.dev.java.net/" %>

<c:choose>
  <c:when test="${job==null}">
    ${jobName} #<%-- --%>${number}
  </c:when>
  <c:otherwise>
    <c:set var="r" value="${h:getRun(job,number)}" />
    <c:choose>
      <c:when test="${r==null}">
        ${jobName} #<%-- --%>${number}
      </c:when>
      <c:otherwise>
        <a href="${rootURL}/${r.url}">
          <img src="${rootURL}/images/16x16/${r.buildStatusUrl}" />${jobName} #<%-- --%>${number}
        </a>
      </c:otherwise>
    </c:choose>
  </c:otherwise>
</c:choose>