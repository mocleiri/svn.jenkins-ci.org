<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://stapler.dev.java.net/" %>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <h1>What's "project relationship"?</h1>
  <p>
    When you have projects that depend on each other, Hudson can track which build of
    the upstream project is used by which build of the downstream project, by using
    the records created by
    <a href="https://hudson.dev.java.net/fingerprint.html">the fingerprint support</a>.
  </p>
  <p>
    For this feature to work, the following conditions need to be met:
  </p>
  <ol>
    <li>The upstream project records the fingerprints of its build artifacts</li>
    <li>The downstream project records the fingerprints of the upstream jar files it uses</li>
  </ol>
  <p>
    This allows Hudson to correlate two projects.
  </p>
</l:main-panel>
<l:footer/>
