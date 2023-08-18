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
  'daterangepicker': $services.webjars.url('bootstrap-daterangepicker', 'js/bootstrap-daterangepicker.js')
})
#set ($l10nKeys = ['today', 'yesterday', 'lastSevenDays', 'lastThirtyDays', 'thisMonth', 'lastMonth', 'clear', 'apply',
  'customRange', 'from', 'to'])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($discard = $l10n.put($key, $services.localization.render("daterange.$key")))
#end
#[[*/
// Start JavaScript-only code.
(function(paths, l10n) {
  "use strict";

  require.config({paths});

  require([
    'jquery',
    'moment',
    'moment-jdateformatparser',
    'daterangepicker',
    'xwiki-events-bridge'
  ], function($, moment) {
    var bindInputs = function(livetable) {
      $(livetable).find('input[data-type="date"]').each(function(i, element) {
        var input = $(element);
        var hidden = input.parent().prevAll('input[type="hidden"]');
        var dateFormat = moment().toMomentFormatString(input.attr('data-dateformat'));

        input.daterangepicker({
          drops: 'down',
          opens: 'center',
          autoUpdateInput: false,
          timePicker: true,
          timePicker24Hour: true,
          ranges: {
            [l10n.today]: [moment().startOf('day'), moment().endOf('day')],
            [l10n.yesterday]: [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')],
            [l10n.lastSevenDays]: [moment().subtract(6, 'days').startOf('day'), moment().endOf('day')],
            [l10n.lastThirtyDays]: [moment().subtract(29, 'days').startOf('day'), moment().endOf('day')],
            [l10n.thisMonth]: [moment().startOf('month'), moment().endOf('month')],
            [l10n.lastMonth]: [moment().subtract(1, 'month').startOf('month'),
              moment().subtract(1, 'month').endOf('month')]
          },
          locale: {
            format: dateFormat,
            cancelLabel: l10n.clear,
            applyLabel: l10n.apply,
            customRangeLabel: l10n.customRange,
            fromLabel: l10n.from,
            toLabel: l10n.to
          }
        });

        // Connect the input to the picker via the aria-controls attribute.
        const pickerId = input.attr('id') + '-picker';
        input.data('daterangepicker').container.attr("id", pickerId);
        input.parent().attr('aria-controls', pickerId);

        var updateInput = function(element, event, picker) {
          if (event.type == 'cancel') {
            input.val('');
            hidden.val('');
          } else {
            if (picker.startDate.isSame(picker.endDate, 'day')) {
              input.val(picker.startDate.format(dateFormat));
            } else {
              input.val(picker.startDate.format(dateFormat) + ' - ' + picker.endDate.format(dateFormat));
            }
            // Make sure that the end time is the end of the minute.
            hidden.val(picker.startDate.format('x') + '-' + picker.endDate.seconds(59).milliseconds(999).format('x'));
          }
          $(document).trigger("xwiki:livetable:" + $(livetable).attr('id') + ":filtersChanged");
        }

        input.on('apply.daterangepicker cancel.daterangepicker', function(event, picker) {
          updateInput(this, event, picker);
        });

        input.on('keypress', function (e) {
          if(e.which === 13) {
            // when the enter key is pressed.
            var txt = input.val().trim();
            var range = txt.split(' - ');
            if (range.length === 2) {
              //range
              hidden.val(moment(range[0], dateFormat).format('x') + '-' + moment(range[1], dateFormat).format('x'));
            } else {
              //date or part of date
              hidden.val(txt);
            }
          }
        });

        input.on('show.daterangepicker', function(event) {
          event.target.parentElement.setAttribute('aria-expanded', true);
        });

        input.on('hide.daterangepicker', function(event) {
          // Overwrite at instance level the 'hide' function added by Prototype.js to the Element prototype.
          // This removes the 'hide' function only for the event target.
          event.target.hide = undefined;
          event.target.parentElement.setAttribute('aria-expanded', false);
          // Restore the 'hide' function after the event is handled (i.e. after all the listeners have been called).
          setTimeout(function () {
            // This deletes the local 'hide' key from the instance, making the 'hide' inherited from the prototype
            // visible again (the next calls to 'hide' won't find the key on the instance and thus it will go up
            // the prototype chain).
            delete event.target['hide'];
          }, 0);
        }); 
      });
    };

    $('.xwiki-livetable').each(function(i, livetable) {
      bindInputs(livetable);
    });
  });

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths, $l10n]));
