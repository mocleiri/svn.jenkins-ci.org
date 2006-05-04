<%-- Show files in the workspace --%>
<st:include page="sidepanel.jsp" />
<l:main-panel>
  <div class=dirTree>
  <%-- parent path --%>
    <div class=parentPath>
      <form action="." method="get">
        <a href="${topPath}"><img src="${rootURL}/images/48x48/${icon}" class=rootIcon /></a>
        <c:forEach var="p" items="${parentPath}">
          <a href="${p.href}">${p.title}</a>
          /
        </c:forEach>
        <input type="text" name="path">
        <input type="image" src="${rootURL}/images/16x16/go-next.gif" style="vertical-align:middle"/>
      </form>
    </div>
    <table class=fileList>
      <c:forEach var="f" items="${files}">
        <tr>
          <td>
            <img src="${rootURL}/images/16x16/${f[0].iconName}"/>
          </td>
          <td>
            <c:forEach var="t" items="${f}" varStatus="st"
              ><a href="${t.href}">${t.title}</a
              ><c:if test="${!st.last}">/</c:if
              ><c:set var="x" value="${t}"
            /></c:forEach>
          </td>
          <c:if test="${!x.folder}">
            <td class=fileSize>
              ${x.size}
              <a href="${x.href}/*fingerprint*/"
                ><img src="${rootURL}/images/16x16/fingerprint.gif" alt="fingerprint"
              ></a>
            </td>
          </c:if>
        </tr>
      </c:forEach>
    </table>
  </div>
</l:main-panel>
<l:footer/>
