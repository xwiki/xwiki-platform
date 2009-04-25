// Make sure the XWiki 'namespace' and the AjaxSaveAndContinue class exist.
if (typeof(XWiki) == "undefined"
  || typeof(XWiki.actionButtons) == "undefined"
  || typeof(XWiki.actionButtons.AjaxSaveAndContinue) == "undefined") {
  if (typeof console != "undefined" && typeof console.warn == "function") {
    console.warn("[Autosave feature] Required class missing: XWiki.actionButtons.AjaxSaveAndContinue");
  }
} else {
if (typeof(XWiki.dataEditors) == 'undefined') {
  XWiki.dataEditors = new Object();
}

/**
 * Autosave feature.
 * TODO Improve i18n support
 * TODO Don't save if there were no changes
 * TODO Support for the WYSIWYG editors
 * TODO Create hidden fields for minorEdit and comment when such fields don't exist
 */
XWiki.dataEditors.AutoSave = Class.create({
  enabled: false,
  frequency: 5, // minutes
  initialize : function() {
    this.editComment = $(document.body).down("input[name='comment']"); // The element containing the edit comment from the edit form
    this.minorEditCheckbox = $(document.body).down("input[name='minorEdit']"); // The minor edit checkbox from the edit form
    this.createUIElements();
    this.addListeners();
    if (this.enabled) {
      this.startTimer();
    }
  },

  createUIElements : function() {
    // Checkbox to enable/disable the autosave
    this.autosaveCheckbox = new Element('input', {type: "checkbox", checked: this.enabled});
    // Input for setting the autosave frequency
    this.autosaveInput = new Element('input', {type: "text", value: this.frequency, size : "2", "class": "autosave-frequency"});
    // Labels
    var autosaveLabel = new Element('label');
    autosaveLabel.appendChild(this.autosaveCheckbox);
    autosaveLabel.appendChild(document.createTextNode(" Autosave"));
    var frequencyLabel = new Element('label');
    frequencyLabel.appendChild(document.createTextNode("every "));
    frequencyLabel.appendChild(this.autosaveInput);
    this.timeUnit = new Element('span');
    this.setTimeUnit();
    frequencyLabel.appendChild(document.createTextNode(" "));
    frequencyLabel.appendChild(this.timeUnit);
    if (!this.enabled) {
      frequencyLabel.addClassName('hidden');
    }
    // A paragraph containing the whole thing
    var container = new Element('p', {"id": "autosaveControl"});
    container.appendChild(autosaveLabel);
    container.appendChild(document.createTextNode(" "));
    container.appendChild(frequencyLabel);
    // Insert in the editing UI
    $(document.body).down(".alleditcontent").insert({before : container});
  },

  addListeners : function() {
    // Stop the Enter key from submitting the form
    var preventSubmit = function(event) {
      if (event.keyCode == Event.KEY_RETURN) {
        event.stop();
        event.element().blur();
      }
    };
    ["keydown", "keyup", "keypress"].each(function(eventName) {
      this.autosaveInput.observe(eventName, preventSubmit);
      this.autosaveCheckbox.observe(eventName, preventSubmit);
    }.bind(this));

    // Enable/disable autosave
    Event.observe(this.autosaveCheckbox, "click", function() {
      this.enabled = this.autosaveCheckbox.checked;
      if (this.enabled) {
        this.startTimer();
        this.autosaveInput.up("label").removeClassName("hidden");
      } else {
        this.stopTimer();
        this.autosaveInput.up("label").addClassName("hidden");
      }
    }.bindAsEventListener(this));
    // Set autosave frequency
    Event.observe(this.autosaveInput, "blur", function() {
      // is the given value valid?
      var newFrequency = new Number(this.autosaveInput.value);
      if (newFrequency > 0) {
        // yes: memorize it
        this.frequency = newFrequency;
        this.setTimeUnit();
        // reset autosave loop
        this.restartTimer();
      } else {
        // no: restore the previous value in the input
        this.autosaveInput.value = this.frequency;
      }
      // The input element should look like plain text when not focused.
      // Since IE doesn't understand :focused, use a classname
      this.autosaveInput.removeClassName('focused');
    }.bindAsEventListener(this));
    // The input element should look like any input when focused
    Event.observe(this.autosaveInput, "focus", function() {
      this.autosaveInput.addClassName('focused');
    }.bindAsEventListener(this));
  },

  // TODO This is bad, very difficult to internationalize.
  setTimeUnit : function() {
    if (this.frequency == 1) {
      this.timeUnit.update("minute");
    } else {
      this.timeUnit.update("minutes");
    }
  },

  startTimer : function() {
    this.timer = new PeriodicalExecuter(this.doAutosave.bind(this), this.frequency * 60 /* seconds in a minute */);
  },
  stopTimer : function() {
    if (this.timer) {
      this.timer.stop();
      delete this.timer;
    }
  },
  restartTimer : function() {
    this.stopTimer();
    this.startTimer();
  },

  doAutosave : function() {
    var editComment = "";
    var isMinorEdit = "";
    // Add "(Autosaved)" in the comment field
    // TODO These fields might not be available; create hidden inputs
    if (this.editComment) {
      editComment = this.editComment.value;
      this.editComment.value += " (Autosaved)";
    }
    // Check the minor edit checkbox
    if (this.minorEditCheckbox) {
      isMinorEdit = this.minorEditCheckbox.checked;
      this.minorEditCheckbox.checked = true;
    }
    // Hacks to force the rich text editors dump the data into the textarea
    // TODO Write me!
    // Call save and continue
    document.fire("xwiki:actions:save", {"continue": true, form: this.editComment.form});
    // Restore comment and minor edit to previous values
    if (this.editComment) {
      this.editComment.value = editComment;
    }
    // Check the minor edit checkbox
    if (this.minorEditCheckbox ) {
      this.minorEditCheckbox.checked = isMinorEdit;
    }
  }
});

// When the document is loaded, create the Autosave control
document.observe("xwiki:dom:loaded", function() {
  new XWiki.dataEditors.AutoSave();
});
}//XWiki.actionButtons.AjaxSaveAndContinue exists
