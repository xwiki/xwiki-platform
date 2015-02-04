define(['jquery', 'jsTree', 'xwiki-events-bridge'], function($) {
  'use strict';

  // Default configuration options.
  $.jstree.defaults.finder = {
    url: '',
    placeholder: 'find ...',
    suggestion: {
      hint: 'data.hint',
      info: 'data.info',
      type: 'data.type'
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
      resultType: options.finder.suggestion.type,
      resultValue: 'text',
      script: options.finder.url,
      timeout: 0,
      varname: 'query'
    });

    return input;
  };

  var findNode = function(event, data) {
    this.deselect_all();
    this.close_all();
    this.openTo(data.id, $.proxy(function(node) {
      if (this.select_node(node) !== false) {
        // Scroll only the node label into view because the entire node may take a lot of vertical space due to its
        // descendants (when the node is expanded).
        this.get_node(node, true).children('.jstree-anchor')[0].scrollIntoView(false);
      }
    }, this));
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
