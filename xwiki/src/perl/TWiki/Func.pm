#
# TWiki WikiClone (see wiki.pm for $wikiversion and other info)
#
# Copyright (C) 2000 Peter Thoeny, Peter@Thoeny.com
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
# Notes:
# - Latest version at http://twiki.org/
# - Installation instructions in $dataDir/TWiki/TWikiDocumentation.txt
# - Customize variables in TWiki.cfg when installing TWiki.
# - Upgrading TWiki is easy as long as you use Plugins.
# - Check web server error logs for errors, i.e. % tail /var/log/httpd/error_log
#
# This is the module with official funcions. Plugins should
# ONLY use functions published in this module. If you use
# other functions you might impose a security hole and you
# will likely need to change your plugin when you upgrade
# TWiki.

package TWiki::Func;

use strict;

# =========================
# get session value (from session plugin)
# =========================
sub getSessionValue
{
#   my( $key ) = @_;
    return ""; #&TWiki::getSessionValue( @_ );
}

# =========================
# get a preferences value
# =========================
sub getPreferencesValue
{
#   my( $theKey, $theWeb ) = @_;
    # $theKey is "MYPLUGIN_COLOR" to get the "Set COLOR" setting in "MyPlugin" topic
    # $theWeb is optional (does not apply to settings of plugin topics)
    return ""; #&TWiki::Prefs::getPreferencesValue( @_ );
}

# =========================
# get a preferences flag value
# =========================
sub getPreferencesFlag
{
#   my( $theKey ) = @_;
    # $theKey is "MYPLUGIN_SHOWHELP" to get the "Set SHOWHELP" setting in "MyPlugin" topic
    return ""; #&TWiki::Prefs::getPreferencesFlag( @_ );
}

