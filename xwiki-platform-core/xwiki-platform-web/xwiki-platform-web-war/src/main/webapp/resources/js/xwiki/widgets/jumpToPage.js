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
  'js': {
    'xwiki-suggestPages': $xwiki.getSkinFile('uicomponents/suggest/suggestPages.js', true)
  },
  'css': {
    'selectize': [
      $services.webjars.url('selectize.js', 'css/selectize.bootstrap3.css'),
      $xwiki.getSkinFile('uicomponents/suggest/xwiki.selectize.css', true)
    ]
  }
})
#set ($l10n = {
  'inputTooltip': $services.localization.render('core.viewers.jump.dialog.input.tooltip'),
  'viewLabel': $services.localization.render('core.viewers.jump.dialog.actions.view'),
  'viewTooltip': $services.localization.render('core.viewers.jump.dialog.actions.view.tooltip'),
  'editLabel': $services.localization.render('core.viewers.jump.dialog.actions.edit'),
  'editTooltip': $services.localization.render('core.viewers.jump.dialog.actions.edit.tooltip'),
  'content': $services.localization.render('core.viewers.jump.dialog.content')
})
#set ($shortcuts = {
  'show': 'core.viewers.jump.shortcuts',
  'view': 'core.viewers.jump.dialog.actions.view.shortcuts',
  'edit': 'core.viewers.jump.dialog.actions.edit.shortcuts'
})
#foreach ($entry in $shortcuts.entrySet())
  ## An action can have multiple  keyboard shortcuts associated (comma separated).
  #set ($values = $services.localization.render($entry.value).split('\s*,\s*'))
  #foreach ($value in $values)
    ## Each keyboard shortcut is wrapped in quotes because it was (poorly) designed to be injected direcly in JavaScript,
    ## which we don't do anymore. So we need to remove the quotes.
    #set ($discard = $values.set($foreach.index, $value.replaceAll('(^[''"])|([''"]$)', '')))
  #end
  #set ($discard = $entry.setValue($values))
