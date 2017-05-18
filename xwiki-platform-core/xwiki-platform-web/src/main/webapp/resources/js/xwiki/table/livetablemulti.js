require.config({
    paths: {
        'bootstrap-select': "$services.webjars.url('bootstrap-select', 'js/bootstrap-select.js')"
    },
    shim: {
        'bootstrap-select': ['jquery', 'bootstrap']
    }
});

require(['jquery', 'bootstrap-select'],
function($) {
    $('.xwiki-livetable select.xwiki-livetable-multilist').each(function(i, element) {
        $(element).selectpicker({
            width: '100%',
            container: 'body',
            noneSelectedText: '', // empty to imitate other inputs in livetable
        });
    });
});
