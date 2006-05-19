<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Test report XMLs"
  description="
    <a href='http://ant.apache.org/manual/CoreTypes/fileset.html'>Fileset 'includes'</a>
    setting that specifies the generated raw XML report files,
    such as 'myproject/target/test-reports/*.xml'.
    Basedir of the fileset is <a href='ws/'>the workspace root</a>.
  ">
  <input class="setting-input" name="junitreport_includes"
    type="text" value="${instance.testResults}">
</s:entry>
