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
(function() {
  'use strict';
  CKEDITOR.plugins.add('xwiki-table', {
    requires: 'table'
  });

  CKEDITOR.on('dialogDefinition', function(event) {
    // Make sure we affect only the editors that load this plugin.
    if (!event.editor.plugins['xwiki-table']) {
      return;
    }

    // Take the dialog window name and its definition from the event data.
    var dialogName = event.data.name;
    var dialogDefinition = event.data.definition;
    if (dialogName === 'table') {
      enhanceTableDialog(dialogDefinition, event.editor);
    }
  });

  var enhanceTableDialog = function(dialogDefinition, editor) {
    var infoTab = dialogDefinition.getContents('info');
    // Reset the default values.
    ['txtBorder', 'txtWidth', 'txtCellSpace', 'txtCellPad'].forEach(function(fieldId) {
      delete infoTab.get(fieldId)['default'];
    });
  };
})();
