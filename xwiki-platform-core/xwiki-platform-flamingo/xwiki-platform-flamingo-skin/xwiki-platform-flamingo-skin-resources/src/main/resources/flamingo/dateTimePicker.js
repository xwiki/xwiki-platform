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
  'bootstrap-datetimepicker': $services.webjars.url('Eonasdan-bootstrap-datetimepicker',
    'js/bootstrap-datetimepicker.min')
})
#[[*/
// Start JavaScript-only code.
(function(paths) {
  "use strict";

  require.config({
    paths: paths
  });

  require([
    'jquery',
    'moment',
    'moment-jdateformatparser',
    'bootstrap',
    'bootstrap-datetimepicker',
    'xwiki-events-bridge'
  ], function($, moment) {
    var init = function(event, data) {
      var container = $((data && data.elements) || document);
      container.find('input.datetime').each(function() {
        var dateTimeInput = $(this);
        maybeLoadLocale(dateTimeInput.data('locale')).then(createDateTimePicker.bind(null, dateTimeInput));
      });
    };

    var maybeLoadLocale = function(locale) {
      return new Promise((resolve, reject) => {
        // Convert the Java locale name to the moment locale name (en_US -> en-us).
        var momentLocale = locale.toLowerCase().replace(/_/g, '-');
        // Check if the locale is already loaded.
        if (moment.locales().indexOf(momentLocale) < 0) {
          require(['moment/locale/' + momentLocale], resolve, resolve);
        } else {
          resolve();
        }
      });
    };

    var createDateTimePicker = function(dateTimeInput) {
      // Attach the date picker to the body element if the date input has a parent with position relative and overflow
      // hidden, otherwise the date picker might not be fully visible and accessible.
      var shouldAttachToBody = dateTimeInput.parentsUntil('body').filter(function() {
        var parent = $(this);
        return (parent.css('overflow-x') !== 'visible' || parent.css('overflow-y') !== 'visible') &&
          parent.css('position') === 'relative';
      }).length > 0;

      dateTimeInput.datetimepicker({
        // See http://eonasdan.github.io/bootstrap-datetimepicker/Options/
        locale: dateTimeInput.data('locale'),
        format: moment().toMomentFormatString(dateTimeInput.data('format')),
        // Attach the date picker to the body element if needed, in order to be sure it's fully visible.
        widgetParent: shouldAttachToBody ? 'body' : null
      });

      // Fix the date picker position when it is attached to the body element.
      // See https://github.com/Eonasdan/bootstrap-datetimepicker/issues/1762
      dateTimeInput.on('dp.show', function() {
        var dateTimePicker = $('body').children('.bootstrap-datetimepicker-widget').last();
        if (dateTimePicker.hasClass('bottom')) {
          dateTimePicker.css({
            top: (dateTimeInput.offset().top + dateTimeInput.outerHeight()) + 'px',
            bottom: 'auto',
            left: dateTimeInput.offset().left + 'px'
          });
        } else if (dateTimePicker.hasClass('top')) {
          dateTimePicker.css({
            top: (dateTimeInput.offset().top - dateTimePicker.outerHeight() - 4) + 'px',
            bottom: 'auto',
            left: dateTimeInput.offset().left + 'px'
          });
        }
      });

      // Open the date time picker if the input is already focused.
      if (dateTimeInput.is(':focus')) {
        dateTimeInput.data('DateTimePicker').show();
      }
    };

    $(document).on('xwiki:dom:updated', init);
    return $(init);
  });

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths]));
