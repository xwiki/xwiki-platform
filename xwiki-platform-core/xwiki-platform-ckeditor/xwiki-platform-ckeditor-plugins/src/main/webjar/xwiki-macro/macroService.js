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

/**
 * Macro Service
 */
define('macroService', ['jquery', 'xwiki-meta'], function ($, xcontext) {
  'use strict';

  var macroDescriptors = {};

  var getMacroDescriptor = function (macroId, maybeSourceDocumentReference) {
    var deferred = $.Deferred();
    var macroDescriptor = macroDescriptors[macroId];
    if (macroDescriptor) {
      deferred.resolve(macroDescriptor);
    } else {
      var sourceDocumentReference = maybeSourceDocumentReference || XWiki.currentDocument.documentReference;
      var url = new XWiki.Document(sourceDocumentReference).getURL('get', $.param({
        outputSyntax: 'plain',
        language: $('html').attr('lang'),
        sheet: 'CKEditor.MacroService'
      }));
      $.get(url, {
        data: 'descriptor',
        macroId: macroId
      }).done(function (macroDescriptor) {
        if (typeof macroDescriptor === 'object' && macroDescriptor !== null) {
          macroDescriptors[macroId] = macroDescriptor;
          deferred.resolve(macroDescriptor);
        } else {
          deferred.reject.apply(deferred, arguments);
        }
      }).fail(function () {
        deferred.reject.apply(deferred, arguments);
      });
    }
    return deferred.promise();
  };

  let getMacroParametersFromHTML = function (macroId, html) {
    let deferred = $.Deferred();
    if (html === "") {
      return deferred.resolve({});
    } else {
      let sourceDocumentReference = XWiki.currentDocument.documentReference;
      let url = new XWiki.Document(sourceDocumentReference).getURL('get', $.param({
        outputSyntax: 'plain',
        language: $('html').attr('lang'),
        sheet: 'CKEditor.MacroService',
      }));
      $.post(url, {
        data: 'macroParameters',
        macroId: macroId,
        macroHTML: html
      }).done(function (parameters) {
        if (typeof parameters === 'object' && parameters !== null) {
          deferred.resolve(parameters);
        } else {
          deferred.reject(...arguments);
        }
      }).fail(function (...args) {
        deferred.reject(...args);
      });
      return deferred.promise();
    }
  };

  var macrosBySyntax = {};

  var getMacros = function (syntaxId, force) {
    var deferred = $.Deferred();
    var macros = macrosBySyntax[syntaxId || ''];
    if (macros && !force) {
      deferred.resolve(macros);
    } else {
      var url = new XWiki.Document('MacroService', 'CKEditor').getURL('get', $.param({
        outputSyntax: 'plain',
        language: $('html').attr('lang')
      }));
      $.get(url, {
        data: 'list',
        syntaxId: syntaxId
      }).done(function (macros) {
        // Bulletproofing: check if the returned data is json since it could some HTML representing an error
        if (typeof macros === 'object' && Array.isArray(macros.list)) {
          var macroList = macros.list;
          if (Array.isArray(macros.notinstalled)) {
            macroList = macroList.concat(macros.notinstalled);
          }
          macrosBySyntax[syntaxId || ''] = macroList;
          deferred.resolve(macroList);
        } else {
          deferred.reject.apply(deferred, arguments);
        }
      }).fail(function () {
        deferred.reject.apply(deferred, arguments);
      });
    }
    return deferred.promise();
  };

  var installMacro = function (extensionId, extensionVersion) {
    var url = new XWiki.Document('MacroService', 'CKEditor').getURL('get', $.param({
      outputSyntax: 'plain',
      language: $('html').attr('lang')
    }));

    return $.post(url, {
      action: 'install',
      extensionId: extensionId,
      extensionVersion: extensionVersion,
      /*jshint camelcase: false */
      'form_token': xcontext.form_token
    });
  };

  return {
    getMacroDescriptor: getMacroDescriptor,
    installMacro: installMacro,
    getMacros: getMacros,
    getMacroParametersFromHTML: getMacroParametersFromHTML
  };
});
