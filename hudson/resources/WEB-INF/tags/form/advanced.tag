<%--
  Expandable section that shows "advanced..." button by default.
  Upon clicking it, a section unfolds.
--%>
<tr><td></td>
  <td>
    <div class=advancedLink>
      <input type="button" value="Advanced..." class="advancedButton" />
    </div
    <%-- no space inbetween so that 'nextSibling' takes us to advancedBody --%>
    ><div class=advancedBody>
    <%-- this is the hidden portion that becomes visible once "advanced" is clicked --%>
      <jsp:doBody/>
    </div>
  </td>
</tr>