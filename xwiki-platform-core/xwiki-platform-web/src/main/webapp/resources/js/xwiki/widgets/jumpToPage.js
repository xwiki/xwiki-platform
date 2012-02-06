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
  /** The template of the XWiki URL. */
  urlTemplate : "$xwiki.getURL('__space__.__document__', '__action__')",
  /** Constructor. Registers the key listener that pops up the dialog. */
  initialize : function($super) {
    var content = new Element("div");
    this.input = new Element("input", {
      "type" : "text",
      "id" : "jmp_target",
      "title" : "$msg.get('core.viewers.jump.dialog.input.tooltip')"
    });
    content.appendChild(this.input);
    this.viewButton = this.createButton("button", "$msg.get('core.viewers.jump.dialog.actions.view')", "$msg.get('core.viewers.jump.dialog.actions.view.tooltip')", "jmp_view");
    this.editButton = this.createButton("button", "$msg.get('core.viewers.jump.dialog.actions.edit')", "$msg.get('core.viewers.jump.dialog.actions.edit.tooltip')", "jmp_edit");
    var buttonContainer = new Element("div", {"class" : "buttons"});
    buttonContainer.appendChild(this.viewButton);
    buttonContainer.appendChild(this.editButton);
    content.appendChild(buttonContainer);
    $super(
      content,
      {
        "show" : { method : this.showDialog, keys : [$msg.get('core.viewers.jump.shortcuts')] },
        "view" : { method : this.openDocument, keys : [$msg.get('core.viewers.jump.dialog.actions.view.shortcuts')] },
        "edit" : { method : this.openDocument, keys : [$msg.get('core.viewers.jump.dialog.actions.edit.shortcuts')] }
      },
      {
        title : "$msg.get('core.viewers.jump.dialog.content')",
        verticalPosition : "top"
      }
    );
    this.addQuickLinksEntry();
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
        script: "${request.contextPath}/rest/wikis/${context.database}/search?scope=name&number=10&",
        // Prefixed with & since the current (as of 1.7) Suggest code does not automatically append it.
        varname: "q",
        noresults: "$msg.get('core.viewers.jump.suggest.noResults')",
        icon: "${xwiki.getSkinFile('icons/silk/page_white_text.png')}",
        json: true,
        resultsParameter : "searchResults",
        resultId : "id",
        resultValue : "pageFullName",
        resultInfo : "pageFullName",
        timeout : 30000,
        parentContainer : this.dialog
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
  /**
   * Open the selected document in the specified mode.
   * 
   * @param {Event} event The event that triggered this action. Either a keyboard shortcut or a button click.
   * @param {String} mode The mode that the document should be opened in. One of "view" or "edit". Note that on the
   *     server side, "edit" could be replaced with "inline" if the document is sheet-based.
   */
  openDocument : function(event, mode) {
    if (!$('as_jmp_target') && this.input.value != "") {
      Event.stop(event);
      window.self.location = this.urlTemplate.replace("__space__/__document__", this.input.value.replace(".", "/")).replace("__action__", mode);
    }
  },
  addQuickLinksEntry : function() {
    $$(".panel.QuickLinks .xwikipanelcontents").each(function(item) {
      var jumpToPageActivator = new Element('span', {'class': "jmp-activator"});
      jumpToPageActivator.update("$msg.get('core.viewers.jump.quickLinksText')");
      Event.observe(jumpToPageActivator, "click", function(event) {
        this.showDialog(event);
      }.bindAsEventListener(this));
      item.appendChild(jumpToPageActivator);
    }.bind(this));
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
