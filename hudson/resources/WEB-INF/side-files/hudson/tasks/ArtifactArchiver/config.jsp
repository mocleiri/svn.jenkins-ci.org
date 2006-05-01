<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Files to archive"
  description="
   Can use wildcards like 'module/dist/**/*.zip'.
   See <a href='http://ant.apache.org/manual/CoreTypes/fileset.html'>
   the @includes of Ant fileset</a> for the exact format.
   the base directory is the workspace.
  ">
  <input class="setting-input" name="artifacts"
    type="text" value="${instance.artifacts}">
</s:entry>
