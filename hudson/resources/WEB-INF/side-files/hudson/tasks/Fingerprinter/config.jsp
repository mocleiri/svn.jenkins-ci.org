<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Files to fingerprint"
  description="
   Can use wildcards like 'module/dist/**/*.zip'.
   See <a href='http://ant.apache.org/manual/CoreTypes/fileset.html'>
   the @includes of Ant fileset</a> for the exact format.
   the base directory is the workspace.
  ">
  <input class="setting-input" name="fingerprint_targets"
    type="text" value="${publisher.targets}">
</s:entry>
<s:entry title="">
  <input name="fingerprint_artifacts"
    type="checkbox"
    <c:if test="${publisher.recordBuildArtifacts}">checked</c:if>>
  Also fingerprint all archived artifacts
</s:entry>
