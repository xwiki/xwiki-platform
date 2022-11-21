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

<xwikidoc version="1.5" reference="PanelsCode.ApplicationsPanelConfigurationSheet" locale="">
  <web>PanelsCode</web>
  <name>ApplicationsPanelConfigurationSheet</name>
  <language/>
  <defaultLanguage/>
  <translation>0</translation>
  <creator>xwiki:XWiki.Admin</creator>
  <parent>Panels.ApplicationsPanel</parent>
  <author>xwiki:XWiki.Admin</author>
  <contentAuthor>xwiki:XWiki.Admin</contentAuthor>
  <version>1.1</version>
  <title>#if($doc.documentReference.name == 'ApplicationsPanelConfigurationSheet')ApplicationsPanelConfiguration Sheet#{else}$services.display.title($doc, {'displayerHint': 'default'})#end</title>
  <comment/>
  <minorEdit>false</minorEdit>
  <syntaxId>xwiki/2.1</syntaxId>
  <hidden>true</hidden>
  <content>{{velocity}}
##########################
## JAVASCRIPT/CSS
##########################
#set($discard = $xwiki.jsx.use('PanelsCode.ApplicationsPanelConfigurationSheet'))
#set($discard = $xwiki.ssx.use('PanelsCode.ApplicationsPanelConfigurationSheet'))
##########################
## First, we split the applications in 2 categories (white listed/black listed)
##########################
#set($displayedApps = [])
#set($blacklistedApps = [])
#if($doc.fullName == 'XWiki.XWikiPreferences')
  #set($configDoc = $xwiki.getDocument($services.model.createDocumentReference('', 'PanelsCode', 'ApplicationsPanelConfiguration')))
#else
  #set($configDoc = $doc)
#end
#foreach($uix in $services.uix.getExtensions('org.xwiki.platform.panels.Applications', {'sortByParameter' : 'label'}))
  #if("$!configDoc.getObject('PanelsCode.ApplicationsPanelBlackListClass', 'applicationId', $uix.id)" != '')
    #set($discard = $blacklistedApps.add({'uix': $uix}))
  #else
    #set ($app = {'uix': $uix})
    #set ($orderObj = $configDoc.getObject('PanelsCode.ApplicationsPanelOrderClass', 'applicationId', $uix.id))
    #if ($orderObj)
      #set ($discard = $app.put('order', $orderObj.getValue('order')))
    #else
      ## if order is not set, set MAX_INTEGER
      #set ($discard = $app.put('order', 2147483647))
    #end
    #set ($discard = $displayedApps.add($app))
  #end
#end
## Sort the displayedApp
#set ($displayedApps = $collectiontool.sort($displayedApps, 'order'))
##########################
## Macro to display an application panel
##########################
#macro(showAppPanel $id $title $class $apps)

  {{html}}
    &lt;div class="col-xs-12 col-md-6"&gt;
      &lt;div class="panel-width-Small panel $!class appsPanel" id="$id"&gt;
        &lt;div class="panel-heading"&gt;
          &lt;h2&gt;$title&lt;/h2&gt;
        &lt;/div&gt;
        &lt;div class="panel-body"&gt;
          &lt;ul class="nav nav-pills applicationsPanel"&gt;
            #foreach($app in $apps)
              #set($params = $app.uix.getParameters())
              #set($normalizedIcon = $stringtool.substringBefore($!params.icon, ' '))
              #if("$!normalizedIcon" != '' &amp;&amp; "$!params.label" != '' &amp;&amp; "$!params.target" != '' &amp;&amp; $xwiki.hasAccessLevel('view', $xcontext.user, $params.target))
                #if ($normalizedIcon.startsWith('icon:'))
                  #set($icon = $services.icon.renderHTML($normalizedIcon.substring(5)))
                #else
                  #set($icon = $services.rendering.render($services.rendering.parse("image:${normalizedIcon}", 'xwiki/2.1'), 'xhtml/1.0'))
                #end
                &lt;li class="draggableApp" id="$escapetool.xml($app.uix.id)"&gt;
                  &lt;a&gt;&lt;span class="application-img"&gt;$icon &lt;/span&gt; &lt;span class="application-label"&gt;$escapetool.xml($params.label)&lt;/a&gt;
                &lt;/li&gt;
              #end
            #end
          &lt;/ul&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
  {{/html}}
