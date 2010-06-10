#!/usr/bin/perl

## 
$source="/usr/src/redhat/greenballs.hpi";  // will need to be made dynamic somehow, and copied to this location

use XML::Simple;
use Data::Dumper;

$xml = new XML::Simple;
$data = $xml->XMLin("pom.xml");     // will need to be made dynamic somehow

$name="$data->{artifactId}";
$version="$data->{version}";
$version=~s/-/./;                   // remove hyphens, they are not allowed in rpm versions.
$summary="$data->{description}";

$url="$data->{url}";
$license="$data->{licenses}->{license}->{name}";
$buildroot="%{_tmppath}/build-%{name}-%{version}";

// the output from this print is redirected to a file in /usr/src/redhat/SPECS
print "
%define _prefix %{_usr}/lib/hudson
%define workdir %{_var}/lib/hudson

%define debug_package %{nil}

Name: hudson-$name
Version: $version
Release: 1
Summary: $summary
URL: $url
Group: Development/Tools/Building
License: $license
BuildRoot: %{_tmppath}/build-%{name}-%{version}

Source: $source

PreReq: hudson
BuildArch: noarch

%description
$summary

%prep
%setup -q -T -c

%build

%install
%__install -d \"%{buildroot}${workdir}\"
%__install -d \"%{buildroot}${workdir}/plugins\"

%__install -D -m0644 \"%{SOURCE0}\" \"%{buildroot}%{workdir}/plugins/$name.hpi\"

%post
/sbin/server hudson restart

%clean
%__rm -rf \"%{buildroot}\"

%files
%defattr(-,root,root)
%{workdir}/plugins/$name.hpi
";
