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
    'xwiki-selectize': "$xwiki.getSkinFile('uicomponents/suggest/xwiki.selectize.js', true)" +
      "?v=$escapetool.url($xwiki.version)"
  }
});

/**
 * Utils
 */
define('xwiki-selectize-utils', ['jquery'], function($) {
  'use strict';

  var maybeLoadMoreSuggestions = function(source, params) {
    return function(suggestions) {
      if (suggestions.length < params.limit) {
        return source.call(null, params).then(function(response) {
          // Success.
          if ($.isArray(response)) {
            suggestions.push.apply(suggestions, response.slice(0, params.limit - suggestions.length));
            suggestions.sort(suggestionComparator);
          }
          return suggestions;
        }, function() {
          // Failure.
          return $.Deferred().resolve(suggestions);
        });
      } else {
        return suggestions;
      }
    }
  };

  var suggestionComparator = function(alice, bob) {
    return alice.label.localeCompare(bob.label);
  };

  return {
    loadSuggestions: function(sources, params) {
      return sources.reduce(function(promise, source) {
        return promise.then(maybeLoadMoreSuggestions(source, params));
      }, $.Deferred().resolve([]));
    }
  };
});

/**
 * Users
 */
define('xwiki-suggestUsers', ['jquery', 'xwiki-selectize-utils', 'xwiki-selectize'], function($, utils) {
  'use strict';

  var getSelectizeOptions = function(select) {
    return {
      create: true,
      load: function(text, callback) {
        loadUsers(select.attr('data-userScope'), {
          'input': text,
          'limit': 10
        }).done(callback).fail(callback);
      }
    };
  };

  var loadUsers = function(userScope, params) {
    if (userScope === 'LOCAL_AND_GLOBAL') {
      return getLocalAndGlobalUsers(params);
    } else if (userScope === 'GLOBAL_ONLY') {
      return getGlobalUsers(params);
    } else {
      return getUsers(params);
    }
  };

  var getLocalAndGlobalUsers = function(params) {
    return utils.loadSuggestions([getUsers, getGlobalUsers], params);
  };

  var getGlobalUsers = function(params) {
    return getUsers($.extend(params, {'wiki': 'global'}));
  };

  var getUsers = function(params) {
    return $.getJSON(XWiki.currentDocument.getURL('get'), $.extend(params, {
      'xpage': 'uorgsuggest',
      'media': 'json',
      'uorg': 'user'
    }));
  };

  $.fn.suggestUsers = function(options) {
    return this.each(function() {
      $(this).xwikiSelectize($.extend(getSelectizeOptions($(this)), options));
    });
  };
});

/**
 * Groups
 */
define('xwiki-suggestGroups', ['jquery', 'xwiki-selectize-utils', 'xwiki-selectize'], function($, utils) {
  'use strict';

  var getSelectizeOptions = function(select) {
    return {
      create: true,
      load: function(text, callback) {
        loadGroups(select.attr('data-userScope'), {
          'input': text,
          'limit': 10
        }).done(callback).fail(callback);
      }
    };
  };

  var loadGroups = function(userScope, params) {
    if (XWiki.currentWiki !== XWiki.mainWiki && userScope !== 'LOCAL_ONLY') {
      // We can have both local and global groups in a subwiki that accepts global users. In case the wiki accepts only
      // global users, local groups can be used to setup various rights for the global users.
      return getLocalAndGlobalGroups(params);
    } else {
      return getGroups(params);
    }
  };

  var getLocalAndGlobalGroups = function(params) {
    return utils.loadSuggestions([getGroups, getGlobalGroups], params);
  };

  var getGlobalGroups = function(params) {
    return getGroups($.extend(params, {'wiki': 'global'}));
  };

  var getGroups = function(params) {
    return $.getJSON(XWiki.currentDocument.getURL('get'), $.extend(params, {
      'xpage': 'uorgsuggest',
      'media': 'json',
      'uorg': 'group'
    }));
  };

  $.fn.suggestGroups = function(options) {
    return this.each(function() {
      $(this).xwikiSelectize($.extend(getSelectizeOptions($(this)), options));
    });
  };
});

require(['jquery', 'xwiki-suggestUsers', 'xwiki-suggestGroups', 'xwiki-events-bridge'], function($) {
  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.suggest-users').suggestUsers();
    container.find('.suggest-groups').suggestGroups();
  };

  // We listen to xwiki:dom:loaded because the old lightbox widget that is still used in the administration triggers it
  // instead of xwiki:dom:updated.
  $(document).on('xwiki:dom:loaded xwiki:dom:updated', init);
  XWiki.domIsLoaded && init();
});
