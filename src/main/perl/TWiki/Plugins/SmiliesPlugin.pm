#
# Copyright (C) 2000-2001 Andrea Sterbini, a.sterbini@flashnet.it
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details, published at 
# http://www.gnu.ai.mit.edu/copyleft/gpl.html 
#
# =========================
#
# This plugin replaces smilies with small smilies bitmaps
# see TWiki.SmiliesPlugin
#
# =========================
package TWiki::Plugins::SmiliesPlugin;
# =========================
use vars qw($web $topic $user $installWeb $VERSION
	    %smiliesUrls %smiliesEmotions %smiliesPatterns
	    $smiliesPattern );
$VERSION = '1.000';

$smiliesPattern = '^\s*\|\s*<nop>([^\s|]+)\s*\|\s*%ATTACHURL%\/([^\s]+)\s*\|\s*"([^"|]+)"\s*\|\s*$';
#                          smilie       url            emotion
# =========================
sub initPlugin
{
    ( $topic, $web, $user, $installWeb ) = @_;    

    my $SmilieFile   = "$TWiki::dataDir/$installWeb/SmiliesPlugin.txt"; 

    #FIXME: use readWebTopic
    open(IN, "<$SmilieFile") or print "Cannot open $SmilieFile";  # Later warn?
    my @lines = <IN>;
    close IN;

    @lines = grep { /$smiliesPattern/ } @lines; # catch only the right lines

    my $line = "";
    foreach $line (@lines)
    {
	if ( $line =~ /$smiliesPattern/ ) 
	{
	$smiliesPatterns{$1} = "\Q$1\E";
	$smiliesUrls{$1}     = $2;
	$smiliesEmotions{$1} = $3;
	}
    }
    
    # Initialization OK
    return 1;
}
# =========================
sub commonTagsHandler
{
#    my ( $text, $topic, $web ) = @_;
    $_[0] =~ s/%SMILIES%/&allSmiliesTable()/geo;
}
# =========================
sub outsidePREHandler
{
#    my ( $text, $web ) = @_;
    my $key;
    my $url;
    my $emotion;
    my $p;
    while ( ($key,$p ) = each %smiliesPatterns ) 
    {
	$emotion  = $smiliesEmotions{$key};
	$url      = $smiliesUrls{$key};
        $_[0] =~ s/(\s|^)$p(\s|$)/"$1<IMG alt=\"$emotion\" src=\"$TWiki::pubUrlPath\/$installWeb\/SmiliesPlugin\/$url\">$2"/ge;
    }
}
# =========================
#sub insidePREHandler
#{
#    my ( $text, $web ) = @_;
#}
# =========================
sub allSmiliesTable
{
    my $text = "| *What to Type* | *Graphic That Will Appear* | *Emotion* |\n";

#    my ($k, $a, $b);
    foreach $k ( sort { $smiliesEmotions{$b} cmp $smiliesEmotions{$a} } 
		keys %smiliesEmotions )
    {
        $text .= "| <nop>$k | $k | ". $smiliesEmotions{$k} ." |\n";
    }
    return $text;
}

# =========================

1;
