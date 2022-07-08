/*
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
 */
(function (){
  'use strict';
  var $ = jQuery;

  CKEDITOR.plugins.add('xwiki-toolbar', {
    requires: 'menu,menubutton,xwiki-localization',

    afterInit: function(editor) {
      var menus = editor.config.toolbarMenus || {};
      var menuItems = editor.config.toolbarMenuItems || {};
      var allButtons = Object.keys(editor.ui.items).map(function(item) {
        return item.toLowerCase();
      });
      var removedButtons = (editor.config.removeButtons || '').toLowerCase();
      $.each(menus, function(menuId, menu) {
        var activeMenuItems = [];
        // Default menu priority.
        var groupPriority = 100;
        menu.groups.forEach(function(group) {
          // The menu separator is displayed only between groups with different priority.
          editor.addMenuGroup(group.id, groupPriority);
          groupPriority += 10;
          group.items.forEach(function(itemId, index) {
            var item = $.extend({
              // Prefix the menu item id in order to prevent conflicts with the context menu items.
              id: 'toolbar_' + itemId,
              label: 'xwiki-toolbar.' + itemId,
              command: itemId
            }, menuItems[itemId]);
            var command = item.command;
            editor.addMenuItem(item.id, {
              label: l10n(editor, item.label),
              icon: item.icon || command,
              command: command,
              data: item.data,
              group: group.id,
              // Avoid 0 index because it is considered as unset.
              order: index + 1,
              // Overwrite in order to be able to pass data when executing the command.
              onClick: function() {
                editor.execCommand(this.command, this.data);
              }
            });
            // Show only the items that either don't have an associated button or that have their button removed from
            // the tool bar.
            if (editor.commands[command] && (allButtons.indexOf(command.toLowerCase()) < 0 ||
                removedButtons.indexOf(command.toLowerCase()) >= 0)) {
              activeMenuItems.push(item);
            }
          });
        });
        if (activeMenuItems.length > 0) {
          editor.ui.add(menuId, CKEDITOR.UI_MENUBUTTON, {
            label: l10n(editor, menu.label || 'xwiki-toolbar.' + menuId),
            icon: menu.icon || activeMenuItems[0].command.toLowerCase(),
            toolbar: menu.toolbar,
            modes: {
              wysiwyg: 1
            },
            onMenu: function() {
              var active = {};
              activeMenuItems.forEach(function(item) {
                active[item.id] = editor.commands[item.command].state;
                if (item.icon && active[item.id]) {
                  editor.ui.instances[menuId]._.menu._.panelDefinition.css.push('.cke_button__' + item.command +
                    '_icon {' + CKEDITOR.skin.getIconStyle(item.icon) + '}');
                }
              });
              return active;
            },
            refresh: function() {
              // Disable the menu button if all the menu items are disabled. We do this with a short delay in order to
              // make sure that the state of the menu items has been updated.
              setTimeout((function() {
                var enabled = activeMenuItems.some(function(item) {
                  return editor.commands[item.command].state;
                });
                this.setState(enabled ? CKEDITOR.TRISTATE_OFF : CKEDITOR.TRISTATE_DISABLED);
              }).bind(this), 0);
            }
          });
        }
      });

      if (editor.ui.items.Format) {
        // Separate the Format drop down from the Styles drop down because we want to be able to place them
        // independently on the tool bar.
        editor.ui.items.Format.toolbar = 'format,10';
      }
    }
  });

  var l10n = function(editor, key) {
    if (key.substring(0, 6) === 'xwiki-') {
      return editor.localization.get(key);
    } else {
      var value = editor.lang;
      key.split('.').forEach(function(part) {
        value = (value || {})[part];
      });
      return value || key;
    }
  };
})();
