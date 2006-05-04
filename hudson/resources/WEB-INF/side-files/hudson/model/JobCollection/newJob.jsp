<%--
  New Project page
--%>
<jsp:include page="sidepanel.jsp" />

<l:main-panel>
  <s:form method="post" action="createJob">
    <s:entry title="Job name">
      <input type="text" name="name" class="setting-input" />
    </s:entry>
    <s:radioBlock name="mode" value="newJob" title="Create new job" checked="true">
      <s:entry title="Job type">
        <select name="type">
          <option value="hudson.model.Project">Building a software project</option>
          <option value="hudson.model.ExternalJob">Monitoring an external job</option>
        </select>
      </s:entry>
    </s:radioBlock>
    <s:radioBlock name="mode" value="copyJob" title="Copy existing job" checked="false">
      <s:entry title="Copy from">
        <select name="from">
          <c:forEach var="v" items="${app.views}">
            <optgroup label="${v.viewName}">
              <c:forEach var="j" items="${v.jobs}">
                <option>${j.name}</option>
              </c:forEach>
            </optgroup>
          </c:forEach>
        </select>
      </s:entry>
    </s:radioBlock>

    <s:block>
      <input type="submit" name="Submit" value="OK" />
    </s:block>
  </s:form>
</l:main-panel>
<l:footer/>
