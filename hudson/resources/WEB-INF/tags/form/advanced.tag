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
    ><table class=advancedBody><tbody>
    <%-- this is the hidden portion that hosts the "advanced" part. Contents will be moved to the master table when "advanced..." is clicked --%>
      <jsp:doBody/>
    </tbody></table>
  </td>
</tr>