#! /usr/bin/perl -w

use strict;
use CGI;

my $cgi = new CGI;
my $text1 = $cgi->param('text1');
my $text2 = $cgi->param('text2');

print "Content-type: text/html\n\n";

print "<p>You submitted:</p>";
print "<table border='1'>";
print "<thead><tr bgcolor='#cccccc'><td width='50%'>text1</td><td width='50%'>text2</td></tr></thead>";
print "<tbody><tr><td>$text1</td><td>$text2</td></tr></tbody>";
print "</table>";
