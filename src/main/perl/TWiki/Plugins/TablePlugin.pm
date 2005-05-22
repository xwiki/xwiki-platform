#
# TWiki WikiClone ($wikiversion has version info)
#
# Copyright (C) 2001 John Talintyre, jet@cheerful.com
# Copyright (C) 2001 Peter Thoeny, peter@thoeny.com
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
#
# Allow sorting of tables, plus setting of background colour for headings and data cells
# see TWiki.TablePlugin for details of use


# =========================
package TWiki::Plugins::TablePlugin;

use Time::Local;


# =========================
use vars qw(
        $web $topic $user $installWeb $VERSION $debug
        $insideTABLE $tableCount @curTable $sortCol $requestedTable $up
        $doBody $doAttachments $currTablePre
        $tableBorder $cellPadding $cellSpacing $headerAlign $dataAlign $vAlign
        $headerBg $doSort $twoCol @dataBg @isoMonth
        @fields $upchar $downchar $diamondchar $url $curTablePre
        @isoMonth %mon2num $initSort $initDirection
    );

$VERSION = '1.000';
$currTablePre = "";
$upchar = "";
$downchar = "";
$diamondchar = "";
@isoMonth     = ( "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" );
{ my $count = 0;
  %mon2num = map { $_ => $count++ } @isoMonth;
}
@fields = ( "text", "attributes", "th td X", "numbers", "dates" );
# X means a spanned cell

# =========================
sub initPlugin
{
    ( $topic, $web, $user, $installWeb ) = @_;

    # check for Plugins.pm versions
    if( $TWiki::Plugins::VERSION < 1 ) {
        &TWiki::Func::writeWarning( "Version mismatch between TablePlugin and Plugins.pm" );
        return 0;
    }

    # Get plugin debug flag
    $debug = &TWiki::Func::getPreferencesFlag( "TABLEPLUGIN_DEBUG" );

    $insideTABLE = 0;
    $tableCount = 0;

    $twoCol = 1;

    my $cgi = &TWiki::Func::getCgiQuery();
    #if( ! $cgi ) {
    #    return 0;
    #}

    #my $plist = $cgi->query_string();
    #$plist =~ s/\;/\&/go;
    #$plist =~ s/\&?sortcol.*up=[0-9]+\&?//go;
    #$plist .= "\&" if $plist;
    #$url = $cgi->url . $cgi->path_info() . "?" . $plist;
    #$url =~ s/\&/\&amp;/go;

    #$sortCol = $cgi->param( 'sortcol' );
    #$requestedTable = $cgi->param( 'table' );
    #$up = $cgi->param( 'up' );

    $doBody = 0;
    $doAttachments = 0;
    my $tmp = &TWiki::Func::getPreferencesValue( "TABLEPLUGIN_SORT" );
    if( ! $tmp || $tmp =~ /^all$/oi ) {
        $doBody = 1;
        $doAttachments = 1;
    } elsif( $tmp =~ /^attachments$/oi ) {
        $doAttachments =1;
    }

    setDefaults();

    # Plugin correctly initialized
    &TWiki::Func::writeDebug( "- TWiki::Plugins::TablePlugin::initPlugin( $web.$topic ) is OK" ) if $debug;
    return 1;
}

# =========================
sub setDefaults
{
    $doSort = $doBody;
    my $val = &TWiki::Func::getPreferencesValue( "TABLEPLUGIN_TABLEBORDER" );
    $tableBorder = ( $val ne "" ) ? $val : "1";   # distinguish "" and "0"
    $val = &TWiki::Func::getPreferencesValue( "TABLEPLUGIN_CELLSPACING" );
    $cellSpacing = ( $val ne "" ) ? $val : "1";
    $val = &TWiki::Func::getPreferencesValue( "TABLEPLUGIN_CELLPADDING" );
    $cellPadding = ( $val ne "" ) ? $val : "1";
    $headerAlign = &TWiki::Func::getPreferencesValue( "TABLEPLUGIN_HEADERALIGN" ) || "";
    $dataAlign   = &TWiki::Func::getPreferencesValue( "TABLEPLUGIN_DATAALIGN" )   || "";
    $vAlign      = &TWiki::Func::getPreferencesValue( "TABLEPLUGIN_VALIGN" )      || "";
    $headerBg    = &TWiki::Func::getPreferencesValue( "TABLEPLUGIN_HEADER_BG" )   || "#99CCCC";
    @dataBg = split( /,\s*/, ( &TWiki::Func::getPreferencesValue( "TABLEPLUGIN_DATA_BG" ) || "" ) );
    undef $initSort;
}

