<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" tagdir="/WEB-INF/tags/form" %>

<s:entry title="Days to keep records"
  help="/help/project-config/log-days.html">
  <input class="setting-input" name="logrotate_days"
    type="text" value="${it.logRotator.daysToKeepStr}">
</s:entry>
<s:entry title="Max # of records to keep"
         help="/help/project-config/log-nums.html">
  <input class="setting-input" name="logrotate_nums"
    type="text" value="${it.logRotator.numToKeepStr}">
</s:entry>
