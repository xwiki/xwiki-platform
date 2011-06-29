var XWiki = (function (XWiki) {
// Start XWiki augmentation.
var viewers = XWiki.viewers = XWiki.viewers || {};
/**
 * Enhancements for the Attachment upload area: adding and removing file fields, resetting with the Cancel button,
 * preventing submit if no files are selected.
 */
viewers.Attachments = Class.create({
  /** Counter for creating distinct upload field names. */
  counter : 1,
  /** Constructor. Adds all the JS improvements of the Attachment area. */
  initialize : function() {
    if ($("attachform")) {
      // Listeners for AJAX attachment deletion
      this.addDeleteListener();
      // If the upload form is already visible, enhance it.
      this.prepareForm();
    } else {
      // Otherwise, we wait for a notification for the AJAX loading of the Attachments metadata tab.
      this.addTabLoadListener();
    }
  },
  /**
   * Ajax attachment deletion.
   * For all delete buttons, listen to "click", and make ajax request to remove the attachment. Remove the corresponding
   * HTML element on succes. Display error message (alert) on failure.
   */
  addDeleteListener : function() {
    $$('.attachment a.deletelink').each(function(item) {
      item.observe('click', function(event) {
        item.blur();
        event.stop();
        if (item.disabled) {
          // Do nothing if the button was already clicked and it's waiting for a response from the server.
          return;
        } else {
          new XWiki.widgets.ConfirmedAjaxRequest(
            /* Ajax request URL */
            item.readAttribute('href') + (Prototype.Browser.Opera ? "" : "&ajax=1"),
            /* Ajax request parameters */
            {
              onCreate : function() {
                // Disable the button, to avoid a cascade of clicks from impatient users
                item.disabled = true;
              },
              onSuccess : function() {
                // Remove the corresponding HTML element from the UI and update the attachment count
                item.up(".attachment").remove();
                this.updateCount();
              }.bind(this),
              onComplete : function() {
                // In the end: re-inable the button
                item.disabled = false;
              }
            },
            /* Interaction parameters */
            {
               confirmationText: "$msg.get('core.viewers.attachments.delete.confirm')",
               progressMessageText : "$msg.get('core.viewers.attachments.delete.inProgress')",
               successMessageText : "$msg.get('core.viewers.attachments.delete.done')",
               failureMessageText : "$msg.get('core.viewers.attachments.delete.failed')"
            }
          );
        }
      }.bindAsEventListener(this));
    }.bind(this));
  },
  updateCount : function() {
    if ($("Attachmentstab") && $("Attachmentstab").down(".itemCount")) {
      $("Attachmentstab").down(".itemCount").update("$msg.get('docextra.extranb', ['__number__'])".replace("__number__", $("Attachmentspane").select(".attachment").size()));
    }
    if ($("attachmentsshortcut") && $("attachmentsshortcut").down(".itemCount")) {
      $("attachmentsshortcut").down(".itemCount").update("$msg.get('docextra.extranb', ['__number__'])".replace("__number__", $("Attachmentspane").select(".attachment").size()));
    }
  },
  /** Enhance the upload form with JS behaviors. */
  prepareForm : function() {
    if (!$("attachform")) {
      return;
    }
    this.form = $("attachform").up("form");
    this.defaultFileDiv = this.form.down("input[type='file']").up("div");
    this.inputSize = this.form.down("input[type='file']").size;
    this.addInitialRemoveButton();
    this.addAddButton();
    this.blockEmptySubmit();
    this.resetOnCancel();
  },
  /** By default the form contains one upload field. Add a "remove" button for this one, too. */
  addInitialRemoveButton : function() {
    this.defaultFileDiv.appendChild(this.createRemoveButton());
  },
  /** Add an "Add another file" button below the file fields. */
  addAddButton : function() {
    var addButton = new Element("input", {
      type: "button",
      value: "$msg.get('core.viewers.attachments.upload.addFileInput')",
      className: "attachmentActionButton add-file-input"
    });
    this.addDiv = new Element("div");
    this.addDiv.appendChild(addButton);
    Event.observe(addButton, 'click', this.addField.bindAsEventListener(this));
    this.defaultFileDiv.up().insertBefore(this.addDiv, this.defaultFileDiv.next());
  },
  /** Add a submit listener that prevents submitting the form if no file was specified. */
  blockEmptySubmit : function() {
    Event.observe(this.form, 'submit', this.onSubmit.bindAsEventListener(this));
  },
  /** Add a reset listener that resets the number of file fields to 1. */
  resetOnCancel : function() {
    Event.observe(this.form, 'reset', this.onReset.bindAsEventListener(this));
    Event.observe(this.form.down('.cancel'), 'click', this.onReset.bindAsEventListener(this));
  },
  /** Creates and inserts a new file input field. */
  addField : function(event) {
    var fileInput = new Element("input", {
      type: "file",
      name: "filepath_" + this.counter,
      size: this.inputSize,
      className: "uploadFileInput"
    });
    // For the moment, specifying a different name is not used anymore.
    var filenameInput = new Element("input", {type: "hidden", name : "filename_" + this.counter});
    var removeButton = this.createRemoveButton();
    var containerDiv = new Element("div");
    containerDiv.insert(filenameInput).insert(fileInput).insert(removeButton);
    this.addDiv.parentNode.insertBefore(containerDiv, this.addDiv);
    // Remove the focus border from the button
    event.element().blur();
    this.counter++;
  },
  /** Remove a file field when pressing the corresponding "Remove" button. */
  removeField : function(event) {
    event.element().up("div").remove();
  },
  /** Create a remove button that triggers {@link #removeField} when clicked. */
  createRemoveButton : function() {
    var removeButton = new Element("input", {
      type: "button",
      value: "$msg.get('core.viewers.attachments.upload.removeFileInput')",
      title: "$msg.get('core.viewers.attachments.upload.removeFileInput.title')",
      className: "attachmentActionButton remove-file-input"
    });
    Event.observe(removeButton, "click", this.removeField.bindAsEventListener(this));
    return removeButton;
  },
  /** Form submit listener. It checks that at least one file item contains a filename. If not, cancel the submission. */
  onSubmit : function(event) {
    var hasFiles = false;
    this.form.getInputs("file").each(function(item) {
      if(item.value != '') {
        hasFiles = true;
      }
    });
    if(!hasFiles) {
      event.stop();
    }
  },
  /** Form reset listener. It resets the number of file fields to just one. */
  onReset : function(event) {
    if (event) {
      event.stop();
    }
    this.form.getInputs("file").each(function(item) {
      item.up().remove();
    });
    this.counter = 1;
    this.addField(event);
  },
  /**
   * Registers a listener that watches for the insertion of the Attachments tab and triggers the form enhancement.
   * After that, the listener removes itself, since it is no longer needed.
   */
  addTabLoadListener : function(event) {
    var listener = function(event) {
      if (event.memo.id == 'Attachments') {
        this.addDeleteListener();
        this.prepareForm();
        document.stopObserving("xwiki:docextra:loaded", listener);
        delete listener;
      }
    }.bindAsEventListener(this);
    document.observe("xwiki:docextra:loaded", listener);
  }
});

// When the document is loaded, trigger the attachment form enhancements.
(XWiki.domIsLoaded && new viewers.Attachments())
|| document.observe("xwiki:dom:loaded", function() { new viewers.Attachments(); });

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));

