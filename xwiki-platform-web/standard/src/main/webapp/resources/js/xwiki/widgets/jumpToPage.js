// Make sure the XWiki 'namespace' and the ModalPopup class exist.
if(typeof(XWiki) == "undefined" || typeof(XWiki.widgets) == "undefined" || typeof(XWiki.widgets.ModalPopup) == "undefined") {
  if (typeof console != "undefined" && typeof console.warn == "function") {
    console.warn("[JumpToPage widget] Required class missing: XWiki.widgets.ModalPopup");
  }
} else {
/**
 * "Jump to page" behavior. Allows the users to jump to any other page by pressing a shortcut, entering a page name, and
 * pressing enter. It also enables a Suggest behavior on the document name selector, for easier selection.
 */
XWiki.widgets.JumpToPage = Class.create(XWiki.widgets.ModalPopup, {
  /** The template of the XWiki URL. */
  urlTemplate : "$xwiki.getURL('__space__.__document__', '__action__')",
  /** Constructor. Registers the key listener that pops up the dialog. */
  initialize : function($super) {
    var content = new Element("div");
    this.input = new Element("input", {
      "type" : "text",
      "id" : "jmp_target",
      "title" : "Space.Document"
    });
    content.appendChild(this.input);
    this.viewButton = this.createButton("button", "View", "View page (Enter, Meta+V)", "jmp_view");
    this.editButton = this.createButton("button", "Edit", "Edit page in the default editor (Meta+E)", "jmp_edit");
    var buttonContainer = new Element("div", {"class" : "buttons"});
    buttonContainer.appendChild(this.viewButton);
    buttonContainer.appendChild(this.editButton);
    content.appendChild(buttonContainer);
    $super(
      content,
      {
        "show" : { method : this.showDialog, keys : ['Meta+G','Ctrl+G', 'Ctrl+/', 'Meta+/'] },
        "view" : { method : this.openDocument, keys : ['Enter', 'Meta+V','Ctrl+V'] },
        "edit" : { method : this.openDocument, keys : ['Meta+E','Ctrl+E'] }
      },
      {
        title : "Go to:",
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
        script: "${request.contextPath}/rest/wikis/${context.database}/search?scope=name&number=10&media=json&",
        // Prefixed with & since the current (as of 1.7) Suggest code does not automatically append it.
        varname: "q",
        noresults: "Document not found",
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
      jumpToPageActivator.update("Jump to any page in the wiki (Meta+G)"); // TODO: i18n!
      Event.observe(jumpToPageActivator, "click", function(event) {
        this.showDialog(event);
      }.bindAsEventListener(this));
      item.appendChild(jumpToPageActivator);
    }.bind(this));
  }
});

// When the document is loaded, enable the keyboard listener that triggers the dialog.
document.observe("xwiki:dom:loaded", function() {
  new XWiki.widgets.JumpToPage();
});
} // if the parent widget is defined