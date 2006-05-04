<s:entry title="Modules"
  description="
    URL of SVN module. Multiple URLs can be specified.
  ">
  <input class="setting-input" name="svn_modules"
    type="text" value="${scm.modules}">
</s:entry>
<s:entry title="Use update"
  description="
    If checked, Hudson will use 'svn update' whenever possible, making the build faster.
    But this causes the artifacts from the previous build to remain when a new build starts.">
  <input name="svn_use_update"
    type="checkbox"
    <c:if test="${scm.useUpdate}">checked</c:if>>
</s:entry>
<s:advanced>
  <s:entry title="Username"
    description="
      If you need to specify a user name for accessing the repository.
      To specify a password, <a href='svn-password'>see this</a>.

    ">
    <input class="setting-input" name="svn_username"
      type="text" value="${scm.username}">
  </s:entry>
  <s:entry title="Other options"
    description="
      If you need to specify any other SVN option during checkout/update, specify them here
    ">
    <input class="setting-input" name="svn_other_options"
      type="text" value="${scm.otherOptions}">
  </s:entry>
</s:advanced>