# =========================
# Convert text to number and date if possible
sub getTypes
{
    my( $text ) = @_;

    $text =~ s/&nbsp;/ /go;

    my $num = undef;
    my $date = undef;
    if( $text =~ /^\s*$/ ) {
        $num = 0;
        $date = 0;
    }

    if( $text =~ /^\s*([0-9]{1,2})\s*([A-Z][a-z][a-z])\s*([0-9]{4})\s*-\s*([0-9][0-9]):([0-9][0-9])\s*$/ ) {
        $date = timegm(0, $5, $4, $1, $mon2num{$2}, $3 - 1900);
    } elsif( $text =~ m|^\s*([0-9]{1,2})[-\s/]([A-Z][a-z][a-z])[-\s/]([0-9]{2,4})\s*$| ) {
        my $year = $3;
        $year += 1900 if( length( $year ) == 2 && $year > 80 );
        $year += 2000 if( length( $year ) == 2 );
        $date = timegm( 0, 0, 0, $1, $mon2num{$2}, $year - 1900 );
    } elsif ( $text =~ /^\s*[0-9]+(\.[0-9]+)?\s*$/ ) {
        $num = $text;
    }

    return( $num, $date );
}


# =========================
sub processTR {
    my ( $thePre, $theRow ) = @_;

    $currTablePre = $thePre || "";
    my $attr = "";
    my $span = 0;
    my $l1 = 0;
    my $l2 = 0;
    if( ! $insideTABLE ) {
        @curTable = ();
        $tableCount++;
    }
    $theRow =~ s/\t/   /go;  # change tabs to space
    $theRow =~ s/\s*$//o;    # remove trailing spaces
    $theRow =~ s/(\|\|+)/$TWiki::TranslationToken . length($1) . "\|"/geo;  # calc COLSPAN
    my $colCount = 0;
    my @row = ();
    $span = 0;
    my $value = "";
    foreach( split( /\|/, $theRow ) ) {
        $colCount++;
        $attr = "";
        $span = 0;
        #AS 25-5-01 Fix to avoid matching also single columns
        if ( s/$TWiki::TranslationToken([0-9]+)// ) { # No o flag for mod-perl compatibility
            $span = $1;
            $attr = " colspan=\"$span\"" ;
        }
        s/^\s+$/ &nbsp; /o;
        /^(\s*).*?(\s*)$/;
        $l1 = length( $1 || "" );
        $l2 = length( $2 || "" );
        if( $l1 >= 2 ) {
            if( $l2 <= 1 ) {
                $attr .= ' align="right"';
            } else {
                $attr .= ' align="center"';
            }
        }
        if( /^\s*\*(.*)\*\s*$/ ) {
            $value = $1;
            $attr  = " align=\"$headerAlign\"" if $headerAlign; # override $attr
            $attr .= " valign=\"$vAlign\"" if $vAlign;
            push @row, [ $value, "$attr", "th" ];
        } else {
            if( /^\s*(.*)\s*$/ ) {   # strip white spaces
                $_ = $1;
            }
            $value = $_;
            $attr  = " align=\"$dataAlign\"" if $dataAlign; # override $attr
            $attr .= " valign=\"$vAlign\"" if $vAlign;
            push @row, [ $value, "$attr", "td" ];
        }
        while( $span > 1 ) {
            push @row, [ $value, "", "X" ];
            $span--;
        }
    }
    push @curTable, \@row;
    return "$currTablePre<nop>"; # Avoid TWiki converting empty lines to new paras
}

# =========================
# Do sort?
sub doIt
{
    my( $header ) = @_;

    # Attachments table?
    if( $header->[0]->[0] =~ /FileAttachment/ ) {
        return $doAttachments;
    }

    my $doIt = $doSort;
    if( $doSort ) {
        # All cells in header are headings?
        foreach $cell ( @$header ) {
            if( $cell->[2] ne "th" ) {
                $doIt = 0;
                last;
            }
        }
    }

    return $doIt;
}

# =========================
# Is a colum a date (4), number (3) or text (0)?
sub colType
{
   my( $col ) = @_;
   my $isDate = 1;
   my $isNum  = 1;
   my $num = "";
   my $date = "";
   foreach my $row ( @curTable ) {
       ( $num, $date ) = getTypes( $row->[$col]->[0] );
       $isDate = 0 if( ! defined( $date ) );
       $isNum  = 0 if( ! defined( $num ) );
       last if( !$isDate && !$isNum );
       $row->[$col]->[4] = $date;
       $row->[$col]->[3] = $num;
   }

   if( $isDate ) {
       return 4;
   } elsif( $isNum ) {
       return 3;
   } else {
       return 0;
   }
}


