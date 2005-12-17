<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Maven Version">
  <select class="setting-input" name="maven_version">
    <option>(Default)</option>
    <c:forEach var="inst" items="${descriptor.installations}">
      <option <c:if test="${inst.name==builder.maven.name}">selected</c:if>>${inst.name}</option>
    </c:forEach>
  </select>
</s:entry>
<s:entry title="Goals">
  <input class="setting-input" name="maven_targets"
    type="text" value="${builder.targets}">
</s:entry>
