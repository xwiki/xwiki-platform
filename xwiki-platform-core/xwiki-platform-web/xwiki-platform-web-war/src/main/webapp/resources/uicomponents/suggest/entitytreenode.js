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
require(['jquery', 'xwiki-events-bridge'], function ($) {

  function syncContainer(index, container) {
    let hiddenInput = $(container).find('input.entity-tree-node-input');
    let typeInput = $(container).find('input.entityType');
    let referenceInput = $(container).find('input.entity-tree-node-text');
    let typeLabel = $(container).find('.entity-type-label');
    let typeSelector = $(container).find('.type-selector');
    $(container).find('a.type-change-link').on('click', function (event) {
      event.preventDefault();
      let type = $(this).data('type');
      let pickerType = $(this).data('pickerType');
      typeInput.val(pickerType);
      typeInput.trigger('change');
      typeLabel.text($(this).text());
      typeLabel.data('type', type);
      syncValues(hiddenInput, type, referenceInput.val());
      setTypeSelectorStyle(typeSelector, pickerType);
    });
    referenceInput.on('change', function() {
      syncValues(hiddenInput, typeLabel.data('type'), referenceInput.val());
      setTypeSelectorStyle(typeSelector, typeMapping[typeLabel.data('type')].pickerType);
    });
    let typeMapping = {};
    $(container).find('a.type-change-link').each((index, element) => {
      typeMapping[$(element).data('type')] = {
        'pickerType': $(element).data('pickerType'),
        'label': $(element).text()
      };
    });
    hiddenInput.on('change', function() {
      setValuesFromHiddenInput(hiddenInput, typeInput, referenceInput, typeLabel, typeMapping);
    });
    setValuesFromHiddenInput(hiddenInput, typeInput, referenceInput, typeLabel, typeMapping);
    setTypeSelectorStyle(typeSelector, typeMapping[typeLabel.data('type')].pickerType);
    $(container).addClass('initialized');
  }

  function setTypeSelectorStyle(typeSelector, pickerType) {
    typeSelector.toggleClass('type-selector-with-suggest', pickerType !== 'CUSTOM');
  }

  function setValuesFromHiddenInput(hiddenInput, typeInput, referenceInput, typeLabel, typeMapping) {
    let fullRef = hiddenInput.val() || '';
    let firstColon = fullRef.indexOf(':');
    if (firstColon !== -1) {
      let type = fullRef.substring(0, firstColon);
      let reference = fullRef.substring(firstColon + 1);
      let dataType = typeMapping[type];
      typeInput.val(dataType.pickerType);
      referenceInput.val(reference);
      typeInput.trigger('change');
      typeLabel.text(dataType.label);
      typeLabel.data('type', type);
    }
  }

  function syncValues(hiddenInput, typeValue, referenceValue)
  {
    hiddenInput.val(typeValue + ':' + referenceValue);
  }

  function init() {
    $('.entity-tree-node-picker').not('.initialized').map(syncContainer);
  }
  $(document).on('xwiki:dom:updated', function (event, data) {
    $(data.elements).find('.entity-tree-node-picker').not('.initialized').map(syncContainer);
  });
  $(init);
});