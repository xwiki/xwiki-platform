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

<xwikidoc version="1.5" reference="XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro" locale="">
  <web>XWiki.Notifications.Code</web>
  <name>NotificationsFiltersPreferencesMacro</name>
  <language/>
  <defaultLanguage/>
  <translation>0</translation>
  <creator>xwiki:XWiki.Admin</creator>
  <parent>XWiki.Notifications.Code.WebHome</parent>
  <author>xwiki:XWiki.Admin</author>
  <contentAuthor>xwiki:XWiki.Admin</contentAuthor>
  <version>1.1</version>
  <title>NotificationsFiltersPreferencesMacro</title>
  <comment/>
  <minorEdit>false</minorEdit>
  <syntaxId>xwiki/2.1</syntaxId>
  <hidden>true</hidden>
  <content/>
  <object>
    <name>XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro</name>
    <number>0</number>
    <className>XWiki.JavaScriptExtension</className>
    <guid>35d1d8c7-f9e7-4cad-a505-5e46ddaf5d79</guid>
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
require(['jquery', 'AddNotificationFilterPreferenceLivetable', 'xwiki-bootstrap-switch', 'xwiki-events-bridge'], function ($, AddNotificationFilterPreferenceLivetable) {

  /**
   * Page initialization
   */
  $(function() {

    new AddNotificationFilterPreferenceLivetable($('#modal-add-filter-preference'), window['livetable_notificationFilterPreferencesLiveTable'], $('.filterPreferences button.btn-addfilter'),
      $('.filterPreferences').attr('data-user-url'));
    var initBootstrapSwitches = function () {
      $('#notificationFilterPreferencesLiveTable .notificationFilterPreferenceCheckbox, #notificationFilterPreferencesLiveTable .toggleableFilterPreferenceCheckbox').bootstrapSwitch({
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
    <name>XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro</name>
    <number>1</number>
    <className>XWiki.JavaScriptExtension</className>
    <guid>19d1a6ca-02cf-41f5-81fb-415e557974dc</guid>
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
      <code>define('AddNotificationFilterPreferenceLivetable', ['jquery', 'xwiki-meta', 'bootstrap'], function($, xm) {

  return function(modal, filterPreferencesLivetable, buttonAddFilter, userURL) {
    var self = this;
    self.modal = modal;
    self.filterPreferencesLivetable = filterPreferencesLivetable;
    self.buttonAddFilter = buttonAddFilter;
    self.userURL = userURL;
    self.filterTypeSelector = modal.find('select#notificationFilterTypeSelector');
    self.eventTypeSelector = modal.find('select#notificationFilterEventTypeSelector');
    self.notificationFormatSelector = modal.find('select#notificationFilterNotificationFormatSelector');
    self.filterScopeTree = modal.find('.location-tree');
    self.cancelButton = modal.find('button[data-action="cancel"]');
    self.submitButton = modal.find('button[data-action="submit"]');

    /**
     * Initialization
     */
    self.init = function () {
      self.buttonAddFilter.on('click', function() {
        self.filterScopeTree.xtree();
        self.submitButton.prop('disabled', 'disabled');

        self.filterScopeTree.on('changed.jstree', function(e, data) {
          if (data.selected.length &gt; 0) {
            self.submitButton.prop('disabled', '');
          }
        });
      });

      self.submitButton.on('click', self.onSelectButtonClicked);
    };

    /**
     * On "submit" button clicked
     */
    self.onSelectButtonClicked = function() {
      self.modal.modal('toggle');

      var tree = $.jstree.reference(self.filterScopeTree);
      var nodes = tree.get_selected(true);

      var serviceReference = XWiki.Model.resolve('XWiki.Notifications.Code.NotificationPreferenceService', XWiki.EntityType.DOCUMENT);
      var serviceURL = new XWiki.Document(serviceReference).getURL('get', 'outputSyntax=plain');

      var eventTypesSelected = self.eventTypeSelector.val();
      var eventTypes;
      if (eventTypesSelected != null &amp;&amp; eventTypesSelected.length &gt; 0) {
        eventTypes = eventTypesSelected.join(',');
      }
      var params = {
        action: 'createScopeFilterPreference',
        target: 'user',
        user: self.modal.data('user'),
        filterType: self.filterTypeSelector.val(),
        filterFormats: self.notificationFormatSelector.val().join(','),
        eventTypes: eventTypes,
        csrf: xm.form_token,
        wiki: [],
        space: [],
        page: []
      }

      for (var i = 0; i &lt; nodes.length; ++i) {
        var node = nodes[i];
        var reference = node.data.id;
        if (node.data.type == 'wiki') {
          params['wiki'].push(reference);
        } else if (node.data.type == 'document' &amp;&amp; node.data.id.match('WebHome$')) {
          params['space'].push(reference.substring(0, reference.length - '.WebHome'.length));
        } else {
          params['page'].push(reference);
        }
      }

      // Saving
      var notification = new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.settings.saving'))", 'inprogress');
      // We use a "traditional: true" argument here to be sure that the argument names are not transformed to wiki[] etc by jquery. For more info see doc about $.param.
      $.post({
        url: serviceURL,
        data: params,
        traditional: true
      }).then(data =&gt; {
        notification.hide();
        new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.settings.saved'))", 'done');

        // Reload the livetable
        self.filterPreferencesLivetable.refresh();
      }).catch(() =&gt; {
        notification.hide();
        new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.settings.savingfailed'))", 'error');
      });
    };

    // Call init()
    self.init();
  };

});
</code>
    </property>
    <property>
      <name>Add Notification Filter Preference Livetable</name>
    </property>
    <property>
      <parse>1</parse>
    </property>
    <property>
      <use>currentPage</use>
    </property>
  </object>
  <object>
    <name>XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro</name>
    <number>2</number>
    <className>XWiki.JavaScriptExtension</className>
    <guid>53f67179-890e-4d1f-aec6-69873f9f87f4</guid>
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
      <code>define('ToggleableFilterPreference', ['jquery', 'xwiki-bootstrap-switch'], function($) {
  /**
  * Construct the Toggleable Filter Preference object.
  */
  return function(domElement, userURL, row) {
    var self               = this;
    self.domElement        = $(domElement);
    self.userURL           = userURL;
    self.row               = row;
    /**
     * Initialization
     */
    self.init = function () {
      // On change
      self.domElement.on('switchChange.bootstrapSwitch', function(event, state) {
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
        var restURL = self.userURL + '/objects?media=json&amp;minorRevision=true';
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
        var restURL = self.userURL + '/objects/XWiki.Notifications.Code.ToggleableFilterPreferenceClass/' + objectNumber
          + '/properties/isEnabled?media=json&amp;minorRevision=true';
        $.ajax(restURL, {
          method: 'PUT',
          contentType: 'text/plain',
          data: state ? '1' : '0'
        }).then(data =&gt; {
          notification.hide();
          new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.setEnabled.done'))", 'done');
          $(document).trigger('xwiki:livetable:notifications-filters:toggle', {'row': self.row, 'state': state});
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
      <name>Toggleable Filter Preference</name>
    </property>
    <property>
      <parse>1</parse>
    </property>
    <property>
      <use>currentPage</use>
    </property>
  </object>
  <object>
    <name>XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro</name>
    <number>3</number>
    <className>XWiki.JavaScriptExtension</className>
    <guid>de91de12-11fa-4ec8-9e37-1d231500dedb</guid>
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
      <code>require(['jquery', 'xwiki-meta', 'ToggleableFilterPreference', 'xwiki-bootstrap-switch', 'xwiki-events-bridge'], function ($, xm, ToggleableFilterPreference) {

  $(document).on('xwiki:livetable:notifications-filters:toggle', function(event, data) {
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
  var serviceReference = XWiki.Model.resolve('XWiki.Notifications.Code.NotificationPreferenceService', XWiki.EntityType.DOCUMENT);
  var serviceURL = new XWiki.Document(serviceReference).getURL('get', 'outputSyntax=plain');
  var userURL = $('.filterPreferences').attr('data-user-url');
  var targetUser = $('.filterPreferences').attr('data-user');

  // Callback on livetable row printing
  $(document).on('xwiki:livetable:notificationFilterPreferencesLiveTable:newrow', function(event, data) {
    // Delete action
    $(data.row).find('a.actiondelete').on('click', function(event) {
      event.preventDefault();
      new XWiki.widgets.ConfirmationBox({
        onYes: function() {
          var notif = new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.delete.inProgress'))", 'inprogress');
          $.post(serviceURL, {
            action: 'deleteFilterPreference',
            target: 'user',
            user: targetUser,
            filterPreferenceId: data.data.filterPreferenceId,
            csrf: xm.form_token
          }).then(() =&gt; {
            notif.hide();
            new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.delete.done'))", 'done');
            window.livetable_notificationFilterPreferencesLiveTable.refresh();
          }).catch(() =&gt; {
            notif.hide();
            new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.delete.error'))", 'error');
          });
        }
      });
    });
    // Enable / Disable action for classic filter preferences
    $(data.row).find('.notificationFilterPreferenceCheckbox').on('switchChange.bootstrapSwitch', function(event, state) {
      // TODO: Translation keys
      var notif = new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.setEnabled.inProgress'))", 'inprogress');
      $.post(serviceURL, {
        action: 'setFilterPreferenceEnabled',
        filterPreferenceId: data.data.filterPreferenceId,
        target: 'user',
        user: targetUser,
        enabled: state,
        csrf: xm.form_token
      }).then(() =&gt; {
        notif.hide();
        new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.setEnabled.done'))", 'done');
        $(document).trigger('xwiki:livetable:notifications-filters:toggle', {'row': data.data, 'state': state});
      }).catch(() =&gt; {
        notif.hide();
        new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('notifications.filters.preferences.setEnabled.error'))", 'error');
      });
    });
    // Enable / Disable action for toggleable filter preferences
    $(data.row).find('.toggleableFilterPreferenceCheckbox').each(function() {
      new ToggleableFilterPreference(this, userURL, data.data);
    });
  });

  // Initializer
  $(function() {
    // This script is usually loaded after the livetable is displayed and so the previous callback is not called.
    // Because of that, we need
    if (window.livetable_notificationFilterPreferencesLiveTable) {
      window.livetable_notificationFilterPreferencesLiveTable.refresh();
    }
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
    <name>XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro</name>
    <number>0</number>
    <className>XWiki.StyleSheetExtension</className>
    <guid>068bd3af-8e02-45bb-b1dc-0e99095bf10a</guid>
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
      <code>#notificationFilterPreferencesLiveTable select.pagesizeselect {
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
    <name>XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro</name>
    <number>0</number>
    <className>XWiki.WikiMacroClass</className>
    <guid>35dc5b39-6a83-4d70-bdb5-531f67b51e12</guid>
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
      <code>{{template name="locationPicker_macros.vm" /}}

{{include reference="XWiki.Notifications.Code.NotificationsPreferencesMacros" /}}

{{velocity}}
#if (!$xcontext.userReference)
  {{info}}
    {{translation key="notifications.settings.filters.preferences.forGuest" /}}
  {{/info}}
## FIXME: This check should not be needed with a proper API able to check rights on UserReference.
## This should be improved later with such API.
#elseif ("$!wikimacro.parameters.user" != "" &amp;&amp; $wikimacro.parameters.user.class.simpleName != 'DocumentUserReference')
  {{error}}
    This macro only allows to handle DocumentUserReference references and you specified a $services.rendering.escape($wikimacro.parameters.user.class.simpleName, 'xwiki/2.1') reference.
  {{/error}}
#elseif ("$!wikimacro.parameters.user" != "" &amp;&amp; !$services.security.authorization.hasAccess('admin', $wikimacro.parameters.user.reference) &amp;&amp; !$xcontext.userReference.equals($wikimacro.parameters.user.reference))
  {{error}}
    You don't have administration right on $services.rendering.escape($wikimacro.parameters.user, 'xwiki/2.1').
  {{/error}}
#else
#set ($discard = $services.logging.deprecate("NotificationsFiltersPreferencesMacro", "This macro should not be used anymore in favor of SystemNotificationsFiltersPreferencesMacro and CustomNotificationsFiltersPreferencesMacro."))
## TODO: We should actually manipulate only UserReferences here.
#if ("$!wikimacro.parameters.user" != "")
  #set ($targetUser = $wikimacro.parameters.user.reference)
#else
  #set ($targetUser = $xcontext.userReference)
#end
######################################################
### CSS and JAVASCRIPTS
######################################################
#set ($discard = $xwiki.jsx.use('XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro'))
#set ($discard = $xwiki.ssx.use('XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro'))
## TODO: replace by $xwiki.sswx.use() or something like this when XWIKI-12788 is closed.
#set ($discard = $xwiki.linkx.use($services.webjars.url('bootstrap-switch', 'css/bootstrap3/bootstrap-switch.min.css'), {'type': 'text/css', 'rel': 'stylesheet'}))
######################################################
### MACRO CONTENT
######################################################
{{html clean="false"}}
&lt;div class="filterPreferences xform" data-user-url="$escapetool.xml($services.rest.url($targetUser))"
  data-user="$escapetool.xml($services.model.serialize($targetUser))"&gt;
  &lt;div class="row"&gt;
    &lt;p class="xHint col-xs-12 col-sm-9 col-md-8 col-lg-9"&gt;
      $escapetool.xml($services.localization.render('notifications.settings.filters.preferences.hint'))
    &lt;/p&gt;
    &lt;div class="col-xs-12 col-sm-3 col-md-4 col-lg-3 text-right"&gt;
      &lt;button type="button" class="btn btn-default btn-addfilter" data-target="#modal-add-filter-preference" data-toggle="modal"&gt;
        &lt;span class="fa fa-plus"&gt;&lt;/span&gt;&amp;nbsp;$escapetool.xml($services.localization.render('notifications.settings.addFilter'))
      &lt;/button&gt;
    &lt;/div&gt;
  &lt;/div&gt;
  #set($collist  = ['name', 'filterType', 'eventTypes', 'notificationFormats', 'isEnabled', '_actions'])
  #set($colprops = {
    'name':                { 'sortable': false, 'html': true, 'filterable': false },
    'filterType':          { 'sortable': false, 'filterable': false },
    'eventTypes':          { 'sortable': false, 'html': true, 'filterable': false },
    'notificationFormats': { 'sortable': false, 'html': true, 'filterable': false },
    'isEnabled':           { 'sortable': false, 'html' : true, 'filterable': false },
    '_actions':            { 'sortable': false, 'actions': ['delete'], 'filterable': false}
  })
  #set($options  = {
    'resultPage'        : 'XWiki.Notifications.Code.NotificationFilterPreferenceLivetableResults',
    'rowCount'          : 10,
    'description'       : 'This table lists every filter registered for the current user.',
    'translationPrefix' : 'notifications.settings.filters.preferences.table.',
    'extraParams'       : "eventType=&amp;format=&amp;user=${services.model.serialize($targetUser, 'default')}",
    'outputOnlyHtml'    : true
  })

  #livetable("notificationFilterPreferencesLiveTable" $collist $colprops $options)
&lt;/div&gt;
######################################################
### ADD FILTER MODAL
######################################################
&lt;div class="modal fade" tabindex="-1" role="dialog" id="modal-add-filter-preference"
  data-user="$escapetool.xml($services.model.serialize($targetUser))"&gt;
  &lt;div class="modal-dialog" role="document"&gt;
    &lt;div class="modal-content"&gt;
      &lt;div class="modal-header"&gt;
        &lt;button type="button" class="close" data-dismiss="modal" aria-label="Close"&gt;&lt;span aria-hidden="true"&gt;&amp;times;&lt;/span&gt;&lt;/button&gt;
        &lt;div class="modal-title"&gt;$escapetool.xml($services.localization.render('notifications.settings.addFilter'))&lt;/div&gt;
      &lt;/div&gt;
      &lt;div class="modal-body"&gt;
        &lt;div class="xform"&gt;
          &lt;dl class="location-picker"&gt;
            &lt;dt&gt;
              &lt;label for="notificationFilterTypeSelector"&gt;$escapetool.xml($services.localization.render('notifications.filters.type.label'))&lt;/label&gt;
              &lt;span class="xHint"&gt;$escapetool.xml($services.localization.render('notifications.filters.type.hint'))&lt;/span&gt;
            &lt;/dt&gt;
            &lt;dd&gt;
              &lt;select id="notificationFilterTypeSelector" name="notificationFilterTypeSelector" size="1"&gt;
                &lt;option value="inclusive" selected="selected"&gt;$escapetool.xml($services.localization.render('notifications.filters.preferences.addPreference.filterType.inclusive'))&lt;/option&gt;
                &lt;option value="exclusive"&gt;$escapetool.xml($services.localization.render('notifications.filters.preferences.addPreference.filterType.exclusive'))&lt;/option&gt;
              &lt;/select&gt;
            &lt;/dd&gt;
            &lt;dt&gt;
              &lt;label for="notificationFilterNotificationFormatSelector"&gt;$escapetool.xml($services.localization.render('notifications.filters.preferences.addPreference.notificationFormat.label'))&lt;/label&gt;
              &lt;span class="xHint"&gt;$escapetool.xml($services.localization.render('notifications.filters.preferences.addPreference.notificationFormat.hint'))&lt;/span&gt;
            &lt;/dt&gt;
            &lt;dd&gt;
              &lt;select id="notificationFilterNotificationFormatSelector" name="notificationFilterNotificationFormatSelector" size="2" multiple&gt;
                &lt;option value="alert" selected="selected"&gt;$escapetool.xml($services.localization.render('notifications.format.alert'))&lt;/option&gt;
                &lt;option value="email"&gt;$escapetool.xml($services.localization.render('notifications.format.email'))&lt;/option&gt;
              &lt;/select&gt;
            &lt;/dd&gt;
            &lt;dt&gt;
              &lt;label for="notificationFilterEventTypeSelector"&gt;$escapetool.xml($services.localization.render('notifications.filters.preferences.addPreference.eventType.label'))&lt;/label&gt;
              &lt;span class="xHint"&gt;$escapetool.xml($services.localization.render('notifications.filters.preferences.addPreference.eventType.hint'))&lt;/span&gt;
            &lt;/dt&gt;
            &lt;dd&gt;
              &lt;select id="notificationFilterEventTypeSelector" name="notificationFilterEventTypeSelector" size="5" multiple&gt;
                #foreach ($app in $apps)
                  #set ($type = $app[0])
                  &lt;optgroup label="$escapetool.xml($type.applicationName)"&gt;
                  #foreach($descriptor in $app)
                    &lt;option value="$escapetool.xml($descriptor.eventType)"&gt;$escapetool.xml($services.localization.render($descriptor.description))&lt;/option&gt;
                  #end
                  &lt;/optgroup&gt;
                #end
              &lt;/select&gt;
            &lt;/dd&gt;
            &lt;dt&gt;
              &lt;label&gt;$escapetool.xml($services.localization.render('notifications.filters.preferences.addPreference.location.label'))&lt;/label&gt;
              &lt;span class="xHint"&gt;$escapetool.xml($services.localization.render('notifications.filters.preferences.addPreference.location.hint'))&lt;/span&gt;
            &lt;/dt&gt;
            &lt;dd class="document-tree"&gt;
              #documentTree({
                'class': 'location-tree',
                'finder': true,
                'showAttachments': false,
                'showRoot': false,
                'showTerminalDocuments': true,
                'showTranslations': false,
                'showWikis': true
              })
            &lt;/dd&gt;
          &lt;/dl&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class="modal-footer"&gt;
        &lt;button type="button" class="btn btn-default" data-action="cancel" data-dismiss="modal"&gt;$services.localization.render('notifications.filters.cancel')&lt;/button&gt;
        &lt;button type="button" class="btn btn-primary" data-action="submit" disabled="disabled"&gt;$services.localization.render('notifications.filters.submit')&lt;/button&gt;
      &lt;/div&gt;
    &lt;/div&gt;&lt;!-- /.modal-content --&gt;
  &lt;/div&gt;&lt;!-- /.modal-dialog --&gt;
&lt;/div&gt;&lt;!-- /.modal --&gt;
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
      <description>Display the preferences of the current user about notification filters.
This macro is now deprecated in favor of Custom Notifications Filters Preferences or System Notifications Filters Preferences.</description>
    </property>
    <property>
      <id>notificationsFiltersPreferences</id>
    </property>
    <property>
      <name>Notifications Filters Preferences (deprecated)</name>
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
    <name>XWiki.Notifications.Code.NotificationsFiltersPreferencesMacro</name>
    <number>0</number>
    <className>XWiki.WikiMacroParameterClass</className>
    <guid>38b562b3-61ed-40bf-8ab1-088260b579bc</guid>
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
</xwikidoc>