#end
##########################
## Display the information message
##########################
(% class="noitems" %)
{{translation key="platform.panels.applications.helper" /}}
##########################
## Display the 2 panels
##########################
(% class="row appLists" %)
(((
  #showAppPanel('displayedPanels', $services.localization.render('platform.panels.applications.displayedapps'), 'panel-primary', $displayedApps)
  #showAppPanel('blacklistedPanels', $services.localization.render('platform.panels.applications.blacklistedapps'), 'panel-info', $blacklistedApps)
)))
##########################
## Display the buttons
##########################
{{html}}
&lt;button class="btn btn-primary" id="bt-save"&gt;$services.localization.render('platform.panels.applications.save')&lt;/button&gt; &lt;button class="btn btn-default" id="bt-revert"&gt;$services.localization.render('platform.panels.applications.revert')&lt;/button&gt;
{{/html}}
{{/velocity}}
</content>
  <object>
    <name>PanelsCode.ApplicationsPanelConfigurationSheet</name>
    <number>0</number>
    <className>XWiki.JavaScriptExtension</className>
    <guid>3bd008ad-ec26-4593-899c-729733fd5e37</guid>
    <class>
      <name>XWiki.JavaScriptExtension</name>
      <customClass/>
      <customMapping/>
      <defaultViewSheet/>
      <defaultEditSheet/>
      <defaultWeb/>
      <nameField/>
      <validationScript/>
      <cache>
        <cache>0</cache>
        <defaultValue>long</defaultValue>
        <disabled>0</disabled>
        <displayType>select</displayType>
        <freeText>forbidden</freeText>
        <largeStorage>0</largeStorage>
        <multiSelect>0</multiSelect>
        <name>cache</name>
        <number>5</number>
        <prettyName>Caching policy</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator> </separator>
        <separators>|, </separators>
        <size>1</size>
        <unmodifiable>0</unmodifiable>
        <values>long|short|default|forbid</values>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </cache>
      <code>
        <contenttype>PureText</contenttype>
        <disabled>0</disabled>
        <editor>PureText</editor>
        <name>code</name>
        <number>2</number>
        <prettyName>Code</prettyName>
        <rows>20</rows>
        <size>50</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.TextAreaClass</classType>
      </code>
      <name>
        <disabled>0</disabled>
        <name>name</name>
        <number>1</number>
        <prettyName>Name</prettyName>
        <size>30</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </name>
      <parse>
        <disabled>0</disabled>
        <displayFormType>select</displayFormType>
        <displayType>yesno</displayType>
        <name>parse</name>
        <number>4</number>
        <prettyName>Parse content</prettyName>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
      </parse>
      <use>
        <cache>0</cache>
        <disabled>0</disabled>
        <displayType>select</displayType>
        <freeText>forbidden</freeText>
        <largeStorage>0</largeStorage>
        <multiSelect>0</multiSelect>
        <name>use</name>
        <number>3</number>
        <prettyName>Use this extension</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator> </separator>
        <separators>|, </separators>
        <size>1</size>
        <unmodifiable>0</unmodifiable>
        <values>currentPage|onDemand|always</values>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </use>
    </class>
    <property>
      <cache>long</cache>
    </property>
    <property>
      <code>define('xwiki-panels-applications-config-messages', {
  prefix: 'platform.panels.applications.ajax.',
  keys: ['success', 'csrf', 'noright', 'error']
});

/************************************
 * Script that enable the drag &amp; drop of applications
 ************************************/
