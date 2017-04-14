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
define('l10n', ['module', 'jquery'], function(module, $) {
  var getTranslations = function(prefix, keys) {
    var params = $.param({prefix: prefix, key: keys}, true);
    var config = module.config();
    var limit = config.getParamsLimit || 1500;
    return (params.length > limit ? $.post : $.get)(config.url, params);
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
    parentRequire([name + 'TranslationKeys'], function(keys) {
      if (module.config().url) {
        getTranslations(name, keys).done(function(translations) {
          translations.get = getTranslation;
          onLoad(translations);
        }).fail(function() {
          onLoad(emptyResourceBundle);
        });
      } else {
        onLoad(emptyResourceBundle);
      }
    });
  };

  return {
    load: load
  };
});