# =========================
sub stripHtml
{
   my( $text ) = @_;
   $text =~ s/\&nbsp;/ /go;                     # convert space
   $text =~ s/\[\[[^\]]+\]\[([^\]]+)\]\]/$1/go; # extract label from [[...][...]] link
   $text =~ s/<[^>]+>//go;                      # strip HTML
   $text =~ s/^ *//go;                          # strip leading space space
   $text = lc( $text );                         # convert to lower case
   return $text;
}

# =========================
sub emitTable
{
    my $direction = $up ? 0 : 1;
    my $doIt = doIt( $curTable[0] );
    my $text = "$currTablePre<table border=\"$tableBorder\" "
             . "cellspacing=\"$cellSpacing\" cellpadding=\"$cellPadding\">\n";
    my $type = "";
    my $attr = "";
    my $stype = "";

    #Added to aid initial sorting direction and column : ShawnBradford 20020221
    if ( defined( $sortCol ) ) {
        undef $initSort;
    } else {
        $sortCol = $initSort - 1;
        $up = $initDirection;
        $direction = $up ? 0 : 1;
    }

    if( ((defined( $sortCol ) && $requestedTable eq $tableCount)) || defined( $initSort ) ) {
        my $header = shift @curTable;
        $stype = colType( $sortCol );
        &TWiki::Func::writeDebug( "- TWiki::Plugins::TablePlugin sorting col $sortCol as $fields[$stype]" ) if $debug;
        if( $stype ) {
            if( $up ) {
                @curTable = sort { $b->[$sortCol]->[$stype] <=> $a->[$sortCol]->[$stype] } @curTable;
            } else {
                @curTable = sort { $a->[$sortCol]->[$stype] <=> $b->[$sortCol]->[$stype] } @curTable;
            }

        } else {
            if( $up ) {
                # efficient way of sorting stripped HTML text
                @curTable = map { $_->[0] }
                            sort { $b->[1] cmp $a->[1] }
                            map { [ $_, stripHtml( $_->[$sortCol]->[0] ) ] } @curTable;
            } else {
                @curTable = map { $_->[0] }
                            sort { $a->[1] cmp $b->[1] }
                            map { [ $_, stripHtml( $_->[$sortCol]->[0] ) ] } @curTable;
            }
        }
        unshift @curTable, $header;
    }
    my $rowCount = 0;
    my $arrow = "";
    my $color = "";
    foreach my $row ( @curTable ) {
        $text .= "$currTablePre<tr>";
        my $colCount = 0;
        foreach my $fcell ( @$row ) {
            $arrow = "";
            next if( $fcell->[2] eq "X" ); # data was there so sort could work with col spanning
            $type = $fcell->[2];
            my $cell = $fcell->[0];
            my $attr = $fcell->[1];
            if( $type eq "th" ) {

               if( ! $upchar ) {
                   # Added arrow images for up and down S. Bradford 20011018
                   # PTh 13 Nov 2001: Modfied and moved to TablePlugin attachment
                   $upchar = "<img src=\"%PUBURL%/$installWeb/TablePlugin/up.gif\" />";
                   $upchar = &TWiki::Func::expandCommonVariables( $upchar, $topic );
                   $downchar = "<img src=\"%PUBURL%/$installWeb/TablePlugin/down.gif\" />";
                   $downchar = &TWiki::Func::expandCommonVariables( $downchar, $topic );
                   $diamondchar = "<img src=\"%PUBURL%/$installWeb/TablePlugin/diamond.gif\" />";
                   $diamondchar = &TWiki::Func::expandCommonVariables( $diamondchar, $topic );
               }

               $attr .= " bgcolor=\"$headerBg\"";
               my $dir = 0;
               $dir = $direction if( defined( $sortCol ) && $colCount == $sortCol );
               if( defined( $sortCol ) && $colCount == $sortCol && $stype ne "" ) {
                   $arrow = "<a name=\"sorted_table\"><span title=\"$fields[$stype] ";
                   if( $dir == 0 ) {
                       $arrow .= "sorted ascending\">$upchar</span></a>";
                   } else {
                       $arrow .= "sorted descending\">$downchar</span></a>";
                   }
               }
               if( $doIt && $rowCount == 0 ) {
                  if( $cell =~ /\[\[|href/o ) {
                     $cell .= " <a href=\"" . $url
                           . "sortcol=$colCount&amp;table=$tableCount&amp;up=$dir#sorted_table\" "
                           . "title=\"Sort by this column\">$diamondchar</a>$arrow";
                  } else {
                     $cell = "<a href=\"" . $url
                           . "sortcol=$colCount&amp;table=$tableCount&amp;up=$dir#sorted_table\" "
                           . "title=\"Sort by this column\">$cell</a> $arrow";
                  }
               } else {
                  $cell = "*$cell*";
               }
            } elsif( @dataBg ) {
               my $color = $dataBg[$rowCount % ($#dataBg + 1) ];
               $attr .= " bgcolor=\"" . $color . "\"" unless( $color =~ /none/i );
            }

    &TWiki::Func::writeDebug( "- TablePlugin::cell: $cell" ) ;

            $cell =~ s/%._.N._.%/\r\n/go;
            $text .= "<$type $attr> $cell </$type>";
            $colCount++;
        }
        $text .= "</tr>\n";
        $rowCount++;
    }
    $text .= "$currTablePre</table>\n";
    setDefaults();
 
# handle special char for new lines in textarea
    return $text;
}


# =========================
# Different settings for a specific table
sub override
{
    my( $args ) = @_;

    #Defines which column to initially sort : ShawnBradford 20020221
    my $tmp = &TWiki::Func::extractNameValuePair( $args, "initsort" );
    $initSort = $tmp if ( $tmp );

    #Defines which direction to sort the column set by initsort : ShawnBradford 20020221
    $tmp = &TWiki::Func::extractNameValuePair( $args, "initdirection" );
    $initDirection = 0;
    $initDirection = 1 if ( $tmp =~/^up$/oi );

    $tmp = &TWiki::Func::extractNameValuePair( $args, "sort" );
    $tmp = "0" if( $tmp =~ /^off$/oi );
    $doSort = $tmp if( defined $tmp && $tmp ne "" );

    $tmp = &TWiki::Func::extractNameValuePair( $args, "tableborder" );
    $tableBorder = $tmp if( defined $tmp && $tmp ne "" );

    $tmp = &TWiki::Func::extractNameValuePair( $args, "cellpadding" );
    $cellPadding = $tmp if( defined $tmp && $tmp ne "" );

    $tmp = &TWiki::Func::extractNameValuePair( $args, "cellspacing" );
    $cellSpacing = $tmp if( defined $tmp && $tmp ne "" );

    $tmp = &TWiki::Func::extractNameValuePair( $args, "headeralign" );
    $headerAlign = $tmp if( $tmp );

    $tmp = &TWiki::Func::extractNameValuePair( $args, "dataalign" );
    $dataAlign = $tmp if( $tmp );

    $tmp = &TWiki::Func::extractNameValuePair( $args, "valign" );
    $vAlign = $tmp if( $tmp );

    $tmp = &TWiki::Func::extractNameValuePair( $args, "headerbg" );
    $headerBg = $tmp if( $tmp );

    $tmp = &TWiki::Func::extractNameValuePair( $args, "databg" );
    @dataBg = split( /,\s*/, $tmp ) if( $tmp );

    return "$currTablePre<nop>";
}


# =========================
sub outsidePREHandler
{
### my ( $text ) = @_;   # do not uncomment, use $_[0] instead

    #&TWiki::Func::writeDebug( "- TablePlugin::outsidePREHandler( $web.$topic )" ) if $debug;

    # Table of format: | cell | cell |
    # PTh 25 Jan 2001: Forgiving syntax, allow trailing white space
    $_[0] =~ s/%TABLE{(.*)}%/&override($1)/eo;
    if( $_[0] =~ /^(\s*)\|.*\|\s*$/ ) {
        $_[0] =~ s/^(\s*)\|(.*)/&processTR($1,$2)/eo;
        $insideTABLE = 1;
    } elsif( $insideTABLE ) {
        $_[0] = &emitTable() . "$_[0]";
        $insideTABLE = 0;
        undef $initSort;
    }

    # This handler is called by getRenderedVersion, in loop outside of <PRE> tag.
    # This is the place to define customized rendering rules.
    # Note: This is an expensive function to comment out.
    # Consider startRenderingHandler instead
}

# =========================
sub endRenderingHandler
{
### my ( $text ) = @_;   # do not uncomment, use $_[0] instead

    #&TWiki::Func::writeDebug( "- TablePlugin::endRenderingHandler( $web.$topic )" ) if $debug;

    # This handler is called by getRenderedVersion just after the line loop
    if( $insideTABLE ) {
        $_[0] .= emitTable();
        $insideTABLE = 0;
        undef $initSort;
    }
}

# =========================
1;
