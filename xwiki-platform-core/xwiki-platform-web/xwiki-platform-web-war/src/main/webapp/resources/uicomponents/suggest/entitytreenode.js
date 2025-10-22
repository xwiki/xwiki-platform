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

  function syncFields() {
    $('.entitytreenode-container').map(syncContainer);
  }

  function syncContainer(index, container) {
    let hiddenInput = $(container).find('input.entitytreenode-input');
    let typeInput = $(container).find('input.entityType');
    let referenceInput = $(container).find('input.entitytreenode-text');
    let typeLabel = $(container).find('.entitytype-label');
    $(container).find('a.type-change-link').on('click', function (event) {
      event.preventDefault();
      let type = $(this).data('type');
      let reference = $(this).data('reference');
      typeInput.val(reference.toUpperCase());
      typeInput.trigger('change');
      typeLabel.text($(this).text());
      syncValues(hiddenInput, type, referenceInput.val());
    });
    referenceInput.on('change', function() {
      syncValues(hiddenInput, typeInput.val(), referenceInput.val());
    });
    let labelMapping = {};
    $(container).find('a.type-change-link').each((index, element) => {
      labelMapping[$(element).data('reference')] = $(element).text();
    });
    hiddenInput.on('change', function() {
      setValuesFromHiddenInput(hiddenInput, typeInput, referenceInput, typeLabel, labelMapping);
    });
    setValuesFromHiddenInput(hiddenInput, typeInput, referenceInput, typeLabel, labelMapping);
  }

  function setValuesFromHiddenInput(hiddenInput, typeInput, referenceInput, typeLabel, labelMapping) {
    let fullRef = hiddenInput.val() || '';
    let firstColon = fullRef.indexOf(':');
    if (firstColon !== -1) {
      let type = fullRef.substring(0, firstColon);
      let reference = fullRef.substring(firstColon + 1);
      typeInput.val(type.toUpperCase());
      referenceInput.val(reference);
      typeInput.trigger('change');
      typeLabel.text(labelMapping[type]);
    }
  }

  function syncValues(hiddenInput, typeValue, referenceValue) {
    hiddenInput.val(typeValue.toLowerCase() + ':' + referenceValue);
  }

  (XWiki.domIsLoaded && syncFields()) || document.observe('xwiki:dom:updated', syncFields);
});