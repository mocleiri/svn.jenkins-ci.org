<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:section title="E-mail Notification">
  <s:entry title="SMTP server"
           help="/help/tasks/mailer/smtp-server.html">
    <input class="setting-input" name="mailer_smtp_server"
      type="text" value="${descriptor.smtpServer}">
  </s:entry>
  <s:entry title="System Admin E-mail Address"
           help="/help/tasks/mailer/admin-address.html">
    <input class="setting-input" name="mailer_admin_address"
      type="text" value="${descriptor.adminAddress}">
  </s:entry>
  <s:entry title="Hudson URL"
           help="/help/tasks/mailer/url.html">
    <input class="setting-input" name="mailer_hudson_url"
      type="text" value="${descriptor.url}">
  </s:entry>
</s:section>
