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

  const macroDescriptors = {};

  function getMacroDescriptor(macroId, maybeSourceDocumentReference) {
    const deferred = $.Deferred();
    const macroDescriptor = macroDescriptors[macroId];
    if (macroDescriptor) {
      deferred.resolve(macroDescriptor);
    } else {
      const sourceDocumentReference = maybeSourceDocumentReference || XWiki.currentDocument.documentReference;
      const url = new XWiki.Document(sourceDocumentReference).getURL('get', $.param({
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
          deferred.reject(...arguments);
        }
      }).fail(function () {
        deferred.reject(...arguments);
      });
    }
    return deferred.promise();
  }

  let getMacroParametersFromHTML = function (
    macroId,
    parameters = {},
    sourceDocumentReference = XWiki.currentDocument.documentReference
  ) {
    let deferred = $.Deferred();
    if (!Object.keys(parameters).length) {
      return deferred.resolve(parameters);
    } else {
      let url = new XWiki.Document(sourceDocumentReference).getURL('get', $.param({
        language: $('html').attr('lang'),
        sheet: 'CKEditor.MacroService',
      }));
      $.post(url, {
        data: 'macroParameters',
        macroId,
        macroParameters: JSON.stringify(parameters),
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

  const macrosBySyntax = {};

  function getMacros(syntaxId, force) {
    const deferred = $.Deferred();
    const macros = macrosBySyntax[syntaxId || ''];
    if (macros && !force) {
      deferred.resolve(macros);
    } else {
      const url = new XWiki.Document('MacroService', 'CKEditor').getURL('get', $.param({
        outputSyntax: 'plain',
        language: $('html').attr('lang')
      }));
      $.get(url, {
        data: 'list',
        syntaxId: syntaxId
      }).done(function (macros) {
        // Bulletproofing: check if the returned data is json since it could some HTML representing an error
        if (typeof macros === 'object' && Array.isArray(macros.list)) {
          let macroList = macros.list;
          if (Array.isArray(macros.notinstalled)) {
            macroList = macroList.concat(macros.notinstalled);
          }
          macrosBySyntax[syntaxId || ''] = macroList;
          deferred.resolve(macroList);
        } else {
          deferred.reject(...arguments);
        }
      }).fail(function () {
        deferred.reject(...arguments);
      });
    }
    return deferred.promise();
  }

  function installMacro(extensionId, extensionVersion) {
    const url = new XWiki.Document('MacroService', 'CKEditor').getURL('get', $.param({
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
  }

  return {
    getMacroDescriptor,
    installMacro,
    getMacros,
    getMacroParametersFromHTML
  };
});
