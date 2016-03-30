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
var XWiki = (function(XWiki) {
// Start XWiki augmentation.
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
  urlTemplate : "$xwiki.getURL('__space__.__document__', '__action__')",
  /** Constructor. Registers the key listener that pops up the dialog. */
  initialize : function($super) {
    // Build the modal popup's content
    var content = new Element("div");
    this.input = new Element("input", {
      "type" : "text",
      "id" : "jmp_target",
      "title" : "$services.localization.render('core.viewers.jump.dialog.input.tooltip')"
    });
    content.appendChild(this.input);
    this.viewButton = this.createButton("button", "$services.localization.render('core.viewers.jump.dialog.actions.view')", "$services.localization.render('core.viewers.jump.dialog.actions.view.tooltip')", "jmp_view");
    this.editButton = this.createButton("button", "$services.localization.render('core.viewers.jump.dialog.actions.edit')", "$services.localization.render('core.viewers.jump.dialog.actions.edit.tooltip')", "jmp_edit");
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
	  keys : [$services.localization.render('core.viewers.jump.shortcuts')]
	},
        "view" : {
	  method : this.openDocument,
	  keys : [$services.localization.render('core.viewers.jump.dialog.actions.view.shortcuts')],
	  options : { 'propagate' : true }
	},
        "edit" : {
	  method : this.openDocument,
	  keys : [$services.localization.render('core.viewers.jump.dialog.actions.edit.shortcuts')]
	}
      },
      {
        title : "$services.localization.render('core.viewers.jump.dialog.content')",
        extraClassName: "jump-dialog",
        verticalPosition : "top"
      }
    );

    // Allow the default close event ('Escape' key) to propagate so that the ajaxsuggest can catch it and clear the suggestions list.
    this.shortcuts['close'].options = { 'propagate' : true };
  },
  /**
   * Callback called when the UI was fully retrieved and inserted. Adds listeners to the buttons, enables the suggest,
   * and forwards the call to the {@link #showDialog} method.
   */
  createDialog : function($super, event) {
    // Register the listeners executed when clicking on the buttons.
    Event.observe(this.viewButton, 'click', this.openDocument.bindAsEventListener(this, "view"));
    Event.observe(this.editButton, 'click', this.openDocument.bindAsEventListener(this, "edit"));
    $super(event);
    if (typeof(XWiki.widgets.Suggest) != "undefined") {
      // Create the Suggest.
      new XWiki.widgets.Suggest(this.input, {
        // This document also provides the suggestions.
        // Trick so that Velocity will get executed but Javascript lint will not choke on it... Note that we cannot
        // use a standard javascript comment ("//") since the minification process removes comments ;)
        // We use the special construct ("/*!") which tells yuicompressor to not compress this part...
        /*!#set ($restURL = "${request.contextPath}/rest/wikis/${xcontext.database}/search?scope=name&number=10&")*/
        script: "$response.encodeURL($restURL)",
        // Prefixed with & since the current (as of 1.7) Suggest code does not automatically append it.
        varname: "q",
        noresults: "$services.localization.render('core.viewers.jump.suggest.noResults')",
        icon: "${xwiki.getSkinFile('icons/silk/page_white_text.png')}",
        json: true,
        resultsParameter : "searchResults",
        resultId : "id",
        resultValue : "pageFullName",
        resultInfo : "pageFullName",
        timeout : 30000,
        parentContainer : this.dialogBox,
        // Make sure the suggest widget does not hogg the Enter key press event.
        propagateEventKeyCodes : [ Event.KEY_RETURN ]
      });
    }
  },
  /** Called when the dialog is displayed. Enables the key listeners and gives focus to the (cleared) input. */
  showDialog : function($super) {
    // Display the dialog
    $super();
    // Clear the input field
    this.input.value = '';
    // Focus the input field
    this.input.focus();
  },
  /** Called when the dialog is closed. Overriding default behavior to check if the user actually wanted to close the suggestions list instead. */
  closeDialog : function($super, event) {
    if (!event.type.startsWith('key') || !this.dialogBox.down('.ajaxsuggest')) {
      // Close the dialog either from the close/x button (mouse event) or when the keyboard shortcut (Escape key) is used and there is no ajax suggestion list displayed.
      $super();
      // Clear the suggestion list so that it does not flicker next time we open the dialog.
      this.input.__x_suggest.clearSuggestions();
    }
  },
  /**
   * Open the selected document in the specified mode.
   *
   * @param {Event} event The event that triggered this action. Either a keyboard shortcut or a button click.
   * @param {String} mode The mode that the document should be opened in. One of "view" or "edit". Note that on the
   *     server side, "edit" could be replaced with "inline" if the document is sheet-based.
   */
  openDocument : function(event, mode) {
    // Don`t do anything if the user is still selecting from the suggestions list or if nothing was entered yet.
    var highlightedSuggestion = this.dialogBox.down('.ajaxsuggest .xhighlight');
    if ((!highlightedSuggestion || highlightedSuggestion.hasClassName('noSuggestion')) && this.input.value != "") {
      Event.stop(event);
      var reference = XWiki.Model.resolve(this.input.value, XWiki.EntityType.DOCUMENT);
      if (reference && reference.type == XWiki.EntityType.DOCUMENT) {
        var documentToGo = new XWiki.Document(reference);
        window.self.location = documentToGo.getURL(mode, '', '');
      } else {
        if (typeof(XWiki.widgets.Suggest) != "undefined") {
          new XWiki.widgets.Notification("$services.localization.render('core.viewers.jump.dialog.invalidNameError')", 'error');
        }
      }
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
// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
