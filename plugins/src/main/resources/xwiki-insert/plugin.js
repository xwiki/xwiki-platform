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

  CKEDITOR.plugins.add('xwiki-insert', {
    requires: 'menu,menubutton,xwiki-localization',

    afterInit: function(editor) {
      editor.addMenuGroup('Insert');

      var items = [
        {command: 'image', label: editor.lang.common.image},
        {command: 'table', label: editor.lang.table.toolbar},
        {command: 'xwiki-macro', label: editor.localization.get('xwiki-macro.buttonHint')},
        // We use a custom label because we don't want the "Insert" prefix.
        {command: 'horizontalrule', label: editor.localization.get('xwiki-insert.horizontalRule')},
        {command: 'specialchar', label: editor.localization.get('xwiki-insert.specialChar')},
        {command: 'officeImporter', label: editor.localization.get('xwiki-office.importer.title')}
      ];

      var activeItems = [];
      var removedButtons = editor.config.removeButtons.toLowerCase();
      items.forEach(function(item, index) {
        var command = item.command;
        // Show only the items that have been removed from the tool bar.
        if (editor.commands[command] && removedButtons.indexOf(command.toLowerCase()) >= 0) {
          editor.addMenuItem(command, $.extend(item, {
            group: 'Insert',
            order: index
          }));
          activeItems.push(command);
        }
      });

      if (activeItems.length > 0) {
        editor.ui.add('Insert', CKEDITOR.UI_MENUBUTTON, {
          label: editor.localization.get('xwiki-insert.label'),
          toolbar: 'insert',
          modes: {
            wysiwyg: 1
          },
          onMenu: function() {
            var active = {};
            activeItems.forEach(function(item) {
              active[item] = editor.commands[item].state;
            });
            if (active.officeImporter) {
              editor.ui.instances.Insert._.menu._.panelDefinition.css.push('.cke_button__officeImporter_icon {' +
                CKEDITOR.skin.getIconStyle('pastefromword') + '}');
            }
            return active;
				  }
        });
      }
    }
  });
})();
