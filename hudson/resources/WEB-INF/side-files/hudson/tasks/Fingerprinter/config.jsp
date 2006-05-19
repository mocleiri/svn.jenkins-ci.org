<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Files to fingerprint"
  description="
   Can use wildcards like 'module/dist/**/*.zip'.
   See <a href='http://ant.apache.org/manual/CoreTypes/fileset.html'>
   the @includes of Ant fileset</a> for the exact format.
   the base directory is <a href='ws/'>the workspace</a>.
  ">
  <input class="setting-input" name="fingerprint_targets"
    type="text" value="${instance.targets}">
</s:entry>
<s:entry title="">
  <input name="fingerprint_artifacts"
    type="checkbox"
    <c:if test="${instance.recordBuildArtifacts}">checked</c:if>>
  Fingerprint all archived artifacts
</s:entry>
