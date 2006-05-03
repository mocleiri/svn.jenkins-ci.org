<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:section title="CVS">
  <s:entry title=".cvspass file"
    description=".cvspass file to load passwords from. Leave it empty to read from $HOME/.cvspass">
    <input class="setting-input" name="cvs_cvspass"
      type="text" value="${descriptor.cvspassFile}">
  </s:entry>

  <%--
  <s:entry title="Repository browser" help="/help/system-config/cvs-browser.html">
    <table width="100%">
      <c:forEach var="root" items="${app.allCvsRoots}" varStatus="loop">
        <s:entry title="CVSROOT">
          ${root}
        </s:entry>
        <s:entry title="URL">
          <input name="cvs_repobrowser_cvsroot${loop.index}"
            type="hidden" value="${root}">

          <c:set var="key" value="repository-browser.${root}" />
          <input class="setting-input" name="cvs_repobrowser${loop.index}"
            type="text" value="${descriptor.properties[key]}">

          <c:set var="key" value="repository-browser.diff.${root}" />
          <input class="setting-input" name="cvs_repobrowser_diff${loop.index}"
            type="text" value="${descriptor.properties[key]}">
        </s:entry>
      </c:forEach>
    </table>
  </s:entry>
  --%>
</s:section>