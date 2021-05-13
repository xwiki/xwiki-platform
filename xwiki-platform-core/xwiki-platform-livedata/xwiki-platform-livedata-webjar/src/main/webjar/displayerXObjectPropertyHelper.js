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

define('xwiki-livedata-xObjectPropertyHelper', ['jquery', 'xwiki-meta', 'xwiki-entityReference'], function($, xcontext, XWiki) {
  'use strict';

  /**
   * Pass the vue-i18n helper to the module to allow error messages to be localized.
   * @param $t the vue-i18n localization helper
   */
  function setLocalization($t) {
    this.$t = $t;
  }
  
  /**
   * Resolve the url of the document reference in the given mode.
   * @param documentReference the document reference
   * @param mode the mode
   * @returns {*} the computed relative url
   */
  function computeTargetURL(documentReference, mode) {
    return new XWiki.Document(XWiki.Model.resolve(documentReference, XWiki.EntityType.DOCUMENT)).getURL(mode);
  }

  function computeProperty(property, className) {
    const noDocPrefixProperty = property.replace('doc.', '');
    return className + '[0].' + noDocPrefixProperty;
  }

  // TODO: fix duplicate.
  function loadRequiredSkinExtensions(requiredSkinExtensions) {
    var existingSkinExtensions;
    var getExistingSkinExtensions = function() {
      return $('link, script').map(function() {
        return $(this).attr('href') || $(this).attr('src');
      }).get();
    };
    $('<div/>').html(requiredSkinExtensions).find('link, script').filter(function() {
      if (!existingSkinExtensions) {
        existingSkinExtensions = getExistingSkinExtensions();
      }
      var url = $(this).attr('href') || $(this).attr('src');
      return existingSkinExtensions.indexOf(url) < 0;
    }).appendTo('head');
  }
  
  function load(mode, documentReference, property, className) {
    const targetUrl = computeTargetURL(documentReference, 'get');
    const computedProperty = computeProperty(property, className);
    return new Promise((resolve, reject) => {
      $.get(targetUrl, {
        xpage: 'display',
        mode: mode,
        // TODO: handle the object index when provided
        property: computedProperty,
        type: property.startsWith('doc.') ? 'document' : 'object',
        language: xcontext.locale
      }).done((html, textStatus, jqXHR) => {
        loadRequiredSkinExtensions(jqXHR.getResponseHeader('X-XWIKI-HTML-HEAD'));
        // Update the viewer.
        resolve(html);
      }).fail(() => {
        // TODO: translate
        new XWiki.widgets.Notification(
          this.$t('livedata.displayer.xObjectProperty.failedToRetrieveField.errorMessage', {mode}), 'error');
        reject();
      })
    });
  }

  /**
   * Load an XObject property field in edit mode.
   * @param documentReference the reference of the document containing the XObject
   * @param className the class name of the XObject
   * @param property the XObject property to display
   * @returns {*} the XObject property field html content in edit mode
   */
  function edit(documentReference, className, property) {
    return load('edit', documentReference, property, className);
  }

  /**
   * Load an XObject property field in view mode.
   * @param documentReference the reference of the document containing the XObject
   * @param className the class name of the XObject
   * @param property the XObject property to display
   * @returns {*} the XObject property field html content in view mode
   */
  function view(documentReference, className, property) {
    return load('view', documentReference, property, className);
  }

  return {edit, view, setLocalization};
});