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
define(['jquery', 'jsTree', 'xwiki-events-bridge'], function($) {
  'use strict';

  // jsTree uses the underscore notation for its API, instead of camel case.
  // jshint camelcase:false

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

  // We want to still activate the links with a click even after they are selected from the finder.
  $.jstree.defaults.core.allow_reselect = true;

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
      resultURL: 'a_attr.href',
      script: options.finder.url,
      timeout: 0,
      varname: 'query'
    });

    return input;
  };

  var findNode = function(event, data) {
    this.deselect_all();
    this.close_all();
    this.openTo(data.id, node => {
      if (this.select_node(node) !== false) {
        // Scroll only the node label into view because the entire node may take a lot of vertical space due to its
        // descendants (when the node is expanded).
        this.get_node(node, true).children('.jstree-anchor')[0].scrollIntoView(false);
      }
    });
  };

  // Move the icon inside the value container because it's easier to style it like that.
  $(document).on('xwiki:suggest:updated', function(event, data) {
    if ($(data.container).hasClass('xtree-finder-suggestions')) {
      $(data.container).find('.icon').each(function() {
        $(this).next('.value').prepend(this);
      });
    }
  });

  $(document).on('click.xtreeFinderSuggestion', '.xtree-finder-suggestions .suggestItem', function(event) {
    // Don't follow the link when selecting a suggestion because we want to open the tree to that location.
    event.preventDefault();
  });

  $.jstree.plugins.finder = function (options, parent) {
    this.init = function (element, options) {
      parent.init.call(this, element, options);

      $(createSuggestInput(options)).insertBefore(element).on('xwiki:suggest:selected', findNode.bind(this));
    };
  };
});
