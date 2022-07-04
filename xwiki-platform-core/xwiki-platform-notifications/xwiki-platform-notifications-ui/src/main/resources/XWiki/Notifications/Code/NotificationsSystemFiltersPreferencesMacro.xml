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

<xwikidoc version="1.5" reference="XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro" locale="">
  <web>XWiki.Notifications.Code</web>
  <name>NotificationsSystemFiltersPreferencesMacro</name>
  <language/>
  <defaultLanguage/>
  <translation>0</translation>
  <creator>xwiki:XWiki.Admin</creator>
  <parent>WebHome</parent>
  <author>xwiki:XWiki.Admin</author>
  <contentAuthor>xwiki:XWiki.Admin</contentAuthor>
  <version>1.1</version>
  <title>NotificationsSystemFiltersPreferencesMacro</title>
  <comment/>
  <minorEdit>false</minorEdit>
  <syntaxId>xwiki/2.1</syntaxId>
  <hidden>true</hidden>
  <content/>
  <object>
    <name>XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro</name>
    <number>0</number>
    <className>XWiki.JavaScriptExtension</className>
    <guid>7e7d151b-e4dd-498b-8e50-f213104fa256</guid>
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
      <code>'use strict';
/**
 * Start the real script.
 */
require(['jquery', 'xwiki-bootstrap-switch', 'xwiki-events-bridge'], function ($) {

  /**
   * Page initialization
   */
  $(function() {
    var initBootstrapSwitches = function () {
      $('#notificationSystemFilterPreferencesLiveTable .notificationFilterPreferenceCheckbox, #notificationSystemFilterPreferencesLiveTable .toggleableFilterPreferenceCheckbox').bootstrapSwitch({
        size: 'mini',
        labelText: '$escapetool.javascript($services.icon.renderHTML("bell"))'
      });
    };
    $(document).on('xwiki:livetable:displayComplete', function (event, data) {
      // Enable bootstrap switches
      initBootstrapSwitches();
    });
    initBootstrapSwitches();

  });
});
</code>
    </property>
    <property>
      <name>JS</name>
    </property>
    <property>
      <parse>1</parse>
    </property>
    <property>
      <use>currentPage</use>
    </property>
  </object>
  <object>
    <name>XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro</name>
    <number>2</number>
    <className>XWiki.JavaScriptExtension</className>
    <guid>f3d87be8-97d5-490f-b519-952b54fc3e69</guid>
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
      <code>define('SystemToggleableFilterPreference', ['jquery', 'xwiki-bootstrap-switch'], function($) {
  /**
  * Construct the Toggleable Filter Preference object.
  */
  return function(domElement, docURL, row) {
    var self               = this;
    self.domElement        = $(domElement);
    self.docURL           = docURL;
    self.row               = row;
    /**
     * Initialization
     */
    self.init = function () {
      // On change
      self.domElement.off('switchChange.bootstrapSwitch.systemFilter')
        .on('switchChange.bootstrapSwitch.systemFilter', function(event, state) {
          setTimeout(function() { self.save(state); }, 1);
        });
    };

    /**
     * Save the filter preference
     */
    self.save = function(state) {
      var notification = new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.settings.saving'))", 'inprogress');
      var filterName = self.domElement.attr('data-filterName');
      var objectNumber = self.domElement.attr('data-objectNumber');
      if (objectNumber == '') {
        // If the object does not exist yet, create it (make this change a minor edit so it's not displayed, by default, in notifications)
        var restURL = self.docURL + '/objects?media=json&amp;minorRevision=true';
        var params = {
          'className': 'XWiki.Notifications.Code.ToggleableFilterPreferenceClass',
          'property#filterName': filterName,
          'property#isEnabled': state ? '1' : '0'
        };
        $.post(restURL, params).then(data =&gt; {
          self.domElement.attr('data-objectNumber', data.number);
          notification.hide();
          new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.setEnabled.done'))", 'done');
        }).catch(() =&gt; {
          notification.hide();
          new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.setEnabled.error'))", 'error');
        });
      } else if (objectNumber != '') {
        // If the object already exist, just update its value (make this change a minor edit so it's not displayed, by default, in notifications)
        var restURL = self.docURL + '/objects/XWiki.Notifications.Code.ToggleableFilterPreferenceClass/' + objectNumber
          + '/properties/isEnabled?media=json&amp;minorRevision=true';
        $.ajax(restURL, {
          method: 'PUT',
          contentType: 'text/plain',
          data: state ? '1' : '0'
        }).then(data =&gt; {
          notification.hide();
          new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.setEnabled.done'))", 'done');
          $(document).trigger('xwiki:livetable:notifications-filters-system:toggle', {'row': self.row, 'state': state});
        }).catch(() =&gt; {
          notification.hide();
          new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.setEnabled.error'))", 'error');
        });
      }
    };

    // Call init
    self.init();
  };
});
</code>
    </property>
    <property>
      <name>System Toggleable Filter Preference</name>
    </property>
    <property>
      <parse>1</parse>
    </property>
    <property>
      <use>currentPage</use>
    </property>
  </object>
  <object>
    <name>XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro</name>
    <number>3</number>
    <className>XWiki.JavaScriptExtension</className>
    <guid>2ce1c5a8-4fdb-479f-85f7-7cbfe9b7d646</guid>
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
      <code>require([
  'jquery',
  'SystemToggleableFilterPreference',
  'xwiki-meta',
  'xwiki-bootstrap-switch',
  'xwiki-events-bridge'
], function ($, SystemToggleableFilterPreference, xm) {
  $(document).on('xwiki:livetable:notifications-filters-system:toggle', function(event, data) {
    var changedRow = $(data.row.isEnabled);
    if (changedRow.length &gt; 0) {
      if (data.state) {
        changedRow.attr("checked", "checked");
      } else {
        changedRow.removeAttr("checked");
      }
      data.row.isEnabled = changedRow.get(0).outerHTML;
    }
  });

  // Globals
  var serviceReference = XWiki.Model.resolve('XWiki.Notifications.Code.NotificationPreferenceService',
    XWiki.EntityType.DOCUMENT);
  var serviceURL = new XWiki.Document(serviceReference).getURL('get', 'outputSyntax=plain');
  var target = $('.systemFilterPreferences').attr('data-target');

  var docURL = $('.systemFilterPreferences').attr('data-doc-url');
  var postParams = {
    action: 'setFilterPreferenceEnabled',
    csrf: xm.form_token,
    target: target
  };
  if (target == 'user') {
    var targetUser = $('.systemFilterPreferences').attr('data-user');
    postParams['user'] = targetUser;
  };

  var enhanceRow = function(row, data) {
    // Enable / Disable action for classic filter preferences
    $(row).find('.notificationFilterPreferenceCheckbox').off('switchChange.bootstrapSwitch.systemFilter')
    .on('switchChange.bootstrapSwitch.systemFilter', function(event, state) {
      var notif = new XWiki.widgets.Notification($jsontool.serialize($services.localization.render(
        'notifications.filters.preferences.setEnabled.inProgress')), 'inprogress');
      var params = {
        filterPreferenceId: data.filterPreferenceId,
        enabled: state
      };
      $.post(serviceURL, $.extend({}, params, postParams)).then(() =&gt; {
        notif.replace(new XWiki.widgets.Notification($jsontool.serialize($services.localization.render(
          'notifications.filters.preferences.setEnabled.done')), 'done'));
        $(document).trigger('xwiki:livetable:notifications-filters-system:toggle', {'row': data, 'state': state});
      }).catch(() =&gt; {
        notif.replace(new XWiki.widgets.Notification($jsontool.serialize($services.localization.render(
          'notifications.filters.preferences.setEnabled.error')), 'error'));
      });
    });
    // Enable / Disable action for toggleable filter preferences
    $(row).find('.toggleableFilterPreferenceCheckbox').each(function() {
      new SystemToggleableFilterPreference(this, docURL, data);
    });
  };

  // Callback on livetable row printing
  $(document).on('xwiki:livetable:notificationSystemFilterPreferencesLiveTable:newrow', function(event, data) {
    enhanceRow(data.row, data.data);
  });

  // Initializer
  $(function() {
    // This script is usually loaded after the live table is displayed and so the previous callback might not be called.
    $('#notificationSystemFilterPreferencesLiveTable-display &gt; tr[data-index]').each(function() {
      var data = window.livetable_notificationSystemFilterPreferencesLiveTable.fetchedRows[$(this).data('index')];
      enhanceRow(this, data);
    });
  });
});</code>
    </property>
    <property>
      <name>Filter Preferences Livetable Callback</name>
    </property>
    <property>
      <parse>1</parse>
    </property>
    <property>
      <use>currentPage</use>
    </property>
  </object>
  <object>
    <name>XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro</name>
    <number>0</number>
    <className>XWiki.StyleSheetExtension</className>
    <guid>0dbeaf20-9549-4eb2-99b9-95dc3fef916d</guid>
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
      <code>#notificationSystemFilterPreferencesLiveTable select.pagesizeselect {
  width: auto;
}</code>
    </property>
    <property>
      <contentType>CSS</contentType>
    </property>
    <property>
      <name>NotificationFilterPreferences</name>
    </property>
    <property>
      <parse/>
    </property>
    <property>
      <use>currentPage</use>
    </property>
  </object>
  <object>
    <name>XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro</name>
    <number>0</number>
    <className>XWiki.WikiMacroClass</className>
    <guid>3ed9f676-5c82-48c0-9082-1305822ba742</guid>
    <class>
      <name>XWiki.WikiMacroClass</name>
      <customClass/>
      <customMapping/>
      <defaultViewSheet/>
      <defaultEditSheet/>
      <defaultWeb/>
      <nameField/>
      <validationScript/>
      <async_cached>
        <defaultValue>0</defaultValue>
        <disabled>0</disabled>
        <displayFormType>select</displayFormType>
        <displayType/>
        <name>async_cached</name>
        <number>13</number>
        <prettyName>Cached</prettyName>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
      </async_cached>
      <async_context>
        <cache>0</cache>
        <disabled>0</disabled>
        <displayType>select</displayType>
        <freeText>forbidden</freeText>
        <largeStorage>0</largeStorage>
        <multiSelect>1</multiSelect>
        <name>async_context</name>
        <number>14</number>
        <prettyName>Context elements</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator>, </separator>
        <separators>|, </separators>
        <size>5</size>
        <unmodifiable>0</unmodifiable>
        <values>action=Action|doc.reference=Document|icon.theme=Icon theme|locale=Language|rendering.defaultsyntax=Default syntax|rendering.restricted=Restricted|rendering.targetsyntax=Target syntax|request.base=Request base URL|request.cookies|request.parameters=Request parameters|request.url=Request URL|request.wiki=Request wiki|user=User|wiki=Wiki</values>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </async_context>
      <async_enabled>
        <defaultValue>0</defaultValue>
        <disabled>0</disabled>
        <displayFormType>select</displayFormType>
        <displayType/>
        <name>async_enabled</name>
        <number>12</number>
        <prettyName>Asynchronous rendering</prettyName>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
      </async_enabled>
      <code>
        <disabled>0</disabled>
        <editor>Text</editor>
        <name>code</name>
        <number>10</number>
        <prettyName>Macro code</prettyName>
        <rows>20</rows>
        <size>40</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.TextAreaClass</classType>
      </code>
      <contentDescription>
        <contenttype>PureText</contenttype>
        <disabled>0</disabled>
        <editor>PureText</editor>
        <name>contentDescription</name>
        <number>9</number>
        <prettyName>Content description (Not applicable for "No content" type)</prettyName>
        <rows>5</rows>
        <size>40</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.TextAreaClass</classType>
      </contentDescription>
      <contentJavaType>
        <cache>0</cache>
        <defaultValue>Unknown</defaultValue>
        <disabled>0</disabled>
        <displayType>input</displayType>
        <freeText>allowed</freeText>
        <largeStorage>1</largeStorage>
        <multiSelect>0</multiSelect>
        <name>contentJavaType</name>
        <number>8</number>
        <picker>1</picker>
        <prettyName>Macro content type</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator>|</separator>
        <separators>|</separators>
        <size>1</size>
        <unmodifiable>0</unmodifiable>
        <values>Unknown|Wiki</values>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </contentJavaType>
      <contentType>
        <cache>0</cache>
        <disabled>0</disabled>
        <displayType>select</displayType>
        <freeText>forbidden</freeText>
        <largeStorage>0</largeStorage>
        <multiSelect>0</multiSelect>
        <name>contentType</name>
        <number>7</number>
        <prettyName>Macro content availability</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator>|</separator>
        <separators>|</separators>
        <size>1</size>
        <unmodifiable>0</unmodifiable>
        <values>Optional|Mandatory|No content</values>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </contentType>
      <defaultCategories>
        <cache>0</cache>
        <disabled>0</disabled>
        <displayType>input</displayType>
        <freeText>forbidden</freeText>
        <largeStorage>0</largeStorage>
        <multiSelect>1</multiSelect>
        <name>defaultCategories</name>
        <number>4</number>
        <prettyName>Default categories</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator> </separator>
        <separators>|, </separators>
        <size>1</size>
        <unmodifiable>0</unmodifiable>
        <values/>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </defaultCategories>
      <description>
        <contenttype>PureText</contenttype>
        <disabled>0</disabled>
        <editor>PureText</editor>
        <name>description</name>
        <number>3</number>
        <prettyName>Macro description</prettyName>
        <rows>5</rows>
        <size>40</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.TextAreaClass</classType>
      </description>
      <id>
        <disabled>0</disabled>
        <name>id</name>
        <number>1</number>
        <prettyName>Macro id</prettyName>
        <size>30</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </id>
      <name>
        <disabled>0</disabled>
        <name>name</name>
        <number>2</number>
        <prettyName>Macro name</prettyName>
        <size>30</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </name>
      <priority>
        <disabled>0</disabled>
        <name>priority</name>
        <number>11</number>
        <numberType>integer</numberType>
        <prettyName>Priority</prettyName>
        <size>10</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.NumberClass</classType>
      </priority>
      <supportsInlineMode>
        <disabled>0</disabled>
        <displayFormType>select</displayFormType>
        <displayType>yesno</displayType>
        <name>supportsInlineMode</name>
        <number>5</number>
        <prettyName>Supports inline mode</prettyName>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
      </supportsInlineMode>
      <visibility>
        <cache>0</cache>
        <disabled>0</disabled>
        <displayType>select</displayType>
        <freeText>forbidden</freeText>
        <largeStorage>0</largeStorage>
        <multiSelect>0</multiSelect>
        <name>visibility</name>
        <number>6</number>
        <prettyName>Macro visibility</prettyName>
        <relationalStorage>0</relationalStorage>
        <separator>|</separator>
        <separators>|</separators>
        <size>1</size>
        <unmodifiable>0</unmodifiable>
        <values>Current User|Current Wiki|Global</values>
        <classType>com.xpn.xwiki.objects.classes.StaticListClass</classType>
      </visibility>
    </class>
    <property>
      <async_cached>0</async_cached>
    </property>
    <property>
      <async_context/>
    </property>
    <property>
      <async_enabled>0</async_enabled>
    </property>
    <property>
      <code>{{include reference="XWiki.Notifications.Code.NotificationsPreferencesMacros" /}}

