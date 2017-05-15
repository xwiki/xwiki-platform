require.config({
    paths: {
        moment: "$services.webjars.url('momentjs', 'moment.js')",
        daterangepicker: "$services.webjars.url('bootstrap-daterangepicker', 'js/bootstrap-daterangepicker.js')"
    },
    shim: {
        'daterangepicker': ['jquery', 'bootstrap', 'moment']
    }
});

require(['jquery', 'moment', 'daterangepicker', 'xwiki-events-bridge'],
function($, moment) {

    // Load bootstrap-daterangepicker CSS only when needed
    // This is a bit of a hack, but it's required until we have an API to
    // include CSS from a webjar in Velocity.
    $('head').append('<link rel="stylesheet" type="text/css" href="$services.webjars.url('bootstrap-daterangepicker', 'css/bootstrap-daterangepicker.css')">');

    var bindInputs = function(id) {
        var input = $('#' + id + ' input[data-type="date"]');
        var hidden = $('#' + id + ' input[type="hidden"]');

        input.daterangepicker({
            drops: 'down',
            opens: 'center',
            autoUpdateInput: false,
            timePicker: true,
            timePicker24Hour: true,
            ranges: {
                '$escapetool.javascript($services.localization.render("daterange.today"))': [moment().startOf('day'), moment().endOf('day')],
                '$escapetool.javascript($services.localization.render("daterange.yesterday"))': [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')],
                '$escapetool.javascript($services.localization.render("daterange.lastSevenDays"))': [moment().subtract(6, 'days').startOf('day'), moment().endOf('day')],
                '$escapetool.javascript($services.localization.render("daterange.lastThirtyDays"))': [moment().subtract(29, 'days').startOf('day'), moment().endOf('day')],
                '$escapetool.javascript($services.localization.render("daterange.thisMonth"))': [moment().startOf('month'), moment().endOf('month')],
                '$escapetool.javascript($services.localization.render("daterange.lastMonth"))': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')]
            },
            locale: {
                cancelLabel: '$escapetool.javascript($services.localization.render("daterange.clear"))',
                applyLabel: '$escapetool.javascript($services.localization.render("daterange.apply"))'
            }
        });

        var updateInput = function(elem, ev, picker) {
            if (ev.type == 'cancel')
            {
                input.val('');
                hidden.val('');
            } else {
                if (picker.startDate.isSame(picker.endDate, 'day'))
                    input.val(picker.startDate.format('DD/MM/YYYY'));
                else
                    input.val(picker.startDate.format('DD/MM/YYYY') + ' - ' + picker.endDate.format('DD/MM/YYYY'));
                hidden.val(picker.startDate.format('x') + '-' + picker.endDate.format('x'));
            }
            if ('dispatchEvent' in elem)
                elem.dispatchEvent(new Event('change'));
            else
                elem.fireEvent('onchange');
        }

        input.on('apply.daterangepicker cancel.daterangepicker', function(ev, picker) {
            updateInput(this, ev, picker);
        });
    };

    $('.xwiki-livetable').each(function(i, elem) {
        bindInputs($(elem).attr('id'));
    });

});
