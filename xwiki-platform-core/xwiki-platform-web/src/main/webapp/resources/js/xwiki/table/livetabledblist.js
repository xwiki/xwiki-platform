require.config({
  paths: {
    'typeahead': "$services.webjars.url('Bootstrap-3-Typeahead', 'bootstrap3-typeahead.min.js')"
  },
  shim: {
    'typeahead': ['jquery', 'bootstrap']
  }
});

require(['jquery', 'xwiki-meta', 'typeahead', 'xwiki-events-bridge'],
  function($, xm) {
    var bindInputs = function(livetable) {
      $(livetable).find('input[data-type="suggest"]').each(function(i, element) {
        var input = $(element);
        var hidden = input.prev('input[type="hidden"]');
        input.typeahead({
          source: function(query, callback) {
            $.getJSON("$xwiki.getURL('XWiki.LiveTableDBListFilterService', 'get')", {
                'outputSyntax': 'plain',
                'class': input.attr('data-xclass'),
                'prop': hidden.attr('name'),
                'input': query,
                'limit': 10
            }, callback);
          },
          items: 10,
          fitToElement: true,
          afterSelect: function(item) {
            hidden.val(item.id);
            $(document).trigger("xwiki:livetable:" + $(livetable).attr('id') + ":filtersChanged")
          }
        });
        input.on('input', function() {
            if(input.val() == '') {
                hidden.val('');
                $(document).trigger("xwiki:livetable:" + $(livetable).attr('id') + ":filtersChanged")
            }
        });
      });
    };

    $('.xwiki-livetable').each(function(i, element) {
      bindInputs(element);
    });
  }
);
