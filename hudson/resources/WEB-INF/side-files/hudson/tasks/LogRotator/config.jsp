<s:entry title="Days to keep records"
  description="if not empty, log/artifacts etc are only kept up to this number of days">
  <input class="setting-input" name="logrotate_days"
    type="text" value="${it.logRotator.daysToKeepStr}">
</s:entry>
<s:entry title="Max # of records to keep"
  description="if not empty, only up to this number of log/artifacts etc are kept">
  <input class="setting-input" name="logrotate_nums"
    type="text" value="${it.logRotator.numToKeepStr}">
</s:entry>
