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
/*!
#set ($paths = {
  'xwiki-suggestPages': $xwiki.getSkinFile('uicomponents/suggest/suggestPages.js', true),
  'xwiki-suggestAttachments': $xwiki.getSkinFile('uicomponents/suggest/suggestAttachments.js')
})
#[[*/
// Start JavaScript-only code.
(function(paths) {
  "use strict";

require.config({paths});

define('xwiki-entityTypeSwitch', ['jquery', 'xwiki-suggestPages', 'xwiki-suggestAttachments'], function($) {
  var maybeEnableEntityTypeSwitch = function(entityTypeField) {
    var referenceField = getReferenceField(entityTypeField);
    entityTypeField.on('change', function(event) {
      updateReferencePicker(referenceField, entityTypeField.val());
    });
    // Activate the picker that corresponds to the default entity type.
    updateReferencePicker(referenceField, entityTypeField.val());
  };

  var getReferenceField = function(entityTypeField) {
    var propertyGroup = entityTypeField.data('propertyGroup');
    if (propertyGroup) {
      // Go up in the DOM tree and look for reference fields in the same property group.
      var parents = entityTypeField.parents();
      for (var i = 0; i < parents.length; i++) {
        var referenceField = $(parents[i]).find('.entityReferenceString[data-property-group]').filter(function() {
          return $(this).data('propertyGroup') === propertyGroup;
        });
        if (referenceField.length > 0) {
          return referenceField;
        }
      }
    }
    return $();
  };

  var entityTypeToPickerFactory = {
    'DOCUMENT': 'suggestPages',
    'ATTACHMENT': 'suggestAttachments'
  };

  var updateReferencePicker = function(referenceField, entityType) {
    // Destroy the current picker.
    referenceField.each(function() {
      if (this.selectize) {
        this.selectize.destroy();
      }
    });
    var pickerFactory = entityTypeToPickerFactory[entityType];
    if (pickerFactory && typeof referenceField[pickerFactory] === 'function') {
      referenceField[pickerFactory].call(referenceField);
    }
  };

  $.fn.switchEntityType = function() {
    return this.not('.initialized').each(function() {
      maybeEnableEntityTypeSwitch($(this).addClass('initialized'));
    });
  };
});

require(['jquery', 'xwiki-entityTypeSwitch', 'xwiki-events-bridge'], function($) {
  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.entityType[data-property-group]').switchEntityType();
  };

  $(document).on('xwiki:dom:updated', init);
  $(init);
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths]));
