#
# TWiki WikiClone ($wikiversion has version info)
#
# Copyright (C) 2001 Peter Thoeny, Peter@Thoeny.com
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
# http://www.gnu.org/copyleft/gpl.html
#
# =========================
#
# This is the spreadsheet TWiki plugin.
#
# Each plugin is a package that contains the subs:
#
#   initPlugin           ( $topic, $web, $user )
#   commonTagsHandler    ( $text, $topic, $web )
#   startRenderingHandler( $text, $web )
#   outsidePREHandler    ( $text )
#   insidePREHandler     ( $text )
#   endRenderingHandler  ( $text )
#
# initPlugin is required, all other are optional.
# For increased performance, DISABLE handlers you don't need.

# =========================
package TWiki::Plugins::SpreadSheetPlugin;

# =========================
use vars qw(
        $web $topic $user $installWeb $VERSION $debug
        $renderingWeb @tableMatrix $cPos $rPos
    );

$VERSION = '1.000';

# =========================
sub initPlugin
{
    ( $topic, $web, $user, $installWeb ) = @_;

    # check for Plugins.pm versions
    if( $TWiki::Plugins::VERSION < 1 ) {
        &TWiki::Func::writeWarning( "Version mismatch between SpreadSheetPlugin and Plugins.pm" );
        return 0;
    }

    $renderingWeb = $web;

    # Get plugin debug flag
    $debug = &TWiki::Func::getPreferencesFlag( "SPREADSHEETPLUGIN_DEBUG" );

    # Plugin correctly initialized
    &TWiki::Func::writeDebug( "- TWiki::Plugins::SpreadSheetPlugin::initPlugin( $web.$topic ) is OK" ) if $debug;
    return 1;
}

# =========================
sub commonTagsHandler
{
### my ( $text, $topic, $web ) = @_;   # do not uncomment, use $_[0], $_[1]... instead

    &TWiki::Func::writeDebug( "- SpreadSheetPlugin::commonTagsHandler( $_[2].$_[1] )" ) if $debug;

    if( ! ( $_[0] =~ /%CALC\{.*?\}%/ ) ) {
        # nothing to do
        return;
    }

    @tableMatrix = ();
    $cPos = -1;
    $rPos = -1;

    my $result = "";
    my $insidePRE = 0;
    my $insideTABLE = 0;
    my $line = "";
    my $before = "";
    my $cell = "";
    my @row = ();

    $_[0] =~ s/\r//go;
    $_[0] =~ s/\\\n//go;  # Join lines ending in "\"
    foreach( split( /\n/, $_[0] ) ) {

        # change state:
        m|<pre>|i       && ( $insidePRE = 1 );
        m|<verbatim>|i  && ( $insidePRE = 1 );
        m|</pre>|i      && ( $insidePRE = 0 );
        m|</verbatim>|i && ( $insidePRE = 0 );

        if( ! ( $insidePRE ) ) {

            if( /^\s*\|.*\|\s*$/ ) {
                # inside | table |
                if( ! $insideTABLE ) {
                    $insideTABLE = 1;
                    @tableMatrix = ();  # reset table matrix
                    $cPos = -1;
                    $rPos = -1;
                }
                $line = $_;
                $line =~ s/^(\s*\|)(.*)\|\s*$/$2/o;
                $before = $1;
                @row  = split( /\|/o, $line, -1 );
                push @tableMatrix, [ @row ];
                $rPos++;
                $line = "$before";
                for( $cPos = 0; $cPos < @row; $cPos++ ) {
                    $cell = $row[$cPos];
                    $cell =~ s/%CALC\{(.*?)\}%/&doCalc($1)/geo;
                    $line .= "$cell|";
                }
                s/.*/$line/o;

            } else {
                # outside | table |
                if( $insideTABLE ) {
                    $insideTABLE = 0;
                }
                s/%CALC\{(.*?)\}%/&doCalc($1)/geo;
            }
        }
        $result .= "$_\n";
    }
    $_[0] = $result;
}

# =========================
sub doCalc
{
    my( $theAttributes ) = @_;
    my $text = &TWiki::extractNameValuePair( $theAttributes );
    $text = doFunc( "MAIN", $text );

    if( ( $rPos >= 0 ) && ( $cPos >= 0 ) ) {
        # update cell in table matrix
        $tableMatrix[$rPos][$cPos] = $text;
    }

    return $text;
}