require([
  'jquery',
  'xwiki-meta',
  'xwiki-l10n!xwiki-panels-applications-config-messages',
  'jquery-ui',
  'jquery-ui-touch-punch'
], function($, xm, l10n) {

  /**
   * Save the black list and the order configuration
   */
  var save = function() {
    // Generate the array that contains all the applications to have in the blacklist
    var blacklistElems = $('#blacklistedPanels li.draggableApp');
    var blacklist = [];
    for (var i = 0; i &lt; blacklistElems.length; ++i) {
      blacklist.push(blacklistElems[i].id);
    }
    // Generate the JSON array that contains the order of the displayed applications
    var displaylistElems = $('#displayedPanels li.draggableApp');
    var displaylistOrder = [];
    for (var i = 0; i &lt;displaylistElems.length; ++i) {
      displaylistOrder.push(displaylistElems[i].id);
    }
    $.post(new XWiki.Document('ApplicationsPanelConfigurationAjax', 'PanelsCode').getURL('get'), {
      outputSyntax: 'plain',
      form_token: xm.form_token,
      blacklist: JSON.stringify(blacklist),
      orderlist: JSON.stringify(displaylistOrder)
    }).then(data =&gt; {
      if (data === 'SUCCESS') {
        new XWiki.widgets.Notification(l10n['success'], 'done');
      } else if (data === 'BAD CSRF') {
        new XWiki.widgets.Notification(l10n['csrf'], 'error');
      } else if (data === 'NO RIGHT') {
        new XWiki.widgets.Notification(l10n['noright'], 'error');
      } else {
        new XWiki.widgets.Notification(l10n['error'], 'error');
      }
    }).catch(() =&gt; {
      new XWiki.widgets.Notification(l10n['error'], 'error');
    });
  };

  /**
   * Initialization
   */
  $(function() {
    // Define the selector for the elements on which we will use the jquery-ui sortable plugin, which allows to sort
    // inner elements with drag &amp;amp; drop.
    var sortableSelector = '.appsPanel &gt; div &gt; ul';

    // Enable the sortable plugin.
    // The 'connectWith' option allows the drag &amp; drop from a sortable list (e.g. the displayed panels) to an other
    // sortable list (e.g. the blacklisted panels).
    // Here we create 2 sortable lists, because the selector match those 2.
    $(sortableSelector).sortable({connectWith: sortableSelector});

    // Allow reverting the changes
    $('#bt-revert').on('click', function() {
      // Refresh the page
      window.location.reload();
    });

    // Redefine the save action
    $('#bt-save').on('click', save);
  });
});
</code>
    </property>
    <property>
      <name>Drag &amp; Drop</name>
    </property>
    <property>
      <parse>0</parse>
    </property>
    <property>
      <use>onDemand</use>
    </property>
  </object>
  <object>
    <name>PanelsCode.ApplicationsPanelConfigurationSheet</name>
    <number>0</number>
    <className>XWiki.StyleSheetExtension</className>
    <guid>e6d34b0f-3b57-4cdf-b263-ad878e949af3</guid>
    <class>
      <name>XWiki.StyleSheetExtension</name>
      <customClass/>
      <customMapping/>
      <defaultViewSheet/>
      <defaultEditSheet/>
      <defaultWeb/>
      <nameField/>
      <validationScript/>
      <cache>
        <cache>0</cache>
        <defaultValue>long</defaultValue>
        <disabled>0</disabled>
        <displayType>select</displayType>
        <freeText>forbidden</freeText>
        <largeStorage>0</largeStorage>
        <multiSelect>0</multiSelect>
        <name>cache</name>
        <number>5</number>
        <prettyName>Caching policy</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator> </separator>
        <separators>|, </separators>
        <size>1</size>
        <unmodifiable>0</unmodifiable>
        <values>long|short|default|forbid</values>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </cache>
      <code>
        <contenttype>PureText</contenttype>
        <disabled>0</disabled>
        <editor>PureText</editor>
        <name>code</name>
        <number>2</number>
        <prettyName>Code</prettyName>
        <rows>20</rows>
        <size>50</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.TextAreaClass</classType>
      </code>
      <contentType>
        <cache>0</cache>
        <disabled>0</disabled>
        <displayType>select</displayType>
        <freeText>forbidden</freeText>
        <largeStorage>0</largeStorage>
        <multiSelect>0</multiSelect>
        <name>contentType</name>
        <number>6</number>
        <prettyName>Content Type</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator> </separator>
        <separators>|, </separators>
        <size>1</size>
        <unmodifiable>0</unmodifiable>
        <values>CSS|LESS</values>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </contentType>
      <name>
        <disabled>0</disabled>
        <name>name</name>
        <number>1</number>
        <prettyName>Name</prettyName>
        <size>30</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </name>
      <parse>
        <disabled>0</disabled>
        <displayFormType>select</displayFormType>
        <displayType>yesno</displayType>
        <name>parse</name>
        <number>4</number>
        <prettyName>Parse content</prettyName>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
      </parse>
      <use>
        <cache>0</cache>
        <disabled>0</disabled>
        <displayType>select</displayType>
        <freeText>forbidden</freeText>
        <largeStorage>0</largeStorage>
        <multiSelect>0</multiSelect>
        <name>use</name>
        <number>3</number>
        <prettyName>Use this extension</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator> </separator>
        <separators>|, </separators>
        <size>1</size>
        <unmodifiable>0</unmodifiable>
        <values>currentPage|onDemand|always</values>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </use>
    </class>
    <property>
      <cache>long</cache>
    </property>
    <property>
      <code>.draggableApp {
  cursor: grab;
  width: 100px;
}

.appsPanel .applicationsPanel {
  min-height: 100px;
}</code>
    </property>
    <property>
      <contentType>CSS</contentType>
    </property>
    <property>
      <name>CSS</name>
    </property>
    <property>
      <parse>0</parse>
    </property>
    <property>
      <use>onDemand</use>
    </property>
  </object>
</xwikidoc>
