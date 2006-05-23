<%--
  Link to a range of build. Used by fingerprint/index.jsp
--%>
<%-- it's hudson.model.Fingerprint.RangeSet but Tomcat can't seem to handler inner classes --%>
<%@ attribute name="range" type="java.lang.Object" required="true" %>
<%@ attribute name="job" type="hudson.model.Job" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<c:forEach var="r" items="${range.ranges}">
  <c:choose>
    <c:when test="${r.start==r.end-1}">
      <t:buildLink job="${job}" number="${r.start}" />
    </c:when>
    <c:when test="${r.start==r.end-2}">
      <t:buildLink job="${job}" number="${r.start}" />
      <t:buildLink job="${job}" number="${r.end-1}" />
    </c:when>
    <c:otherwise>
      <t:buildLink job="${job}" number="${r.start}" />-<t:buildLink job="${job}" number="${r.end-1}" />
    </c:otherwise>
  </c:choose>
</c:forEach>