{{velocity}}
#checkMacroNotificationPreferencesParameters($checkResult)
#if ($checkResult)
#if ($wikimacro.parameters.target == "wiki")
  #set ($divData = "data-doc-url=""$escapetool.xml($services.rest.url($services.model.createDocumentReference('', ['XWiki', 'Notifications', 'Code'], 'NotificationAdministration')))""")
#else
  #if ("$!wikimacro.parameters.user" != "")
    #set ($targetUser = $wikimacro.parameters.user.reference)
  #else
    #set ($targetUser = $xcontext.userReference)
  #end
  #set ($divData = "data-doc-url=""$escapetool.xml($services.rest.url($targetUser))"" data-user=""$services.model.serialize($targetUser)""")
#end
######################################################
### CSS and JAVASCRIPTS
######################################################
#set ($discard = $xwiki.jsx.use('XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro'))
#set ($discard = $xwiki.ssx.use('XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro'))
## TODO: replace by $xwiki.sswx.use() or something like this when XWIKI-12788 is closed.
#set ($discard = $xwiki.linkx.use($services.webjars.url('bootstrap-switch', 'css/bootstrap3/bootstrap-switch.min.css'), {'type': 'text/css', 'rel': 'stylesheet'}))
######################################################
### MACRO CONTENT
######################################################
{{html clean="false"}}
&lt;div class="systemFilterPreferences xform" data-target="$escapetool.xml($wikimacro.parameters.target)" $divData&gt;
  &lt;div class="row"&gt;
    &lt;p class="xHint col-xs-12 col-sm-9 col-md-8 col-lg-9"&gt;
      $escapetool.xml($services.localization.render('notifications.settings.filters.preferences.system.hint'))
    &lt;/p&gt;
  &lt;/div&gt;
  #set($collist  = ['name', 'filterType', 'notificationFormats', 'isEnabled'])
  #set($colprops = {
    'name':                { 'sortable': false, 'html': true, 'filterable': false },
    'filterType':          { 'sortable': false, 'filterable': false },
    'notificationFormats': { 'sortable': false, 'html': true, 'filterable': false },
    'isEnabled':           { 'sortable': false, 'html' : true, 'filterable': false }
  })
  #set ($extraParams = "eventType=&amp;format=&amp;type=system")
  #if ($wikimacro.parameters.target == 'user')
    #set ($extraParams = "$extraParams&amp;user=${services.model.serialize($targetUser, 'default')}")
  #end
  #set($options  = {
    'resultPage'        : 'XWiki.Notifications.Code.NotificationFilterPreferenceLivetableResults',
    'rowCount'          : 10,
    'description'       : 'This table lists every system filter registered in the wiki.',
    'translationPrefix' : 'notifications.settings.filters.preferences.system.table.',
    'extraParams'       : $extraParams,
    'outputOnlyHtml'    : true
  })

  #livetable("notificationSystemFilterPreferencesLiveTable" $collist $colprops $options)
