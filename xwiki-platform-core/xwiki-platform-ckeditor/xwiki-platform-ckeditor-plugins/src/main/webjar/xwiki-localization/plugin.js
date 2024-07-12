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
(function() {
  'use strict';

  CKEDITOR.plugins.add('xwiki-localization', {
    beforeInit: function(editor) {
      // We don't use editor.lang.* directly because:
      // * it doesn't fail nicely when a translation is missing (e.g. fall-back on the translation key)
      // * it doesn't support parameter substitution
      editor.localization = {
        get: function(key) {
          return getTranslation.apply(editor, arguments);
        }
      };

      // Useful to know which document translation is currently being edited.
      editor.getContentLocale = () => {
        // Check if the content language is specified in the form containing this editor instance.
        // Note that the language field holds the real locale of the edited document.
        const languageField = editor.element?.$.closest('form, .form, body')?.querySelector('input[name="language"]');
        // Fallback on the locale of the current document, specified on the HTML element. Note that this is the raw
        // locale not the real locale, i.e. the value is empty for the default document translation.
        return languageField ? languageField.value : (document.documentElement.dataset.xwikiLocale || '');
      };
    }
  });

  var getTranslation = function(key) {
    var translation = getNestedProperty(this.lang, key);
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

  var getNestedProperty = function(object, path) {
    if (path && path.length > 0) {
      var dotIndex = path.indexOf('.');
      var left = path.substr(0, dotIndex);
      var right = path.substr(dotIndex + 1);
      if (object && object.hasOwnProperty(left)) {
        return getNestedProperty(object[left], right);
      } else if (object && object.hasOwnProperty(path)) {
        return object[path];
      } else {
        return undefined;
      }
    } else {
      return object;
    }
  };
})();
