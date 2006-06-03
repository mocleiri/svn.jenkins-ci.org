<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="CVSROOT">
  <s:editableComboBox id="cvs_root" clazz="setting-input" name="cvs_root" value="${scm.cvsRoot}"
      items="${app.allCvsRoots}" />
</s:entry>
<s:entry title="Module(s)" help="/help/_cvs/modules.html">
  <input class="setting-input" name="cvs_module"
    type="text" value="${scm.allModules}">
</s:entry>
<s:entry title="Branch">
  <input class="setting-input" name="cvs_branch"
    type="text" value="${scm.branch}">
</s:entry>
<s:entry title="CVS_RSH" help="/help/_cvs/cvs-rsh.html">
  <input class="setting-input" name="cvs_rsh"
    type="text" value="${scm.cvsRsh}">
</s:entry>
<s:entry title="Use update" help="/help/_cvs/update.html">
  <input name="cvs_use_update"
    type="checkbox"
    <c:if test="${scm.canUseUpdate}">checked</c:if>>
</s:entry>
<s:entry title="Legacy mode" help="/help/_cvs/legacy.html">
  <input name="cvs_legacy"
    type="checkbox"
    <c:if test="${scm!=null && !scm.flatten}">checked</c:if>>
  (run CVS in a way compatible with older versions of Hudson &lt;1.21)
</s:entry>
