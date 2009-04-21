if(typeof(XWiki) == 'undefined') {
  XWiki = new Object();
}
if(typeof(XWiki.viewers) == 'undefined') {
  XWiki.viewers = new Object();
}
XWiki.viewers.Attachments = Class.create({
  counter : 1,
  initialize : function() {
    if ($("attachform")) {
      // If the upload form is already visible, enhance it.
      this.prepareForm();
    } else {
      // Otherwise, we watch the DOM changes to see when (and if) the form will be later added. This is needed for the AJAX loading of the document metadata tabs.
      this.addDomUpdateListener();
    }
  },
  prepareForm : function() {
    this.form = $("attachform").up("form");
    this.addInitialRemoveButton();
    this.addAddButton();
    this.blockEmptySubmit();
    this.resetOnCancel();
  },
  addInitialRemoveButton : function() {
    this.defaultFileDiv = this.form.down("input[type='file']").up("div");
    this.defaultFileDiv.appendChild(this.createRemoveButton());
  },
  addAddButton : function() {
    var addButton = new Element("input", {type: "button", value: "Add another file", "class": "attachmentActionButton add-file-input"});
    this.addDiv = new Element("div");
    this.addDiv.appendChild(addButton);
    Event.observe(addButton, 'click', this.addField.bindAsEventListener(this));
    this.defaultFileDiv.up().insertBefore(this.addDiv, this.defaultFileDiv.next());
  },
  blockEmptySubmit : function() {
    Event.observe(this.form, 'submit', this.onSubmit.bindAsEventListener(this));
  },
  resetOnCancel : function() {
    Event.observe(this.form, 'reset', this.onReset.bindAsEventListener(this));
  },
  addField : function() {
    var fileInput = new Element("input", {type: "file", name : "filepath_" + this.counter, size: "40", "class": "uploadFileInput"});
    var filenameInput = new Element("input", {type: "hidden", name : "filename_" + this.counter});
    var containerDiv = new Element("div");
    containerDiv.appendChild(filenameInput);
    containerDiv.appendChild(fileInput);
    var removeButton = this.createRemoveButton();
    containerDiv.appendChild(removeButton);
    this.addDiv.parentNode.insertBefore(containerDiv, this.addDiv);
    this.addDiv.down('input').blur();
    this.counter++;
  },
  createRemoveButton : function() {
    var removeButton = new Element("input", {type: "button", value: "Remove", title: "Remove this file", "class": "attachmentActionButton remove-file-input"});
    Event.observe(removeButton, "click", this.removeField.bindAsEventListener(this));
    return removeButton;
  },
  removeField : function(event) {
    Event.element(event).up("div").remove();
  },
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
  onReset : function(event) {
    this.form.getInputs("file").each(function(item) {
      item.up().remove();
    });
    this.counter = 1;
    this.addField();
  },
  addDomUpdateListener : function(event) {
    this.ajaxListener = {
      onComplete: function() {
        this.domModified();
      }.bind(this)
    };
    Ajax.Responders.register(this.ajaxListener);
  },
  domModified : function() {
    if($("attachform")) {
      Ajax.Responders.unregister(this.ajaxListener);
      this.prepareForm();
    }
  }
});

document.observe("dom:loaded", function() { new XWiki.viewers.Attachments(); });