&lt;/div&gt;
{{/html}}
#end
{{/velocity}}</code>
    </property>
    <property>
      <contentDescription/>
    </property>
    <property>
      <contentJavaType/>
    </property>
    <property>
      <contentType>No content</contentType>
    </property>
    <property>
      <defaultCategories>
        <value>Notifications</value>
      </defaultCategories>
    </property>
    <property>
      <description>Display the preferences of the given user about system notification filters.</description>
    </property>
    <property>
      <id>notificationsSystemFiltersPreferences</id>
    </property>
    <property>
      <name>Notifications System Filters Preferences</name>
    </property>
    <property>
      <priority/>
    </property>
    <property>
      <supportsInlineMode>0</supportsInlineMode>
    </property>
    <property>
      <visibility>Current Wiki</visibility>
    </property>
  </object>
  <object>
    <name>XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro</name>
    <number>0</number>
    <className>XWiki.WikiMacroParameterClass</className>
    <guid>d4798048-ddea-4fa6-a959-4a01d1e9bae9</guid>
    <class>
      <name>XWiki.WikiMacroParameterClass</name>
      <customClass/>
      <customMapping/>
      <defaultViewSheet/>
      <defaultEditSheet/>
      <defaultWeb/>
      <nameField/>
      <validationScript/>
      <defaultValue>
        <disabled>0</disabled>
        <name>defaultValue</name>
        <number>4</number>
        <prettyName>Parameter default value</prettyName>
        <size>30</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </defaultValue>
      <description>
        <disabled>0</disabled>
        <name>description</name>
        <number>2</number>
        <prettyName>Parameter description</prettyName>
        <rows>5</rows>
        <size>40</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.TextAreaClass</classType>
      </description>
      <mandatory>
        <disabled>0</disabled>
        <displayFormType>select</displayFormType>
        <displayType>yesno</displayType>
        <name>mandatory</name>
        <number>3</number>
        <prettyName>Parameter mandatory</prettyName>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
      </mandatory>
      <name>
        <disabled>0</disabled>
        <name>name</name>
        <number>1</number>
        <prettyName>Parameter name</prettyName>
        <size>30</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </name>
      <type>
        <disabled>0</disabled>
        <name>type</name>
        <number>5</number>
        <prettyName>Parameter type</prettyName>
        <size>60</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </type>
    </class>
    <property>
      <defaultValue/>
    </property>
    <property>
      <description>User reference of the user for which to display and manipulate the filter preferences. This parameter is optional, default value is the context user. Note that for using this parameter, the context user needs administrator right on the given user reference.</description>
    </property>
    <property>
      <mandatory>0</mandatory>
    </property>
    <property>
      <name>user</name>
    </property>
    <property>
      <type>org.xwiki.user.UserReference</type>
    </property>
  </object>
  <object>
    <name>XWiki.Notifications.Code.NotificationsSystemFiltersPreferencesMacro</name>
    <number>1</number>
    <className>XWiki.WikiMacroParameterClass</className>
    <guid>77306320-9711-4c7f-afd2-3c92b89fbf7c</guid>
    <class>
      <name>XWiki.WikiMacroParameterClass</name>
      <customClass/>
      <customMapping/>
      <defaultViewSheet/>
      <defaultEditSheet/>
      <defaultWeb/>
      <nameField/>
      <validationScript/>
      <defaultValue>
        <disabled>0</disabled>
        <name>defaultValue</name>
        <number>4</number>
        <prettyName>Parameter default value</prettyName>
        <size>30</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </defaultValue>
      <description>
        <disabled>0</disabled>
        <name>description</name>
        <number>2</number>
        <prettyName>Parameter description</prettyName>
        <rows>5</rows>
        <size>40</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.TextAreaClass</classType>
      </description>
      <mandatory>
        <disabled>0</disabled>
        <displayFormType>select</displayFormType>
        <displayType>yesno</displayType>
        <name>mandatory</name>
        <number>3</number>
        <prettyName>Parameter mandatory</prettyName>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.BooleanClass</classType>
      </mandatory>
      <name>
        <disabled>0</disabled>
        <name>name</name>
        <number>1</number>
        <prettyName>Parameter name</prettyName>
        <size>30</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </name>
      <type>
        <disabled>0</disabled>
        <name>type</name>
        <number>5</number>
        <prettyName>Parameter type</prettyName>
        <size>60</size>
        <unmodifiable>0</unmodifiable>
        <classType>com.xpn.xwiki.objects.classes.StringClass</classType>
      </type>
    </class>
    <property>
      <defaultValue>user</defaultValue>
    </property>
    <property>
      <description>Target of the macro (could be "user" for the given user or "wiki" for the current wiki)</description>
    </property>
    <property>
      <mandatory>0</mandatory>
    </property>
    <property>
      <name>target</name>
    </property>
    <property>
      <type/>
    </property>
  </object>
</xwikidoc>
