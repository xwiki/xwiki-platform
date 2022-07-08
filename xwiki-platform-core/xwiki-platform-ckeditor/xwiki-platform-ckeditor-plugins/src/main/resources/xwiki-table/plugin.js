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
    requires: 'table',
    init: function(editor) {
      // The table plugin is using the deprecated align attribute for various reasons. See
      // https://dev.ckeditor.com/ticket/3762 . We could overwrite the table dialog to use the margin:auto style but it
      // won't fix the tables that are pasted. XWiki has a HTML cleaning filter on the server side that handles the
      // align attribute, but it converts it to the float style which doesn't work for tables. This means the alignment
      // can be lost for pasted tables. To overcome this issues we added support for replacing the align attribute with
      // the margin:auto style.
      // CKEDITOR-73: CKEditor is not center-aligning table that's copied from a Word file where it is center-aligned
      var alignWithMarginAuto = !editor.config.tableUseAlignAttribute;

      // Filter the editor input.
      var dataFilter = editor.dataProcessor && editor.dataProcessor.dataFilter;
      if (dataFilter && alignWithMarginAuto) {
        dataFilter.addRules(replaceMarginAutoWithAlign, {priority: 5});
      }

      // Filter the editor output.
      var htmlFilter = editor.dataProcessor && editor.dataProcessor.htmlFilter;
      if (htmlFilter && alignWithMarginAuto) {
        htmlFilter.addRules(replaceAlignWithMarginAuto, {priority: 14, applyToAll: true});
      }
    }
  });

  //
  // Enhance the table dialog.
  //

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

  //
  // margin:auto style -> align attribute
  //

  var hasMarginProperty = /\bmargin\b/i;

  var replaceMarginAutoWithAlign = {
    elements: {
      table: function(table) {
        if (!hasMarginProperty.test(table.attributes.style)) {
          return;
        }
        var styles = table.styles || CKEDITOR.tools.parseCssText(table.attributes.style, true);
        splitShorthandProperty(styles, 'margin');
        if (styles['margin-left'] === 'auto') {
          delete styles['margin-left'];
          if (styles['margin-right'] === 'auto') {
            delete styles['margin-right'];
            table.attributes.align = 'center';
          } else {
            table.attributes.align = 'right';
          }
          table.attributes.style = CKEDITOR.tools.writeCssText(styles);
        } else if (styles['margin-right'] === 'auto') {
          delete styles['margin-right'];
          table.attributes.align = 'left';
          table.attributes.style = CKEDITOR.tools.writeCssText(styles);
        }
      }
    }
  };

  var getSubProperties = function(property) {
    return [
      property + '-top',
      property + '-right',
      property + '-bottom',
      property + '-left'
    ];
  };

  var shorthandPropertyMapping = [
    [0, 0, 0, 0],
    [0, 1, 0, 1],
    [0, 1, 2, 1],
    [0, 1, 2, 3]
  ];

  var splitShorthandValue = function(value, properties, styles) {
    var values = value.split(/\s+/);
    shorthandPropertyMapping[values.length - 1].forEach(function(valueIndex, propertyIndex) {
      styles[properties[propertyIndex]] = values[valueIndex];
    });
  };

  // Alternative to CKEDITOR.filter.transformationsTools.splitMarginShorthand() which doesn't behave correctly when the
  // value contains keywords like 'auto'.
  var splitShorthandProperty = function(styles, property) {
    if (styles[property]) {
      splitShorthandValue(styles[property], getSubProperties(property), styles);
      delete styles[property];
    }
  };

  //
  // align attribute -> margin:auto style
  //

  var alignToMargin = {
    left: ['margin-right'],
    right: ['margin-left'],
    center: ['margin-left', 'margin-right']
  };

  var replaceAlignWithMarginAuto = {
    elements: {
      table: function(table) {
        var properties = alignToMargin[table.attributes.align];
        if (properties) {
          delete table.attributes.align;
          var styles = table.styles || CKEDITOR.tools.parseCssText(table.attributes.style || '', true);
          splitShorthandProperty(styles, 'margin');
          properties.forEach(function(property) {
            styles[property] = 'auto';
          });
          joinShorthandProperty(styles, 'margin');
          table.attributes.style = CKEDITOR.tools.writeCssText(styles);
        }
      }
    }
  };

  var joinShorthandValue = function(values) {
    if (values[1] === values[3]) {
      values = values.slice(0, 3);
      if (values[0] === values[2]) {
        values = values.slice(0, 2);
        if (values[0] === values[1]) {
          values = values.slice(0, 1);
        }
      }
    }
    return values;
  };

  var joinShorthandProperty = function(styles, property) {
    var subProperties = getSubProperties(property);
    var values = [];
    for (var i = 0; i < subProperties.length; i++) {
      var value = styles[subProperties[i]];
      if (value) {
        values.push(value);
      } else {
        return;
      }
    }
    styles[property] = joinShorthandValue(values).join(' ');
    subProperties.forEach(function(subProperty) {
      delete styles[subProperty];
    });
  };

  CKEDITOR.plugins.xwikiTable = {
    replaceMarginAutoWithAlign: replaceMarginAutoWithAlign.elements.table,
    replaceAlignWithMarginAuto: replaceAlignWithMarginAuto.elements.table
  };
})();