# =========================
sub doFunc
{
    my( $theFunc, $theAttr ) = @_;

    &TWiki::writeDebug( "- SpreadSheetPlugin::doFunc: $theFunc( $theAttr ) start" ) if $debug;

    # FIXME: greedy match fails with: 'tt(tt()+tt(tt()))'
    $theAttr =~ s/\$([A-Z]+)\((.*)\)/&doFunc($1,$2)/geo;

    my $result = "";
    my $i = 0;
    if( $theFunc eq "MAIN" ) {
        $result = $theAttr;

    } elsif( $theFunc eq "EVAL" ) {
        # Allow only simple math
        $theAttr =~ s/[^\-\+\*\/0-9\.\(\)]*//go;
        $theAttr =~ /(.*)/;
        $theAttr = $1;  # untainted variable
        unless( $result = eval "$theAttr" ) {
            $result = "ERROR: $@" if $@;
        }

    } elsif( $theFunc eq "INT" ) {
        # Allow only simple math
        $theAttr =~ s/[^\-\+\*\/0-9\.\(\)]*//go;
        $theAttr =~ /(.*)/;
        $theAttr = $1;  # untainted variable
        unless( $result = eval "int( $theAttr )" ) {
            $result = "ERROR: $@" if $@;
        }

    } elsif( $theFunc eq "T" ) {
        $result = "";
        my @arr = getTableRange( "$theAttr..$theAttr" );
        if( @arr ) {
            $result = $arr[0];
        }

    } elsif( $theFunc eq "UPPER" ) {
        $result = uc( $theAttr );

    } elsif( $theFunc eq "LOWER" ) {
        $result = lc( $theAttr );

    } elsif( $theFunc eq "ROW" ) {
        $result = $rPos + $theAttr + 1;

    } elsif( $theFunc eq "COLUMN" ) {
        $result = $cPos + $theAttr + 1;

    } elsif( $theFunc eq "LEFT" ) {
        my $i = $rPos + 1;
        $result = "R$i:C0..R$i:C$cPos";

    } elsif( $theFunc eq "ABOVE" ) {
        my $i = $cPos + 1;
        $result = "R0:C$i..R$rPos:C$i";

    } elsif( $theFunc eq "MAX" ) {
        my @arr = sort { $a <=> $b }
                  grep { /./ }
                  getTableRangeAsFloat( $theAttr );
        $result = $arr[$#arr];

    } elsif( $theFunc eq "MIN" ) {
        my @arr = sort { $a <=> $b }
                  grep { /./ }
                  getTableRangeAsFloat( $theAttr );
        $result = $arr[0];

    } elsif( $theFunc eq "SUM" ) {
        $result = 0;
        my @arr = getTableRangeAsFloat( $theAttr );
        foreach $i ( @arr ) {
            $result += $i  if defined $i;
        }

    } elsif( $theFunc eq "AVERAGE" ) {
        $result = 0;
        my $items = 0;
        my @arr = getTableRangeAsFloat( $theAttr );
        foreach $i ( @arr ) {
            if( defined $i ) {
                $result += $i;
                $items++;
            }
        }
        if( $items > 0 ) {
            $result = $result / $items;
        }

    } elsif( $theFunc eq "COUNTITEMS" ) {
        $result = "";
        my @arr = getTableRange( $theAttr );
        my %items = ();
        my $key = "";
        foreach $key ( @arr ) {
            $key =~ s/^\s*(.*?)\s*$/$1/o;
            if( $key ) {
                if( exists( $items{ $key } ) ) {
                    $items{ $key }++;
                } else {
                    $items{ $key } = 1;
                }
            }
        }
        foreach $key ( sort keys %items ) {
            $result .= "$key: $items{ $key }<br> ";
        }
        $result =~ s/<br>$//o;

    }

    &TWiki::writeDebug( "- SpreadSheetPlugin::doFunc: $theFunc( $theAttr ) returns: $result" ) if $debug;
    return $result;
}

# =========================
sub getTableRangeAsInteger
{
    my( $theAttr ) = @_;

    my $val = 0;
    my @arr = getTableRange( $theAttr );
    (my $baz = "foo") =~ s/foo//;  # reset search vars. defensive coding
    for my $i (0 .. $#arr ) {
        $val = $arr[$i];
        # search first integer pattern
        if( $val =~ /^\s*([\-\+]*[0-9]+).*/o ) {
            $arr[$i] = $1;  # untainted variable, possibly undef
        } else {
            $arr[$i] = undef;
        }
    }
    return @arr;
}

# =========================
sub getTableRangeAsFloat
{
    my( $theAttr ) = @_;

    my $val = 0;
    my @arr = getTableRange( $theAttr );
    (my $baz = "foo") =~ s/foo//;  # reset search vars. defensive coding
    for my $i (0 .. $#arr ) {
        $val = $arr[$i] || "";
        # search first float pattern
        if( $val =~ /^\s*([\-\+]*[0-9\.]+).*/o ) {
            $arr[$i] = $1;  # untainted variable, possibly undef
        } else {
            $arr[$i] = undef;
        }
    }
    return @arr;
}

# =========================
sub getTableRange
{
    my( $theAttr ) = @_;

    my @arr = ();
    if( $rPos < 0 ) {
        return @arr;
    }

    &TWiki::writeDebug( "- SpreadSheetPlugin::getTableRange( $theAttr )" ) if $debug;
    $theAttr =~ /\s*R([0-9]+)\:C([0-9]+)\s*\.\.+\s*R([0-9]+)\:C([0-9]+)/;
    if( ! $4 ) {
        return @arr;
    }
    my $r1 = $1 - 1;
    my $c1 = $2 - 1;
    my $r2 = $3 - 1;
    my $c2 = $4 - 1;
    my $r = 0;
    my $c = 0;
    if( $c1 < 0     ) { $c1 = 0; }
    if( $c2 < 0     ) { $c2 = 0; }
    if( $c2 < $c1   ) { $c = $c1; $c1 = $c2; $c2 = $c; }
    if( $r1 > $rPos ) { $r1 = $rPos; }
    if( $r1 < 0     ) { $r1 = 0; }
    if( $r2 > $rPos ) { $r2 = $rPos; }
    if( $r2 < 0     ) { $r2 = 0; }
    if( $r2 < $r1   ) { $r = $r1; $r1 = $r2; $r2 = $r; }

    my $pRow = ();
    for $r ( $r1 .. $r2 ) {
        $pRow = $tableMatrix[$r];
        for $c ( $c1 .. $c2 ) {
            if( $c < @$pRow ) {
                push( @arr, $$pRow[$c] );
            }
        }
    }
    &TWiki::writeDebug( "- SpreadSheetPlugin::getTableRange() returns @arr" ) if $debug;
    return @arr;
}

# =========================
sub DISABLE_startRenderingHandler
{
### my ( $text, $web ) = @_;   # do not uncomment, use $_[0], $_[1] instead

    &TWiki::Func::writeDebug( "- SpreadSheetPlugin::startRenderingHandler( $$_[1] )" ) if $debug;

    # This handler is called by getRenderedVersion just before the line loop

    $renderingWeb = $_[1];
}

# =========================
sub DISABLE_outsidePREHandler
{
### my ( $text ) = @_;   # do not uncomment, use $_[0] instead

    &TWiki::Func::writeDebug( "- SpreadSheetPlugin::outsidePREHandler( $web.$topic )" ) if $debug;

    # This handler is called by getRenderedVersion, in loop outside of <PRE> tag
    # This is the place to define customized rendering rules

    # do custom extension rule, like for example:
    # $_[0] =~ s/old/new/go;
}

# =========================
sub DISABLE_insidePREHandler
{
### my ( $text ) = @_;   # do not uncomment, use $_[0] instead

    &TWiki::Func::writeDebug( "- SpreadSheetPlugin::insidePREHandler( $web.$topic )" ) if $debug;

    # This handler is called by getRenderedVersion, in loop inside of <PRE> tag
    # This is the place to define customized rendering rules

    # do custom extension rule, like for example:
    # $_[0] =~ s/old/new/go;
}

# =========================
sub DISABLE_endRenderingHandler
{
### my ( $text ) = @_;   # do not uncomment, use $_[0] instead

    &TWiki::Func::writeDebug( "- SpreadSheetPlugin::endRenderingHandler( $_[0] )" ) if $debug;

    # This handler is called by getRenderedVersion just after the line loop

}

# =========================

1;

# EOF
