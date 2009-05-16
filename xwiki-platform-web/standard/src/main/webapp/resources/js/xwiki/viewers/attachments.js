// Make sure the XWiki 'namespace' exists.
if(typeof(XWiki) == 'undefined') {
  XWiki = new Object();
}
// Make sure the viewers 'namespace' exists.
if(typeof(XWiki.viewers) == 'undefined') {
  XWiki.viewers = new Object();
}
/**
 * Enhancements for the Attachment upload area: adding and removing file fields, resetting with the Cancel button,
 * preventing submit if no files are selected.
 */
XWiki.viewers.Attachments = Class.create({
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
    $$('.attachment .xwikibuttonlinks a.deletelink').each(function(item) {
      item.observe('click', function(event) {
        item.blur();
        event.stop();
        if (item.disabled) {
          // Do nothing if the button was already clicked and it's waiting for a response from the server.
          return;
        } else if (confirm("$msg.get('core.viewers.attachments.delete.confirm')")) { // "Are you sure you want to delete?"
          // Disable the button, to avoid a cascade of clicks from inpatient users
          item.disabled = true;
          // Notify the user that deletion is in progress
          item._x_notification = new XWiki.widgets.Notification("$msg.get('core.viewers.attachments.delete.inProgress')", "inprogress");
          // Make request to delete the attachment
          new Ajax.Request(item.href + (Prototype.Browser.Opera ? "" : "&ajax=1"), {
            // Success: delete de corresponding HTML element
            onSuccess : function() {
              var attachment = item.up(".attachment");
              attachment.remove();
              this.updateCount();
              item._x_notification.replace(new XWiki.widgets.Notification("$msg.get('core.viewers.attachments.delete.done')", "done"));
            }.bind(this),
            // Failure: inform the user why the deletion failed
            onFailure : function(response) {
              var failureReason = response.statusText;
              if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                failureReason = 'Server not responding';
              }
              item._x_notification.replace(new XWiki.widgets.Notification("$msg.get('core.viewers.attachments.delete.failed')" + failureReason, "error"));
            },
            // IE converts 204 status code into 1223...
            on1223 : function(response) {
              response.request.options.onSuccess(response);
            },
            // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
            on0 : function(response) {
              response.request.options.onFailure(response);
            },
            // In the end: re-inable the button
            onComplete : function() {
              item.disabled = false;
            }
          });
        }
      }.bindAsEventListener(this));
    }.bind(this));
  },
  updateCount : function() {
    if ($("Attachmentstab") && $("Attachmentstab").down(".itemcount")) {
      $("Attachmentstab").down(".itemcount").update("$msg.get('docextra.extranb', ['__number__'])".replace("__number__", $("Attachmentspane").select(".attachment").size()));
    }
  },
  /** Enhance the upload form with JS behaviors. */
  prepareForm : function() {
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
document.observe("xwiki:dom:loaded", function() { new XWiki.viewers.Attachments(); });
