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
  'moment': $services.webjars.url('momentjs', 'min/moment.min'),
  'moment-jdateformatparser': $services.webjars.url('org.webjars.bower:moment-jdateformatparser',
    'moment-jdateformatparser'),
  'bootstrap-datetimepicker': $services.webjars.url('Eonasdan-bootstrap-datetimepicker',
    'js/bootstrap-datetimepicker.min')
})
*/
// Start JavaScript-only code.
(function(paths) {
  "use strict";

require.config({
  paths,
  shim: {
    // This has been fixed in the latest version of moment-jdateformatparser.
    'moment-jdateformatparser': ['moment']
  }
});

// Workaround for bug in moment-jdateformatparser (remove after upgrading to the latest version).
var module = module || false;

// Hack for a limitation in moment-jdateformatparser that should be fixed in the latest version.
require(['moment'], function(moment) {
  window.moment = moment;
  require([
    'jquery',
    'bootstrap',
    'bootstrap-datetimepicker',
    'moment-jdateformatparser',
    'xwiki-events-bridge'
  ], function($) {
    var init = function(event, data) {
      var container = $((data && data.elements) || document);
      container.find('input.datetime').each(function() {
        var dateTimeInput = $(this);

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
      });
    };

    $(document).on('xwiki:dom:updated', init);
    return XWiki.domIsLoaded && init();
  });
});

// End JavaScript-only code.
}).apply(null, $jsontool.serialize([$paths]));