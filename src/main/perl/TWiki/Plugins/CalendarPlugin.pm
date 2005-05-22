#
# Copyright (C) 2001 Andrea Sterbini, a.sterbini@flashnet.it
# Christian Schultze: debugging, relative month/year, highlight today
# Akim Demaille <akim@freefriends.org>: handle date intervals.
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
# This is a plugin for showing a Month calendar with events.
#
# =========================
package TWiki::Plugins::CalendarPlugin;

# use strict;

# =========================
use vars qw( $web $topic $user $installWeb $VERSION
	    $libsLoaded $libsError %defaults );

$VERSION    = '1.005';  #ad# support Date intervals
#$VERSION   = '1.004';  #as# only HTML::CalendarMonthSimple, ISO dates, options
#$VERSION   = '1.003';  #as# now also with HTML::CalendarMonthSimple
#$VERSION   = '1.002';  #cs# debug, relative month/year, highlight today
#$VERSION   = '1.001';  #as# delayed load
#$VERSION   = '1.000';  #as# initial release

$libsLoaded = 0;
$libsError  = 0;
%defaults    = ();
# =========================
sub initPlugin
{
    ( $topic, $web, $user, $installWeb ) = @_;

    my $webColor = &TWiki::Func::getPreferencesValue("WEBBGCOLOR", $web) ||
		    'wheat' ;

    # reasonable defaults to produce a small calendar
    %defaults = (
	# normal HTML::CalendarMonthSimple options
	border				=> 1,
	width				=> 0,
	showdatenumbers			=> 0,
	showweekdayheaders		=> 0,
	weekdayheadersbig		=> undef, # the default is ok
	cellalignment			=> 'center',
	vcellalignment			=> 'center',
	header				=> undef, # the default is ok
	nowrap				=> undef, # the default is ok
	sharpborders			=> 1,
	cellheight			=> undef, # the default is ok
	cellclass			=> undef, # the default is ok
	weekdaycellclass		=> undef, # the default is ok
	weekendcellclass		=> undef, # the default is ok
	todaycellclass			=> undef, # the default is ok
	headerclass			=> undef, # the default is ok
	# colors
	bgcolor				=> 'white',
	weekdaycolor			=> undef, # the default is ok
	weekendcolor			=> 'lightgrey',
	todaycolor			=> $webColor,
	bordercolor			=> 'black',
	weekdaybordercolor		=> undef, # the default is ok
	weekendbordercolor		=> undef, # the default is ok
	todaybordercolor		=> undef, # the default is ok
	contentcolor			=> undef, # the default is ok
	weekdaycontentcolor		=> undef, # the default is ok
	weekendcontentcolor		=> undef, # the default is ok
	todaycontentcolor		=> undef, # the default is ok
	headercolor			=> $webColor,
	headercontentcolor		=> undef, # the default is ok
	weekdayheadercolor		=> undef, # the default is ok
	weekdayheadercontentcolor	=> undef, # the default is ok
	weekendheadercolor		=> undef, # the default is ok
	weekendheadercontentcolor	=> undef, # the default is ok
	# other options not belonging to HTML::CalendarMonthSimple
	topic			=> $topic,
	web			=> $web,
	format			=> "<A HREF=\"%SCRIPTURLPATH%/view%SCRIPTSUFFIX%/\$web/\$topic\"><FONT size=\"+2\">\$old</FONT><IMG ALT=\"\$description\" SRC=\"%PUBURLPATH%/$installWeb/CalendarPlugin/exclam.gif\"/></A>"
	);

    # now get defaults from CalendarPlugin topic
    my $v;
    foreach $option (keys %defaults) {
	# read defaults from CalendarPlugin topic
	$v = &TWiki::Func::getPreferencesValue("CALENDARPLUGIN_\U$option\E") || undef;
	$defaults{$option} = $v if defined($v);
    }

    # return true if initialization OK
    return 1;
}

# =========================
sub commonTagsHandler
{
    $_[0] =~ s/%CALENDAR{(.*?)}%/&handleCalendar($1)/geo;
    $_[0] =~ s/%CALENDAR%/&handleCalendar("")/geo;
}

# =========================
sub fetchDays
{
  my $pattern = "^\\s*\\*\\s+$_[0]\\s+-\\s+(.*)\$";
  my @lines = split /\n/, ${ $_[1] };

  my @res = (map  { join '|', m/$pattern/ }
	     grep { m/$pattern/ }
	     @lines);

  # Remove the lines we handled, so that when several patterns
  # match a line, only the first pattern is really honored.
  ${$_[1]} = join ("\n",
                   (grep { !m/$pattern/ } @lines));

  return @res;
}

