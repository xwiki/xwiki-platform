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

  CKEDITOR.plugins.add('xwiki-basicstyles', {
    requires: 'menu,menubutton,xwiki-localization',

    init: function(editor) {
      // We specify the priority because the menu separator is displayed only between groups with different priority.
      editor.addMenuGroup('BasicStyles', 100);
      var basicStyles = ['bold', 'italic', 'strike', 'underline', 'subscript', 'superscript'];
      basicStyles.forEach(function(item, index) {
        editor.addMenuItem(item, {
          label: editor.lang.basicstyles[item],
          group: 'BasicStyles',
          command: item,
          order: index
        });
      });

      editor.addMenuGroup('RemoveFormat', 110);
      editor.addMenuItem('removeFormat', {
        label: editor.lang.removeformat.toolbar,
        group: 'RemoveFormat',
        command: 'removeFormat',
        order: basicStyles.length
      });

      var activeMenuItems = [];
      var removedButtons = editor.config.removeButtons.toLowerCase();
      basicStyles.concat(['removeFormat']).forEach(function(item) {
        // Show only the items that have been removed from the tool bar.
        if (editor.commands[item] && removedButtons.indexOf(item.toLowerCase()) >= 0) {
          activeMenuItems.push(item);
        }
      });

      if (activeMenuItems.length > 0) {
        editor.ui.add('BasicStyles', CKEDITOR.UI_MENUBUTTON, {
          label: editor.localization.get('xwiki-basicstyles.label'),
          icon: activeMenuItems[0].toLowerCase(),
          toolbar: 'basicstyles,70',
          modes: {
            wysiwyg: 1
          },
          onMenu: function() {
            var active = {};
            activeMenuItems.forEach(function(item) {
              active[item] = editor.commands[item].state;
            });
            return active;
				  }
        });
      }
    },

    afterInit: function(editor) {
      if (editor.ui.items.Format) {
        // Separate the Format drop down from the Styles drop down because we want to be able to place them
        // independently on the tool bar.
        editor.ui.items.Format.toolbar = 'format,10';
      }
    }
  });
})();
