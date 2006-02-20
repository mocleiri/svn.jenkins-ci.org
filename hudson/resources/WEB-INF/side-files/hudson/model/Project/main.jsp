<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="i" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<table style="margin-top: 1em; margin-left:1em;">
  <c:if test="${it.hasJavadoc}">
    <t:summary icon="help.png">
      <a href="javadoc">Javadoc</a>
    </t:summary>
  </c:if>
  <t:summary icon="folder.png">
    <a href="ws">Workspace</a>
  </t:summary>
  <c:if test="${it.lastSuccessfulBuild.hasArtifacts}">
    <t:summary icon="package.png">
      Latest Artifacts<br>
      <ul>
        <c:forEach var="f" items="${it.lastSuccessfulBuild.artifacts}">
          <li><a href="lastSuccessfulBuild/artifact/${f}">${f.fileName}</a></li>
        </c:forEach>
      </ul>
    </t:summary>
  </c:if>
</table>