# =========================
sub handleCalendar
{
    my( $attributes ) = @_;

    use Date::Calc qw( Date_to_Days Days_in_Month );

    # lazy load of needed libraries
    if (   $libsError  ) { return "";  }
    if ( ! $libsLoaded ) {
	eval 'require HTML::CalendarMonthSimple';
	if ( defined( $HTML::CalendarMonthSimple::VERSION ) ) {
	    $libsLoaded = 1;
	} else	{
	    $libsError = 1;
	    return "";
	}
    }

    # read options from the %CALENDAR% tag
    my %options = %defaults;
    my $v;
    foreach $option (keys %options) {
	$v = &TWiki::Func::extractNameValuePair($attributes,$option) || undef;
	$options{$option} = $v if defined($v);
    }

    # read fixed months/years
    my $m = scalar &TWiki::Func::extractNameValuePair( $attributes, "month" );
    my $y = scalar &TWiki::Func::extractNameValuePair( $attributes, "year" );

    # handle relative dates, too  #cs#
    my $currentDay   = (localtime)[3];
    my $currentMonth = (localtime)[4] + 1;
    my $currentYear  = (localtime)[5] + 1900;
    $y = 0 if $y eq "";  # to avoid warnings in +=
    $y += $currentYear if $y =~ /^[-+]|^0?$/;  # must come before $m !
    if ( $m =~ /^[-+]|^0?$/ ) {
      $m = 0 if $m eq "";  # to avoid warnings in +=
      $m += $currentMonth;
      ($m += 12, --$y) while $m <= 0;
      ($m -= 12, ++$y) while $m > 12;
    }

    my $cal = new HTML::CalendarMonthSimple(month => $m, year => $y);

    my $p = "";
    while (($k,$v) = each %options) {
	$p = "HTML::CalendarMonthSimple::$k";
	$cal->$k($v) if defined(&$p);
    }

    # header color
    my $webColor = &TWiki::Func::getPreferencesValue("WEBBGCOLOR", $options{web}) ||
		    'wheat' ;
    # Highlight today
    $options{todaycolor}  = $webColor;
    $options{headercolor} = $webColor;

    # set the initial day values if normal date numbers are not shown
    if ($cal->showdatenumbers == 0) {
	for ($i=1; $i<33 ; $i++) {
	    $cal->setcontent($i,"$i");
	}
    }

    # parse events
    my @days = ();
    my ($descr, $dd, $mm, $yy, $text) =
       ('',     '',  '',  '',  ''   );
    my %months = (  Jan=>1, Feb=>2, Mar=>3, Apr=>4,  May=>5,  Jun=>6,
		    Jul=>7, Aug=>8, Sep=>9, Oct=>10, Nov=>11, Dec=>12);
    my $days_rx = '[0-9]?[0-9]';
    my $months_rx = join ('|', keys %months);
    my $years_rx = '[12][0-9][0-9][0-9]';
    my $date_rx = "($days_rx)\\s+($months_rx)";
    my $full_date_rx = "$date_rx\\s+($years_rx)";
    if ( defined $options{topic} ) {
        #FIXME: untaint web/topic name?
	#TODO: get the full content to parse it..
	$text = &TWiki::Func::readTopic($options{web}, $options{topic});
	# first collect all date intervals with year
	@days =	fetchDays("$full_date_rx\\s+-\\s+$full_date_rx", \$text);
	foreach $d (@days) {
	    my ($dd1, $mm1, $yy1, $dd2, $mm2, $yy2, $descr) = split( /\|/, $d);
	    my $date1 = Date_to_Days ($yy1, $months{$mm1}, $dd1);
	    my $date2 = Date_to_Days ($yy2, $months{$mm2}, $dd2);
	    for my $d (1 .. Days_in_Month ($y, $m)) {
	      my $date = Date_to_Days ($y, $m, $d);
	      if ($date1 <= $date && $date <= $date2) {
		&highlightDay( $cal, $d, $descr, %options);
	      }
	    }
	}
	# then collect all intervals without year
	@days =	fetchDays("$date_rx\\s+-\\s+$date_rx", \$text);
	foreach $d (@days) {
	    my ($dd1, $mm1, $dd2, $mm2, $descr) = split( /\|/, $d);
	    my $date1 = Date_to_Days ($y, $months{$mm1}, $dd1);
	    my $date2 = Date_to_Days ($y, $months{$mm2}, $dd2);
	    for my $d (1 .. Days_in_Month ($y, $m)) {
	      my $date = Date_to_Days ($y, $m, $d);
	      if ($date1 <= $date && $date <= $date2) {
		&highlightDay( $cal, $d, $descr, %options);
	      }
	    }
	}
 	# first collect all dates with year
 	@days =	fetchDays("$full_date_rx", \$text);
 	foreach $d (@days) {
 	    ($dd, $mm, $yy, $descr) = split( /\|/, $d);
 	    if ($yy == $y && $months{$mm} == $m ) {
 		&highlightDay( $cal, $dd, $descr, %options);
 	    }
 	}
 	# then collect all dates without year
 	@days =	fetchDays("$date_rx", \$text);
 	foreach $d (@days) {
 	    ($dd, $mm, $descr) = split( /\|/, $d);
 	    if ($months{$mm} == $m ) {
 		&highlightDay( $cal, $dd, $descr, %options );
 	    }
 	}
    }
    return $cal->as_HTML;
}

sub highlightDay
{
	my ($c, $day, $description, %options) = @_;
	my $old = $c->getcontent($day);
	my $format = $options{format};
	$format =~ s/\$description/$description/g ;
	$format =~ s/\$web/$options{web}/g ;
	$format =~ s/\$topic/$options{topic}/g ;
	$format =~ s/\$day/$day/g ;
	$format =~ s/\$old/$old/g ;
	$format =~ s/\$installWeb/$installWeb/g ;
	$format =~ s/\$n/\n/g ;

	$c->setcontent($day,$format);
}


1;
