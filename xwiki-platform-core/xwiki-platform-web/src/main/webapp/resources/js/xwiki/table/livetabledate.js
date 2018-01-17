require.config({
  paths: {
    'moment': "$services.webjars.url('momentjs', 'moment.js')",
    'jdateformatparser': "$services.webjars.url('org.webjars.bower:moment-jdateformatparser', 'moment-jdateformatparser.js')",
    'daterangepicker': "$services.webjars.url('bootstrap-daterangepicker', 'js/bootstrap-daterangepicker.js')"
  }
});

require(['jquery', 'jdateformatparser', 'daterangepicker', 'xwiki-events-bridge'],
  function($) {
    var bindInputs = function(livetable, moment) {
      $(livetable).find('input[data-type="date"]').each(function(i, element) {
        var input = $(element);
        var hidden = input.prev('input[type="hidden"]');
        var dateFormat = moment().toMomentFormatString(input.attr('data-dateformat'));

        input.daterangepicker({
          drops: 'down',
          opens: 'center',
          autoUpdateInput: false,
          timePicker: true,
          timePicker24Hour: true,
          ranges: {
            '$escapetool.javascript($services.localization.render("daterange.today"))':
              [moment().startOf('day'), moment().endOf('day')],
            '$escapetool.javascript($services.localization.render("daterange.yesterday"))':
              [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')],
            '$escapetool.javascript($services.localization.render("daterange.lastSevenDays"))':
              [moment().subtract(6, 'days').startOf('day'), moment().endOf('day')],
            '$escapetool.javascript($services.localization.render("daterange.lastThirtyDays"))':
              [moment().subtract(29, 'days').startOf('day'), moment().endOf('day')],
            '$escapetool.javascript($services.localization.render("daterange.thisMonth"))':
              [moment().startOf('month'), moment().endOf('month')],
            '$escapetool.javascript($services.localization.render("daterange.lastMonth"))':
              [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')]
          },
          locale: {
            format: dateFormat,
            cancelLabel: '$escapetool.javascript($services.localization.render("daterange.clear"))',
            applyLabel: '$escapetool.javascript($services.localization.render("daterange.apply"))',
            customRangeLabel: '$escapetool.javascript($services.localization.render("daterange.customRange"))',
            fromLabel: '$escapetool.javascript($services.localization.render("daterange.from"))',
            toLabel: '$escapetool.javascript($services.localization.render("daterange.to"))',
          }
        });

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
            hidden.val(picker.startDate.format('x') + '-' + picker.endDate.format('x'));
          }
          $(document).trigger("xwiki:livetable:" + $(livetable).attr('id') + ":filtersChanged");
        }

        input.on('apply.daterangepicker cancel.daterangepicker', function(event, picker) {
          updateInput(this, event, picker);
        });

        input.on('keypress', function (e) {
          if(e.which === 13) {
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

        input.on('hide.daterangepicker', function(event) {
          // Overwrite at instance level the 'hide' function added by Prototype.js to the Element prototype.
          // This removes the 'hide' function only for the event target.
          this.hide = undefined;
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

    // jdateformatparser enhances moment using a require callback so we need to wait for it (because when you require a
    // module the callback is not called immediately even if the module is already loaded; the callback is called on the
    // next processor cycle).
    // See XWIKI-14579: The picker to filter on Date types, in LiveTables, doesn't appear on IE 11 and Microsoft Edge 40
    // See https://github.com/MadMG/moment-jdateformatparser/issues/20 (Loading with Require.js doesn't always work as
    // expected)
    require(['moment'], function(moment) {
      $('.xwiki-livetable').each(function(i, livetable) {
        bindInputs(livetable, moment);
      });
    });
  }
);