#end
#[[*/
// Start JavaScript-only code.
(function(paths, l10n, shortcuts) {
  "use strict";

require.config({
  paths: paths.js
});

window.XWiki = window.XWiki || {};
var widgets = XWiki.widgets = XWiki.widgets || {};
// Make sure the ModalPopup class exist.
if (!XWiki.widgets.ModalPopup) {
  if (console && console.warn) {
    console.warn("[JumpToPage widget] Required class missing: XWiki.widgets.ModalPopup");
  }
} else {
/**
 * "Jump to page" behavior. Allows the users to jump to any other page by pressing a shortcut, entering a page name, and
 * pressing enter. It also enables a Suggest behavior on the document name selector, for easier selection.
 */
widgets.JumpToPage = Class.create(widgets.ModalPopup, {
  /** The template of the XWiki URL. (deprecated) */
  urlTemplate : new XWiki.Document('__document__', '__space__').getURL('__action__'),
  /** Constructor. Registers the key listener that pops up the dialog. */
  initialize : function($super) {
    // Build the modal popup's content
    var content = new Element("div");
    this.input = new Element("input", {
      "type" : "text",
      "id" : "jmp_target",
      "title" : l10n.inputTooltip
    });
    this.input.placeholder = this.input.title;
    content.appendChild(this.input);
    this.viewButton = this.createButton("button", l10n.viewLabel, l10n.viewTooltip, "jmp_view");
    this.editButton = this.createButton("button", l10n.editLabel, l10n.editTooltip, "jmp_edit", "secondary");
    var buttonContainer = new Element("div", {"class" : "buttons"});
    buttonContainer.appendChild(this.viewButton);
    buttonContainer.appendChild(this.editButton);
    content.appendChild(buttonContainer);

    // Initialize the popup
    $super(
      content,
      {
        "show" : {
          method : this.showDialog,
          keys : shortcuts.show
        },
        "view" : {
          method : this.openDocument,
          keys : shortcuts.view,
          options : { 'propagate' : true }
        },
        "edit" : {
          method : this.openDocument,
          keys : shortcuts.edit
        }
      },
      {
        title : l10n.content,
        extraClassName: "jump-dialog",
        verticalPosition : "top"
      }
    );

    // Allow the default close event ('Escape' key) to propagate so that the page picker can catch it and clear the list
    // of suggestions.
    this.shortcuts['close'].options = { 'propagate' : true };
  },
  /**
   * Callback called when the UI was fully retrieved and inserted. Adds listeners to the buttons, enables the suggest,
   * and forwards the call to the {@link #showDialog} method.
   */
  createDialog : function($super, event) {
    // Register the event listeners executed when clicking on the action buttons.
    Event.observe(this.viewButton, 'click', this.openDocument.bindAsEventListener(this, "view"));
    Event.observe(this.editButton, 'click', this.openDocument.bindAsEventListener(this, "edit"));
    $super(event);
    // Add a CSS class to the container in order to better control the styles for the Jump to Page modal.
    this.input.up('.xdialog-modal-container').addClassName('jump-dialog-container');
    // Initialize the page picker.
    var self = this;
    require(['jquery', 'xwiki-suggestPages'], function($) {
      // Load the required CSS.
      paths.css.selectize.forEach(function(url) {
        $('<link/>').attr({
          type: 'text/css',
          rel: 'stylesheet',
          href: url
        }).appendTo('head');
      });
      var enableActionButtons = function(enable) {
        var actionButtons = $(self.viewButton).add(self.editButton).find('input');
        if (enable === false) {
          // Disable the action buttons right away.
          actionButtons.prop('disabled', true);
        } else {
          setTimeout(function() {
            // Enable the action buttons with a short delay in order to prevent the Enter key from closing the modal
            // after a page is selected.
            actionButtons.prop('disabled', false);
          }, 0);
        }
      };
      var updateActionButtons = function(event) {
        enableActionButtons($(self.input).val() !== '');
      };
      $(self.input).on('change', updateActionButtons).suggestPages({maxItems: 1});
      // Disable the action buttons while the dropdown list of suggestions is open in order to prevent the form from
      // being submitted when a page is selected using the Enter key. We have to do this hack because the page picker
      // doesn't stop the propagation of the Enter key event when the dropdown is opened, as it does with the Esc key.
      self.input.selectize.on('dropdown_open', enableActionButtons.bind(null, false));
      // Update the state of the action buttons after the dropdown is closed (either because a page was selected or
      // because the user pressed the Esc key). The state depends on whether the picker has a selected value.
      self.input.selectize.on('dropdown_close', updateActionButtons);
      // We have to focus the page picker here because #showDialog() is not called when the dialog is displayed for the
      // first time as you would expect...
      self.input.selectize.focus();
      // Synchronize the action buttons state with the text input state.
      updateActionButtons();
    });
  },
  /** Called when the dialog is displayed. Enables the key listeners and gives focus to the (cleared) input. */
  showDialog : function($super) {
    // Display the dialog
    $super();
    // Check if the page picker is available.
    if (this.input.selectize) {
      // Clear the previously selected page and focus the page picker.
      this.input.selectize.clear();
      this.input.selectize.focus();
    } else {
      // Clear the input field
      this.input.value = '';
      // Focus the input field
      this.input.focus();
    }
  },
  /**
   * Open the selected document in the specified mode.
   *
   * @param {Event} event The event that triggered this action. Either a keyboard shortcut or a button click.
   * @param {String} mode The mode that the document should be opened in. One of "view" or "edit".
   */
  openDocument : function(event, mode) {
    // Don't do anything if the corresponding action button is disabled (usually when no value is selected).
    if (!this[(mode || 'view') + 'Button'].down('input').disabled) {
      Event.stop(event);
      var reference = XWiki.Model.resolve(this.input.value, XWiki.EntityType.DOCUMENT,
        XWiki.currentDocument.documentReference);
      window.location = new XWiki.Document(reference).getURL(mode);
    }
  }
});

function init() {
  return new widgets.JumpToPage();
}

// When the document is loaded, enable the keyboard listener that triggers the dialog.
(XWiki.domIsLoaded && init())
|| document.observe("xwiki:dom:loaded", init);

} // if the parent widget is defined

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths, $l10n, $shortcuts]));