# =========================
# extract the value from a name="value" attribute pair
# =========================
sub extractNameValuePair
{
    my( $str, $name ) = @_;

    if( $name ) {
        # format is: %VAR{ ... name = "value" }%
        if( ( $str =~ /(^|[^\S])$name\s*=\s*\"([^\"]*)\"/ ) && ( $2 ) ) {
            return $2;
        }
    } else {
        # test if format: { "value" ... }
        if( ( $str =~ /(^|\=\s*\"[^\"]*\")\s*\"([^\"]*)\"/ ) && ( $2 ) ) {
            # is: { "value" ... }
            return $2;

        } elsif( ( $str =~ /^\s*\w+\s*=\s*\"([^\"]*)/ ) && ( $1 ) ) { # value in double quotes (")
            # is not a standalone var, but: %VAR{ name = "value" }%
            return "";

        } else {
            # format is: %VAR{ value }%
            return $1;
        }
    }
    return "";
}

# =========================
# log Warning that may require admin intervention to data/warning.txt
# =========================
sub writeWarning
{
#   my( $theText ) = @_;
#    return &TWiki::writeWarning( @_ );
 print "WARNING: " . @_;
 return;
}

# =========================
# log debug message to data/debug.txt
# =========================
sub writeDebug
{
#   my( $theText ) = @_;
#    return &TWiki::writeDebug( @_ );
 print "DEBUG: " . @_;
 return;
}

# =========================
# get data directory (topic file root)
# =========================
sub getDataDir
{
    return ""; #&TWiki::getDataDir();
}

# =========================
# get pub directory (file attachment root)
# =========================
sub getPubDir
{
    return ""; #&TWiki::getPubDir();
}

# =========================
# get pub URL path
# =========================
sub getPubUrlPath
{
    return ""; #&TWiki::getPubUrlPath();
}

# =========================
# get script URL path
# =========================
sub getScriptUrlPath
{
    return ""; #$TWiki::scriptUrlPath;
}

# =========================
# get default URL host
# =========================
sub getDefaultUrlHost
{
    return ""; #$TWiki::defaultUrlHost;
}

# =========================
# get URL host
# would this be better as $cgiQuery->url()???
# =========================
sub getUrlHost
{
    return ""; #$TWiki::urlHost;
}

# =========================
# compose fully qualified URL
# =========================
sub getScriptUrl
{
#   my( $web, $topic, $script ) = @_;
    return ""; #&TWiki::getScriptUrl( @_ ); 
}

# =========================
# compose fully qualified view URL
# =========================
sub getViewUrl
{
#   my( $theWeb, $theTopic ) = @_;
    return ""; #&TWiki::getViewUrl( @_ );
}

# =========================
# compose fully qualified "oops" dialog URL
# =========================
sub getOopsUrl
{
#   my( $theWeb, $theTopic, $theTemplate, @theParams ) = @_;
    # up to 4 parameters in @theParams
    return ""; #&TWiki::getOopsUrl( @_ );
}

# =========================
# get wikiToolName
# =========================
sub getWikiToolName
{
    return ""; #$TWiki::wikiToolName;
}

# =========================
# get mainWebname
# =========================
sub getMainWebname
{
    return "Main"; #$TWiki::mainWebname;
}

# =========================
# get twikiWebname
# =========================
sub getTwikiWebname
{
    return "TWiki"; #$TWiki::twikiWebname;
}

# ==========================
# get ScriptName
# ==========================
sub getScriptName
{
    return ""; #&TWiki::getScriptName();
}

# =========================
# expand all common %VARIABLES%
# =========================
sub expandCommonVariables
{
#   my( $theText, $theTopic, $theWeb ) = @_;
    return @_; #&TWiki::handleCommonTags( @_ );
}

# =========================
# render text in Wiki syntax
# =========================
sub renderText
{
#   my( $theText, $theWeb ) = @_;
    return @_; #&TWiki::getRenderedVersion( @_ );
}

# =========================
# do internal link
# =========================
sub internalLink
{
    my( $thePreamble, $theWeb, $theTopic, $theLinkText, $theAnchor, $doLink ) = @_;
    return $theLinkText; #&TWiki::internalLink( @_ );
}

# =========================
# handle include file
# =========================
sub handleIncludeFile
{
    my( $theAttributes, $theTopic, $theWeb, @theProcessedTopics ) = @_;
    return $theWeb . "." . $theTopic; #&TWiki::handleIncludeFile( @_ );
}

# =========================
# get list of all public webs
# =========================
sub getPublicWebList
{
    return ""; #&TWiki::getPublicWebList();
}

# =========================
# test if any permissions are set on this web
# =========================
#sub permissionsSet
#{
#   my( $web ) = @_;
#   &TWiki::Access::permissionsSet( @_ );
#}

# =========================
# check access permissions for this topic
# =========================
sub checkAccessPermission
{
#   my( $theAccessType, $theUserName, $theTopicText, $theTopicName, $theWebName ) = @_;
    return 1; # &TWiki::Access::checkAccessPermission( @_ );
}

# =========================
# get list of all topics in a web
# =========================
sub getTopicList
{
#   my( $theWeb ) = @_;
    return ""; #&TWiki::Store::getTopicNames ( @_ );
}

# =========================
# test if web exists
# =========================
sub webExists
{
#   my( $theWeb ) = @_;
    return 1; #&TWiki::Store::webExists( @_ );
}

# =========================
# test if topic exists
# =========================
sub topicExists
{
#   my( $theWeb, $theTopic ) = @_;
    return 1; #&TWiki::Store::topicExists( @_ );
}

# =========================
# get total number of revisions (this topic)
# =========================
sub getNumberOfRevisions
{
    return 1; #$TWiki::numberOfRevisions;
}

# =========================
# get revision number
# =========================
sub getRevisionNumber
{
#   my( $web, $topic ) = @_;
    return "1.1"; #&TWiki::Store::getRevisionNumber( @_ );
}

# =========================
# get revision info from meta
# =========================
sub getRevisionInfoFromMeta
{
#   my( $web, $topic, $meta, $format );
    return "1.1"; #&TWiki::Store::getRevisionInfoFromMeta( @_ );
}

# =========================u
# get form definition
# =========================
sub getFormDefinition
{
#   my( $text ) = @_;
    return ""; #&TWiki::Form::getFormDefinition( @_ );
}

# =========================
# cache a topic content
# =========================
my %topiccache = ();
sub cacheTopic
{
 my ( $theWebName, $theTopic, $content) = @_; 
 $topiccache{$theWebName . "." . $theTopic} = $content;
}

sub unCacheTopic
{
 my ( $theWebName, $theTopic) = @_; 
 $topiccache{$theWebName . "." . $theTopic} = "";
}

# =========================
# read a topic "as is" (with embedded meta data)
# =========================
sub readTopic
{
    my( $theWebName, $theTopic ) = @_;
    my $content = $topiccache{$theWebName . "." . $theTopic};
    if ($content eq "") {
	# Read the topic in Java
	return "";
    } else {
	return $content;
    }
    #&TWiki::Store::readTopic( @_ );
}

# =========================
# read a template file
# =========================
sub readTemplate
{
#   my( $theName, $theSkin ) = @_;
    return ""; #&TWiki::Store::readTemplate( @_ );
}

# =========================
# read text file, low level
# =========================
sub readFile
{
#   my( $theFileName ) = @_;
    return ""; #&TWiki::Store::readFile( @_ );
}

# =========================
# save text file, low level
# =========================
sub saveFile
{
#   my( $theFileName, $theText ) = @_;
    return 0; #&TWiki::Store::saveFile( @_ );
}

# =========================
# get defaultUserName
# =========================
sub getDefaultUserName
{
    return ""; #$TWiki::defaultUserName;
}

# =========================
# get wikiUserName
# =========================
sub getWikiUserName
{
    return ""; #$TWiki::wikiUserName;
}

# =========================
# translate wikiUserName to userName
# =========================
sub wikiToUserName
{
#   my $wiki = @_;
    return ""; #&TWiki::wikiToUserName( @_ );
}

# =========================
# translate wikiUserName to userName
# =========================
sub userToWikiName
{
#   my $user = @_;
    return ""; #&TWiki::userToWikiName( @_ );
}

# =========================
# Write HTML header
# =========================
sub writeHeader
{
#   my( $theQuery ) = @_;
    return ""; #&TWiki::writeHeader( @_ );
}

# =========================
# get CGI query object
# would this be better restricted to getCgiQueryParam???
# =========================
sub getCgiQuery
{
    return ""; #&TWiki::getCgiQuery();
}

# =========================u
# redirect to URL
# =========================
sub redirectCgiQuery
{
#   my( $theQuery, $theUrl ) = @_;
    return ""; #&TWiki::redirect( @_ );
}

# =========================u
# search web
# =========================
sub searchWeb
{
#   my ( $doInline, $theWebName, $theSearchVal, $theScope, $theOrder,
#         $theRegex, $theLimit, $revSort, $caseSensitive, $noSummary,
#         $noSearch, $noHeader, $noTotal, $doBookView, $doRenameView,
#         $doShowLock, $noEmpty, $template, $meta, $external, @junk ) = @_;
    return ""; #&TWiki::Search::searchWeb( @_ );
}

# =========================
# format the time
# =========================
sub formatGmTime
{
#   my $epSecs = @_;
    return @_; #&TWiki::formatGmTime( @_ );
}


1;

# EOF
