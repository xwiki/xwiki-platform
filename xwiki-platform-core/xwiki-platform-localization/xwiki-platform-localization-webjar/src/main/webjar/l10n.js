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
  config: {
    'xwiki-l10n': {
      url: `${XWiki.contextPath}/rest/wikis/${encodeURIComponent(XWiki.currentWiki)}/localization/translations`
    }
  }
});

define('xwiki-l10n', ['module', 'jquery'], function(module, $) {
  'use strict';

  var getTranslations = function(specs) {
    return Promise.resolve($.getJSON(module.config().url, $.param({
      // It's good to specify the locale when getting the translation messages because the current locale can change
      // between the moment the page is loaded and the moment the translation messages are requested (e.g. if the user
      // opens another page in another browser tab with a different locale specified). We take the current locale from
      // the page HTML, rather than using Velocity, in order to avoid having the current locale cached.
      locale: specs.locale || document.documentElement.getAttribute('lang'),
      prefix: specs.prefix,
      key: specs.keys
    }, true))).then(toTranslationsMap.bind(null, specs.prefix || ''));
  };

  var toTranslationsMap = function(prefix, responseJSON) {
    var translationsMap = {};
    responseJSON.translations?.forEach(
      // Remove the prefix when adding the translation key to the translations map.
      translation => translationsMap[translation.key.substring(prefix.length)] = translation.rawSource
    );
    return translationsMap;
  };

  var getTranslation = function(key) {
    var translation = this[key];
    if (typeof translation === 'string') {
      // Naive implementation for message parameter substitution that suits our current needs.
      for (var i = 1; i < arguments.length; i++) {
        translation = translation.replace(new RegExp('\\{' + (i - 1) + '\\}', 'g'), arguments[i]);
      }
    } else {
      translation = key;
    }
    return translation;
  };

  var emptyResourceBundle = {
    get: getTranslation
  };

  var load = function(name, parentRequire, onLoad, config) {
    parentRequire([name], function(specs) {
      if (module.config().url) {
        getTranslations(specs).then(translations => {
          translations.get = getTranslation;
          onLoad(translations);
        }).catch(() => {
          onLoad(emptyResourceBundle);
        });
      } else {
        onLoad(emptyResourceBundle);
      }
    });
  };

  return {load};
});
