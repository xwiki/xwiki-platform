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

  editMacroWizard = function(macroCall, syntaxId) {
    return editMacro({
      macroCall: macroCall,
      syntaxId: syntaxId
    }).then(selectOtherMacroOrFinish);
  };

  return function(macroCall, syntaxId) {
    if (typeof macroCall === 'object' && macroCall !== null &&
        typeof macroCall.name === 'string' && macroCall.name !== '') {
      macroCall.parameters = macroCall.parameters || {};
      syntaxId = (typeof syntaxId === 'string' && syntaxId) || XWiki.docsyntax;
      return editMacroWizard(macroCall, syntaxId);
    } else {
      // The macro wizard can be called passing just the syntax as the first parameter.
      syntaxId = (typeof macroCall === 'string' && macroCall) ||
        (typeof syntaxId === 'string' && syntaxId) || XWiki.docsyntax;
      var data = {syntaxId: syntaxId};
      if (typeof macroCall === 'object' && macroCall !== null) {
        // We can pass default macro parameter values to the Insert Macro Wizard.
        data.macroCall = macroCall;
      }
      return insertMacroWizard(data);
    }
  };
});
