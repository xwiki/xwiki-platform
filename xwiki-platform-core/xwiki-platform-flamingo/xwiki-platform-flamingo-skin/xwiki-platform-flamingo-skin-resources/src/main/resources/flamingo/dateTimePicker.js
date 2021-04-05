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
  'moment-jdateformatparser': $services.webjars.url('moment-jdateformatparser', 'moment-jdateformatparser.min'),
  'bootstrap-datetimepicker': $services.webjars.url('Eonasdan-bootstrap-datetimepicker',
    'js/bootstrap-datetimepicker.min')
})
#[[*/
// Start JavaScript-only code.
(function(paths) {
  "use strict";

  // We have to declare momentjs as a RequireJS package in order to be able to load momentjs locales on demand using
  // RequireJS. See https://github.com/requirejs/requirejs/issues/1554 .
  var momentRelativePath = 'min/moment.min';
  // The base path shouldn't end with the path separator (slash).
  var momentBasePath = paths.moment.substring(0, paths.moment.length - (momentRelativePath.length + 1));
  // We don't need the path config anymore if we define the package.
  delete paths.moment;
  require.config({
    packages: [{
      name: 'moment',
      location: momentBasePath,
      main: momentRelativePath
    }],
    paths: paths,
    map: {
      '*': {
        // momentjs locales depend on '../moment' which gets resolved as 'moment/moment' due to our package
        // configuration, which points to the unminified version. The consequence is that we end up loading both the
        // minified and the unminified version of momentjs and, more importantly, the locales are loaded into the moment
        // instance created by the unminified code. In order to fix this we map the unminified version to the minified
        // version so that we work with a single moment instance (that has the locales loaded).
        'moment/moment': 'moment'
      }
    }
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
        maybeLoadLocale(dateTimeInput.data('locale')).then($.proxy(createDateTimePicker, null, dateTimeInput));
      });
    };

    var maybeLoadLocale = function(locale) {
      var deferred = $.Deferred();
      // Convert the Java locale name to the moment locale name (en_US -> en-us).
      var momentLocale = locale.toLowerCase().replace(/_/g, '-');
      var resolve = function() {
        deferred.resolve();
      };
      // Check if the locale is already loaded.
      if (moment.locales().indexOf(momentLocale) < 0) {
        require(['moment/locale/' + momentLocale], resolve, resolve);
      } else {
        resolve();
      }
      return deferred.promise();
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
