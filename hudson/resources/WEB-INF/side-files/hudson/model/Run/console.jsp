<%--
  Displays the console output
--%>
<st:include page="sidepanel.jsp" />
<l:main-panel>
<t:buildCaption>
  Console Output
</t:buildCaption>
<pre><c:out value="${it.log}" escapeXml="true" /></pre>
</l:main-panel>
<l:footer/>