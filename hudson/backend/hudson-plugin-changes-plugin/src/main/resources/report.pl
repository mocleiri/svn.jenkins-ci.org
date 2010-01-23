# Perl script to generate report of unreleased plugin changes in Hudson's subversion repository.
# @author Alan.Harder@sun.com
# %knownRevs, %skipTag, %skipEntry, %tagMap will be prepended before script is run.
#
# %knownRevs = Map<pluginDir-rev-revcount,message>
#   If plugin "pluginDir" was last released at revision "rev" and there have been "revcount"
#   revisions since then, show the given message in the report instead of the usual message.
#
# %skipTag = Map<tag,anything>
#   Ignore these entries from /svn/hudson/tags.
# %skipEntry = Map<entry,anything>
#   Ignore these entries from /svn/hudson/trunk/hudson/plugins.
#
# %tagMap = Map<tagBase,pluginDir-or-skip>
#   Tags (without "-version#") to either ignore or map to the right pluginDir.

# Get list of all tags, split into plugin name and version:
my $base = 'https://svn.dev.java.net/svn/hudson'; my $tags = "$base/tags";
my $pluginUrl = 'http://fisheye.hudson-ci.org/browse/Hudson/trunk/hudson/plugins';
my $issueUrl = 'http://issues.hudson-ci.org/browse';
my ($ver, $tagrev, $cnt, $d1, $d2, $known, $since, $p, $x, %x);
open(LS,"svn ls $tags |") or die;
while (<LS>) {
  push(@{$x{$1}}, $2) if m!^(.*)-([\d._]+)/?$! and not exists $skipTag{"$1-$2"};
}
close LS;

# Get "Last Changed Rev" of latest version of each plugin, then get more recent revs in trunk
foreach $x (sort keys %x) {
  next if ($p = exists $tagMap{$x} ? $tagMap{$x} : $x) eq 'skip';
  $skipEntry{$p} = 1;
  $ver = (sort byver @{$x{$x}})[0];
  ($cnt, $d1, $d2, $known) = &revcount($p,$tagrev=&tagrev("$x-$ver"));
  $p = "[$p|$pluginUrl/$p]";
  print "| $p $ver | CURRENT\n" if $cnt == 0;
  $since = "| since $ver | [r$tagrev|http://hudson-ci.org/commit/$tagrev] |";
  print "| $p | $cnt rev", ($cnt > 1 ? "s $since $d1 to $d2" : " $since $d1"), " | $known\n"
    if ($known or $cnt > 0);
}

# List unreleased plugins
open(LS,"svn ls $base/trunk/hudson/plugins |") or die;
$since = '| unreleased |';
while (<LS>) {
  chomp; ($p = $_) =~ s!/$!!;
  next if exists $skipEntry{$p};
  ($cnt, $d1, $d2) = &revcount($p,0);
  print "| [$p|$pluginUrl/$p] | $cnt rev", ($cnt > 1 ? "s $since $d1 to $d2" : " $since $d1"), "\n";
}
close LS;

foreach my $key (keys %knownRevs) {
  print "| Unused data in KnownRevs: | $key | $knownRevs{$key}\n";
}

sub byver {
  my ($i,$x,@a,@b);
  @a=split(/[._]/, $a);
  @b=split(/[._]/, $b);
  for ($i=0; $i<@a; $i++) {
    return $x if ($x = $b[$i] <=> $a[$i]);
  }
  return @b > @a ? 1 : 0;
}

sub tagrev {
  my ($tag, $rev) = ($_[0], '');
  open(TAG, "svn info $tags/$tag |") or die;
  while (<TAG>) {
    do { $rev = $1; <TAG>; last; } if /^Last Changed Rev:\s*(\d+)/;
  }
  close TAG;
  return $rev;
}

sub revcount {
  my ($plugin, $fromrev, $cnt, $d, $d1, $d2, @fixed) = ($_[0], $_[1]+1, 0);
  open(IN,"svn log -r $fromrev:HEAD $base/trunk/hudson/plugins/$plugin |") or die;
  while (<IN>) {
    if (/^r\d+ \| [^|]* \| ([\d-]+) /) { $cnt++; $d = $1 }
    if (/^bumping up POM version|prepare for next development iteration/) { $cnt--; $d = '' }
    while (s/FIXED ([A-Z]+-(\d+))//) { push(@fixed, "[$2|$issueUrl/$1]") }
    if (/^---------/ and $d) { $d2 = $d; $d1 = $d unless $d1 }
  }
  close IN;
  $d = delete $knownRevs{"$plugin-".($fromrev-1)."-$cnt"};
  $d = 'Fixed: ' . join(' ', @fixed) unless ($d or !@fixed);
  return ($cnt, $d1, $d2, $d);
}

