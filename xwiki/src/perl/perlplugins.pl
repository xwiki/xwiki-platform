#!/bin/perl

# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details, published at 
# http://www.gnu.org/copyleft/lgpl.html

use strict ;
use CGI qw(:standard);
use TWiki::Doc;
use TWiki;
use TWiki::Plugins;
use TWiki::Func;

my $perlplugin;

# Java requirements;
use Inline (
	JAVA => 'STUDY',
	SHARED_JVM => 1,
	PORT => 7890,
	DEBUG => 1,
	STUDY => ["com.xpn.xwiki.render.XWikiPerlPluginCaller"],
	CLASSPATH => "c:/dev/java/xwiki/build",
	AUTOSTUDY => "1",
) ;

sub stop {
 $perlplugin->StopCallbackLoop();
}


sub render {
 my $content = shift ;
 my $doc = shift ;
 my $result = "";
 my $webName = $doc->getWeb();
 my $topic = $doc->getName();
 TWiki::Func::cacheTopic($webName, $topic, $content);

 $content = &TWiki::handleAllTags( $webName, $topic, $content, "" );
 $content = &TWiki::getRenderedVersion( $content );
 return $content;
} 

print "Starting perl server\n";
$perlplugin = com::xpn::xwiki::render::XWikiPerlPluginCaller->new();
&TWiki::initialize( "", "ludovic", "WebHome", "", "" );


# This is testing code..
if ($ARGV[0] eq "1") {
 my $content; 
 my $doc = TWiki::Doc->new();

 # Test Calendar
 $content = render("%CALENDAR%", $doc);
 print $content . "\n";

 # Test Table 
 $content = render("%TABLE{}%\n| *Title1*| *Title2* |\n| line1col1 | line1col2 |\n|line2col1 |line1col2 |\n<br>\n", $doc);
 print $content . "\n";
} else {
 print "Start callback loop\n";
 $perlplugin->StartCallbackLoop() ;
 print "Stopping perl server\n";
}
