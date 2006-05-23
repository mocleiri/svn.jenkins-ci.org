<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:if test="${app.fingerprintMap.ready}">
  <l:task icon="images/24x24/search.gif" href="projectRelationship" title="Project Relationship" />  
  <l:task icon="images/24x24/fingerprint.gif" href="fingerprintCheck" title="Check File Fingerprint" />
</c:if>
