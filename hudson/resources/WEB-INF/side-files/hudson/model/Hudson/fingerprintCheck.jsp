<%--
  New View page
--%>
<st:include page="sidepanel.jsp" />

<l:main-panel>
  <h1>
    <img src="${rootURL}/images/48x48/fingerprint.gif">
    Check File Fingerprint
  </h1>
  <s:form method="post" action="doFingerprintCheck" enctype="multipart/form-data">
    <s:block>
      <div style="margin-bottom: 1em;">
      Got a jar file but don't know which version it is? <br>
      Find that out by checking the fingerprint against
      the database in Hudson (<a href="https://hudson.dev.java.net/fingerprint.html">more details</a>)
      </div>
    </s:block>
    <s:entry title="File to check">
      <input type="file" name="name" class="setting-input" />
    </s:entry>
    <s:block>
      <input type="submit" name="Submit" value="Check" />
    </s:block>
  </s:form>
</l:main-panel>
<l:footer/>
