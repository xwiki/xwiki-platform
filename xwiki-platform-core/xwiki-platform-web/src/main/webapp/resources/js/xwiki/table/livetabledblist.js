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

require.config({
  paths: {
    'typeahead': "$services.webjars.url('Bootstrap-3-Typeahead', 'bootstrap3-typeahead.min.js')"
  }
});

require(['jquery', 'xwiki-meta', 'typeahead', 'xwiki-events-bridge'],
  function($, xm) {
    var bindInputs = function(livetable) {
      $(livetable).find('input[data-type="suggest"]').each(function(i, element) {
        var input = $(element);
        var hidden = input.prev('input[type="hidden"]');
        var limit = parseInt(input.attr('data-limit'), 10); // 10 is the radix, not a fallback
        input.typeahead({
          source: function(query, callback) {
            $.getJSON("$xwiki.getURL('XWiki.LiveTableDBListFilterService', 'get')", {
                'outputSyntax': 'plain',
                'class': input.attr('data-xclass'),
                'property': hidden.attr('name'),
                'input': query,
                'limit': limit
            }, callback);
          },
          items: limit,
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
