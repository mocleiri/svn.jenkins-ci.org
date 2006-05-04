<%-- used when fingerprint had no match --%>
<l:header title="No matching record found" />
<l:side-panel>
  <l:tasks>
    <l:task icon="images/24x24/up.gif" href="${rootURL}/" title="Back to Dashboard" />
  </l:tasks>
</l:side-panel>
<l:main-panel>
  <h1>
    <img src="${rootURL}/images/48x48/fingerprint.gif">
    No matching record found
  </h1>

  <p>
    The fingerprint ${id.displayName} did not match any of the recorded data.
  </p>
  <ol>
    <li>
      Perhaps the file was not created under Hudson.
      Maybe it's a version that someone built locally on his/her own machine.
    </li>
    <li>
      Perhaps the projects are not set up correctly and not recoding
      fingerprints. Check the project setting.
    </li>
  </ol>
</l:main-panel>
<l:footer/>
