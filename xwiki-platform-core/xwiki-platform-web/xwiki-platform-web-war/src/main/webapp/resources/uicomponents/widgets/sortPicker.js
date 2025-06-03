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
require(['jquery'], function($) {
  $.fn.sortPicker = function() {
    return this.each(function() {
      const $sortPickerValue = $(this).find('.sortPicker-value');
      const $sortPickerField = $(this).find('.sortPicker-field');
      const $sortPickerOrder = $(this).find('.sortPicker-order');

      const updateSortPickerValue = () => {
        const field = $sortPickerField.val();
        const order = $sortPickerOrder.val();
        $sortPickerValue.val(field + (order ? ':' : '') + order);
      };

      const updateSortPickerFieldAndOrder = () => {
        const parts = $sortPickerValue.val().split(':', 2);
        const field = parts[0];
        $sortPickerField.val(field);
        if ($sortPickerField.val() === null) {
          // Add the missing option.
          $('<option>').text(field).appendTo($sortPickerField);
          $sortPickerField.val(field);
        }

        const order = (parts[1] || '').toLowerCase();
        $sortPickerOrder.val(order);
        // Fallback on empty value if the specified value is unknown.
        if ($sortPickerOrder.val() === null) {
          $sortPickerOrder.val('');
        }
      };

      $sortPickerField.add($sortPickerOrder).on('change', updateSortPickerValue);
      updateSortPickerFieldAndOrder();
    });
  };

  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.sortPicker').sortPicker();
  };

  $(init).on('xwiki:dom:updated', init);
});
