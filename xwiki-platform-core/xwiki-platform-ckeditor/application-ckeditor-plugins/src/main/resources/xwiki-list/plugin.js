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

  // Empty plugin for now, required to know when to customize the list dialogs, see below.
  CKEDITOR.plugins.add('xwiki-list', {
    requires: 'xwiki-dialog,xwiki-localization'
  });

  // Add 'none' to the list type options.
  // See https://github.com/ckeditor/ckeditor4/issues/3752
  CKEDITOR.on('dialogDefinition', function(event) {
    // Make sure we affect only the editors that load this plugin.
    if (!event.editor.plugins['xwiki-list']) {
      return;
    }

    // Take the dialog window name and its definition from the event data.
    var dialogName = event.data.name;
    var dialogDefinition = event.data.definition;
    if (dialogName === 'numberedListStyle' || dialogName === 'bulletedListStyle') {
      var path = CKEDITOR.plugins.xwikiDialog.getUIElementPath('type', dialogDefinition.contents);
      if (path && path.length) {
        var listTypeConfig = path[0].element;
        var listTypes = listTypeConfig.items;
        if (!listTypes.some(function(listType) {
          return listType[1] === 'none';
        })) {
          listTypes.push([event.editor.localization.get('xwiki-list.listType.none'), 'none']);
        }
      }
    }
  });
})();
