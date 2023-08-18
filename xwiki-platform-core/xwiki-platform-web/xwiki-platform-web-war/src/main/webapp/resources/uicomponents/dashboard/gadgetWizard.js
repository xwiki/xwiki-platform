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
/*!
#set ($paths = {
  'css': {
    'macroWizard': $services.webjars.url('org.xwiki.platform:xwiki-platform-ckeditor-plugins',
      'xwiki-macro/macroWizard.min.css', {'evaluate': true})
  }
})
#set ($l10nKeys = [
  'dashboard.gadgetSelector.title',
  'dashboard.gadgetEditor.title',
  'dashboard.gadgetEditor.changeGadget.label',
  'dashboard.gadgetEditor.gadgetTitle.label',
  'dashboard.gadgetEditor.gadgetTitle.hint'
])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($discard = $l10n.put($key, $services.localization.render($key)))
#end
#[[*/
// Start JavaScript-only code.
(function(paths, l10n) {
  "use strict";

define(['jquery', 'xwiki-ckeditor'], function($, ckeditorPromise) {
  var gadgetTitleTemplate = $(
    '<li class="macro-parameter" data-id="$gadgetTitle">' +
      '<div class="macro-parameter-name"></div>' +
      '<div class="macro-parameter-description"></div>' +
      '<input type="text" class="macro-parameter-field" name="$gadgetTitle"/>' +
    '</li>'
  );
  gadgetTitleTemplate.find('.macro-parameter-name').text(l10n['dashboard.gadgetEditor.gadgetTitle.label']);
  gadgetTitleTemplate.find('.macro-parameter-description').text(l10n['dashboard.gadgetEditor.gadgetTitle.hint']);

  $('head').append($('<link type="text/css" rel="stylesheet"/>').attr('href', paths.css.macroWizard));

  var getMacroCall = function(gadget, ckeditor) {
    if (gadget && typeof gadget.content === 'string') {
      return ckeditor.plugins.registered['xwiki-macro'].parseMacroCall(gadget.content);
    }
  };

  var getMacroWizard = function(ckeditor) {
    return new Promise((resolve, reject) => {
      require(['macroWizard'], function(macroWizard) {
        resolve({ckeditor, macroWizard});
      });
    });
  };

  var getDefaultGadgetTitle = function(macroEditor) {
    var gadgetName = macroEditor.attr('data-macroid').split('/')[0];
    return "$services.localization.render('rendering.macro." + gadgetName + ".name')";
  };

  var currentGadget;

  // Customize the Macro Selector step when the Gadget Wizard is running.
  $(document).on('show.bs.modal', '.macro-selector-modal', function(event) {
    if (currentGadget && !$(this).hasClass('gadget-selector-modal')) {
      $(this).addClass('gadget-selector-modal');
      var modalTitleContainer = $(this).find('.modal-title');
      modalTitleContainer.prop('oldText', modalTitleContainer.text()).text(l10n['dashboard.gadgetSelector.title']);
    }
  });

  var restoreMacroSelector = function() {
    var macroSelectorModal = $('.gadget-selector-modal').removeClass('gadget-selector-modal');
    var modalTitleContainer = macroSelectorModal.find('.modal-title');
    modalTitleContainer.text(modalTitleContainer.prop('oldText'));
  };

  // Customize the Macro Editor step when the Gadget Wizard is running.
  var initialGadgetTitle;
  $(document).on('ready', '.macro-editor', function() {
    if (!currentGadget) {
      return;
    }
    var modal = $(this).closest('.modal');
    if (!modal.hasClass('gadget-editor-modal')) {
      initialGadgetTitle = currentGadget.title;
      // Customizations done everytime the Gadget Wizard is started.
      modal.addClass('gadget-editor-modal');
      var modalTitleContainer = modal.find('.modal-title');
      modalTitleContainer.prop('oldText', modalTitleContainer.text()).text(l10n['dashboard.gadgetEditor.title']);
      var changeMacroButton = modal.find('.modal-footer .btn-default').not('*[data-dismiss="modal"]');
      changeMacroButton.prop('oldText', changeMacroButton.text())
        .text(l10n['dashboard.gadgetEditor.changeGadget.label']);
    }
    // Customizations done everytime the Gadget Editor step is shown.
    var gadgetTitleContainer = gadgetTitleTemplate.clone();
    $(this).find('.macro-parameters').prepend(gadgetTitleContainer);
    var gadgetTitle = initialGadgetTitle || getDefaultGadgetTitle($(this));
    // Use the default gadget title next time (i.e. when the user changes the gadget).
    initialGadgetTitle = null;
    gadgetTitleContainer.find('input').val(gadgetTitle).focus();
  });

  var restoreMacroEditor = function() {
    var macroEditorModal = $('.gadget-editor-modal').removeClass('gadget-editor-modal');
    var modalTitleContainer = macroEditorModal.find('.modal-title');
    modalTitleContainer.text(modalTitleContainer.prop('oldText'));
    var changeMacroButton = macroEditorModal.find('.modal-footer .btn-default').not('*[data-dismiss="modal"]');
    changeMacroButton.text(changeMacroButton.prop('oldText'));
  };

  var runGadgetWizard = function(gadget, ckeditor, macroWizard) {
    currentGadget = gadget || {};
    // The macro wizard is returning a jQuery.Deferred() object that we convert to a standard Promise.
    return Promise.resolve(macroWizard(getMacroCall(gadget, ckeditor))).then(function(macroCall) {
      return {
        title: $('.macro-editor input[name="$gadgetTitle"]').val(),
        content: ckeditor.plugins.registered['xwiki-macro'].serializeMacroCall(macroCall)
      };
    }).finally(function() {
      currentGadget = null;
      restoreMacroSelector();
      restoreMacroEditor();
    });
  };

  return function(gadget) {
    // xwiki-ckeditor module is still using jQuery.Deferred() because it needs to support older versions of XWiki that
    // have to work with obsolete browsers, but we can convert it easily to a standard Promise.
    return Promise.resolve(ckeditorPromise).then(getMacroWizard)
      .then(data => runGadgetWizard(gadget, data.ckeditor, data.macroWizard));
  };
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths, $l10n]));
