<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Recipients"
  description="Whitespace-separated list of recipient addresses">
  <input class="setting-input" name="mailer_recipients"
    type="text" value="${publisher.recipients}">
</s:entry>
<s:entry title="">
  <input name="mailer_failureOnly"
    type="checkbox"
    <c:if test="${publisher.failureOnly}">checked</c:if>>
  send e-mails only when a build failed
</s:entry>
<s:entry title="Subject"
    description="Subject of the notification e-mails.
      &#x24;{buildId} expands to the build ID,
      &#x24;{buildNumber} expands to the build number, and
      &#x24;{result} expands to the result.">
  <input class="setting-input" name="mailer_subject"
    type="text" value="${publisher.subject}">
</s:entry>
<s:entry title="From Header">
  <input class="setting-input" name="mailer_from"
    type="text" value="${publisher.from}">
</s:entry>
