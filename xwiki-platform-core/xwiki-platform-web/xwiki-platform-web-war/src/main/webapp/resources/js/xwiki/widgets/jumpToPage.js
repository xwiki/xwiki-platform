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
    ## Each keyboard shortcut is wrapped in quotes because it was (poorly) designed to be injected directly in JavaScript,
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
  if (console?.warn) {
    console.warn("[JumpToPage widget] Required class missing: XWiki.widgets.ModalPopup");
  }
} else {
/**
 * "Jump to page" behavior. Allows the users to jump to any other page by pressing a shortcut, entering a page name, and
 * pressing enter. It also enables a Suggest behavior on the document name selector, for easier selection.
 *
 * Note that this class can be extended using Prototype.js' Class.create(XWiki.widgets.JumpToPage, {...}), which is why
 * the initialization code is in the initialize method (Prototype.js calls only the initialize method when creating an
 * instance of a subclass).
 */
class JumpToPage extends widgets.ModalPopup {
  /** Constructor. Registers the key listener that pops up the dialog. */
  initialize() {
    // Build the modal popup's content
    const content = document.createElement("div");
    this.input = document.createElement("input");
    Object.assign(this.input, {
      type: "text",
      id: "jmp_target",
      title: l10n.inputTooltip,
      placeholder: l10n.inputTooltip
    });
    content.appendChild(this.input);
    this.viewButton = this.createButton("button", l10n.viewLabel, l10n.viewTooltip, "jmp_view");
    this.editButton = this.createButton("button", l10n.editLabel, l10n.editTooltip, "jmp_edit", "secondary");
    const buttonContainer = document.createElement("div");
    buttonContainer.className = "buttons";
    buttonContainer.appendChild(this.viewButton);
    buttonContainer.appendChild(this.editButton);
    content.appendChild(buttonContainer);

    // Initialize the popup
    super.initialize(
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
  }

  /**
   * Callback called when the UI was fully retrieved and inserted. Adds listeners to the buttons, enables the suggest,
   * and forwards the call to the {@link #showDialog} method.
   */
  createDialog(event) {
    // Register the event listeners executed when clicking on the action buttons.
    this.viewButton.addEventListener('click', event => this.openDocument(event, "view"));
    this.editButton.addEventListener('click', event => this.openDocument(event, "edit"));
    super.createDialog(event);
    // Add a CSS class to the container in order to better control the styles for the Jump to Page modal.
    this.input.closest('.xdialog-modal-container').classList.add('jump-dialog-container');
    // Initialize the page picker.
    require(['jquery', 'xwiki-suggestPages'], $ => {
      const enableActionButtons = enable => {
        const actionButtons = $(this.viewButton).add(this.editButton).find('input');
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
      const updateActionButtons = () => {
        enableActionButtons($(this.input).val() !== '');
      };
      $(this.input).on('change', updateActionButtons).suggestPages({maxItems: 1});
      // Disable the action buttons while the dropdown list of suggestions is open in order to prevent the form from
      // being submitted when a page is selected using the Enter key. We have to do this hack because the page picker
      // doesn't stop the propagation of the Enter key event when the dropdown is opened, as it does with the Esc key.
      this.input.selectize.on('dropdown_open', enableActionButtons.bind(null, false));
      // Update the state of the action buttons after the dropdown is closed (either because a page was selected or
      // because the user pressed the Esc key). The state depends on whether the picker has a selected value.
      this.input.selectize.on('dropdown_close', updateActionButtons);
      // We have to focus the page picker here because #showDialog() is not called when the dialog is displayed for the
      // first time as you would expect...
      this.input.selectize.focus();
      // Synchronize the action buttons state with the text input state.
      updateActionButtons();
    });
  }

  /** Called when the dialog is displayed. Enables the key listeners and gives focus to the (cleared) input. */
  showDialog() {
    // Display the dialog
    super.showDialog();
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
  }

  /**
   * Open the selected document in the specified mode.
   *
   * @param {Event} event The event that triggered this action. Either a keyboard shortcut or a button click.
   * @param {String} mode The mode that the document should be opened in. One of "view" or "edit".
   */
  openDocument(event, mode) {
    // Don't do anything if the corresponding action button is disabled (usually when no value is selected).
    if (!this[(mode || 'view') + 'Button'].querySelector('input').disabled) {
      event?.preventDefault();
      event?.stopPropagation();
      const reference = XWiki.Model.resolve(this.input.value, XWiki.EntityType.DOCUMENT,
        XWiki.currentDocument.documentReference);
      window.location = new XWiki.Document(reference).getURL(mode);
    }
  }
}

/** The template of the XWiki URL. (deprecated) */
JumpToPage.prototype.urlTemplate = new XWiki.Document('__document__', '__space__').getURL('__action__');

/** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
JumpToPage.subclasses = [];

widgets.JumpToPage = JumpToPage;

function init() {
  return new widgets.JumpToPage();
}

// When the document is loaded, enable the keyboard listener that triggers the dialog.
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}

} // if the parent widget is defined

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths, $l10n, $shortcuts]));