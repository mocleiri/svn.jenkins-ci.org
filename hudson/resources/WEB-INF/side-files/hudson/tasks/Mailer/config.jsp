<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Recipients"
  description="Whitespace-separated list of recipient addresses. E-mail will be sent when a build fails.">
  <input class="setting-input" name="mailer_recipients"
    type="text" value="${publisher.recipients}">
</s:entry>
<s:entry title="">
  <input name="mailer_not_every_unstable"
    type="checkbox"
    <c:if test="${publisher.dontNotifyEveryUnstableBuild}">checked</c:if>>
  Don't send e-mail for every unstable build
</s:entry>
