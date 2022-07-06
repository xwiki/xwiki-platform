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
define('macroWizard', ['macroSelector', 'macroEditor'], function(selectMacro, editMacro) {
  'use strict';
  var selectOtherMacroOrFinish = function(data) {
    if (data.action === 'changeMacro') {
      delete data.action;
      data.macroId = data.macroCall && data.macroCall.name;
      return insertMacroWizard(data);
    } else {
      return data.macroCall;
    }
  },

  insertMacroWizard = function(data) {
    return selectMacro(data).then(editMacro).then(selectOtherMacroOrFinish);
  },

  editMacroWizard = function(data) {
    return editMacro(data).then(selectOtherMacroOrFinish);
  },

  isMacroCall = function(input) {
    return typeof input === 'object' && input !== null &&
      ((typeof input.name === 'string' && input.name !== '') ||
      (typeof input.parameters === 'object' && input.parameters !== null) ||
      typeof input.content === 'string');
  };

  /**
   * The macro wizard can be called with (macroCall), (macroCall, syntaxId), (syntaxId) or (options) where options is an
   * object that looks like: {macroCall: ..., syntaxId: ..., ...}.
   */
  return function() {
    // Process the function arguments.
    var data = {}, index = 0;
    if (typeof arguments[index] === 'object' && arguments[index] !== null) {
      if (isMacroCall(arguments[index])) {
        data.macroCall = arguments[index++];
      } else {
        data = arguments[index];
      }
    }
    if (data.macroCall) {
      data.macroCall.parameters = data.macroCall.parameters || {};
    }
    if (typeof arguments[index] === 'string') {
      data.syntaxId = arguments[index];
    }
    if (typeof data.syntaxId !== 'string' || data.syntaxId === '') {
      data.syntaxId = XWiki.docsyntax;
    }
    // Edit the macro call if the macro name is specified. Otherwise insert a new macro.
    if (typeof data.macroCall === 'object' && data.macroCall !== null &&
        typeof data.macroCall.name === 'string' && data.macroCall.name !== '') {
      return editMacroWizard(data);
    } else {
      return insertMacroWizard(data);
    }
  };
});
