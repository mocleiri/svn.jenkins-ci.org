// displays one line HTML summary of the build, which includes the difference
// from the previous build
//
// Usage: <buildStatusSummary build="${...}" />

import hudson.Util;
import hudson.model.*;

jelly {
  def prev = build.getPreviousBuild();

  switch(build.result) {
  case Result.SUCCESS:
    if(prev.result==Result.SUCCESS) {
      output.write("stable");
    } else {
      output.write("back to normal");
    }
    return;

  case Result.FAILURE:
    def since = build.getPreviousNotFailedBuild();
    if(since==null) {
      output.write("broken for a long time");
      return;
    }
    if(since==prev) {
      output.write("<font color=red>broken since this build</font>");
      return;
    }

    output.write("broken since #"+since.getNumber());
    return;

  case Result.ABORTED:
    output.write("aborted");
    return;

  case Result.UNSTABLE:
    def trN = build.testResultAction;
    def trP = prev==null ? null : prev.testResultAction;

    if(trP==null) {
      if(trN!=null && trN.failCount>0) {
        output.write(Util.combine(trN.failCount,"test failure"));
      } else {// ???
        output.write("unstable");
      }
      return;
    }

    if(trP.failCount==0) {
      output.write("<font color=red>"+Util.combine(trP.failCount,"test")+" started to fail</font>");
      return;
    }
    if(trP.failCount < trN.failCount) {
      output.write("<font color=red>"+Util.combine(trN.failCount-trP.failCount,"more test")
        +" are failing (${trN.failCount} total)</font>");
      return;
    }
    if(trP.failCount > trN.failCount) {
      output.write(Util.combine(trP.failCount-trN.failCount,"less test")
        +" are failing (${trN.failCount} total)");
      return;
    }
    output.write(Util.combine(trN.failCount,"test")+" are still failing");
    return;

  default:
    output.write("-");
    return;
  }
}
