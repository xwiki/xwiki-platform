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
## Configure require.js to use Colpick (https://github.com/josedvq/colpick-jQuery-Color-Picker/)
#set ($paths = {
  'colpick': $services.webjars.url('colpick', 'js/colpick')
})
#[[*/
// Start JavaScript-only code.
(function(paths) {
  "use strict";

require.config({
  paths,
  shim: {
    'colpick' : ['jquery']
  }
});

require(['jquery', 'colpick'], function($, Colpick) {
  var initColorPicker = function() {
    var input = $(this);
    var parent = input.parent();

    // Update the color preview box.
    parent.find('.color-preview').css('background-color', input.val());

    // Enable the color picker.
    input.colpick({
      layout: 'hex',
      submit: true,
      color: input.val(),
      onShow: function() {
        $(this).addClass('active');
      },
      onHide: function() {
        $(this).removeClass('active');
      },
      onSubmit: function(hsb, hex, rgb, el) {
        var element = $(el);
        // Hide the color picker.
        element.colpickHide();
        // Update the filled value.
        element.val('#'+hex);
        // Update the color preview box.
        element.parent().find('.color-preview').css('background-color', '#'+hex);
        // Emit the "change" event.
        element.trigger('change');
      }
    });

    // Handle the case when the user types the value.
    input.on('keyup', function(event) {
      var element = $(event.target);
      // Update the color preview box.
      element.parent().find('.color-preview').css('background-color', element.val());
      // Update the color picker.
      element.colpickSetColor(element.val(), false);
    });
  };

  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.color-picker').each(initColorPicker);
  };

  // Hide the color picker if the user scrolls (this is for the color theme editor).
  var scrollTimeout;
  $('.tab-content').on('scroll', function() {
    clearTimeout(scrollTimeout);
    var container = $(this);
    scrollTimeout = setTimeout(function() {
      container.find('input.color-picker.active').colpickHide();
    }, 0);
  });

  $(document).on('xwiki:dom:updated', init);
  return XWiki.domIsLoaded && init();
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths]));