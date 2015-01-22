define(['jquery', 'jsTree', 'xwiki-events-bridge'], function($) {
  'use strict';

  // Default configuration options.
	$.jstree.defaults.finder = {
    url: '',
    placeholder: 'find ...',
    suggestion: {
      hint: 'hint',
      info: 'info'
    }
	};

  var createSuggestInput = function(options) {
    var input = document.createElement('input');
    input.type = 'text';
    input.className = 'xtree-finder';
    input.placeholder = options.finder.placeholder;

    new XWiki.widgets.Suggest(input, {
      align: 'auto',
      className: 'xtree-finder-suggestions',
      displayValue: true,
      displayValueText: '',
      fadeOnClear: false,
      hideButton: false,
      json: true,
      minchars: 3,
      resultHint: options.finder.suggestion.hint,
      resultInfo: options.finder.suggestion.info,
      resultValue: 'text',
      script: options.finder.url,
      shownoresults: false,
      timeout: 0,
      varname: 'query'
    });

    return input;
  };

  var findNode = function(event, data) {
    this.deselect_all();
    this.close_all();
    this.openTo(data.id);
  };

  // Move the icon inside the value container because it's easier to style it like that.
  $(document).on('xwiki:suggest:updated', function(event, data) {
    if ($(data.container).hasClass('xtree-finder-suggestions')) {
      $(data.container).find('.icon').each(function() {
        $(this).next('.value').prepend(this);
      });
    }
  });

	$.jstree.plugins.finder = function (options, parent) {
		this.init = function (element, options) {
			parent.init.call(this, element, options);

      $(createSuggestInput(options)).insertBefore(element).on('xwiki:suggest:selected', $.proxy(findNode, this));
		};
	};
});

