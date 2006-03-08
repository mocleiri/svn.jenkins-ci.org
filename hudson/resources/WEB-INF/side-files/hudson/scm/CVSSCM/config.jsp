<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="CVSROOT">
  <s:editableComboBox id="cvs_root" clazz="setting-input" name="cvs_root" value="${scm.cvsRoot}">
    <c:forEach var="cr" items="${app.allCvsRoots}">
      <s:editableComboBoxValue value="${cr}" />
    </c:forEach>
  </s:editableComboBox>
</s:entry>
<s:entry title="Module">
  <input class="setting-input" name="cvs_module"
    type="text" value="${scm.allModules}">
</s:entry>
<s:entry title="Branch">
  <input class="setting-input" name="cvs_branch"
    type="text" value="${scm.branch}">
</s:entry>
<s:entry title="CVS_RSH">
  <input class="setting-input" name="cvs_rsh"
    type="text" value="${scm.cvsRsh}">
</s:entry>
<s:entry title="Use update"
  description="
    If checked, Hudson will use 'cvs update' whenever possible, making the build faster.
    But this causes the artifacts from the previous build to remain when a new build starts.">
  <input name="cvs_use_update"
    type="checkbox"
    <c:if test="${scm.canUseUpdate}">checked</c:if>>
</s:entry>
