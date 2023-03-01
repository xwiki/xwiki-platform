<?xml version="1.1" encoding="UTF-8"?>

<!--
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->

<xwikidoc version="1.3" reference="XWiki.Notifications.Code.NotificationRSSService" locale="">
  <web>XWiki.Notifications.Code</web>
  <name>NotificationRSSService</name>
  <language/>
  <defaultLanguage/>
  <translation>0</translation>
  <creator>xwiki:XWiki.Admin</creator>
  <parent>xwiki:XWiki.Notifications.Code.WebHome</parent>
  <author>xwiki:XWiki.Admin</author>
  <contentAuthor>xwiki:XWiki.Admin</contentAuthor>
  <version>1.1</version>
  <title/>
  <comment/>
  <minorEdit>false</minorEdit>
  <syntaxId>xwiki/2.1</syntaxId>
  <hidden>true</hidden>
  <content>{{velocity}}
#set ($feedContent = $services.notification.notifiers.getFeed(20))
#if ($xcontext.action == 'get' &amp;&amp; "$request.outputSyntax" == 'plain')
  #set ($characterEncoding = 'utf-8')
  #set ($discard = $response.setContentType('application/xml'))
  ## Make sure the Character Encoding response header matches the character encoding used to write the response and
  ## compute its length.
  #set ($discard = $response.setCharacterEncoding($characterEncoding))
  ## We write the output directly to the response to avoid the execution of the Rendering Transformations.
  #set ($discard = $response.writer.print($feedContent))
  ## The content length is measured in bytes and one character can use more than one byte.
  #set ($discard = $response.setContentLength($feedContent.getBytes($characterEncoding).size()))
  ## Make sure the entire content is send back to the client.
  #set ($discard = $response.flushBuffer())
  ## Make sure XWiki doesn't write any more content to the response.
  #set ($discard = $xcontext.setFinished(true))
#else
  {{code language="xml" source="script:feedContent" /}}
#end
{{/velocity}}
</content>
</xwikidoc>
