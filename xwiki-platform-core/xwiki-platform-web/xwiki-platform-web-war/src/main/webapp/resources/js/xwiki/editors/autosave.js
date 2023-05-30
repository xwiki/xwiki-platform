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

var editors = XWiki.editors = XWiki.editors || {};

/**
 * Autosave feature.
 * TODO Improve i18n support
 */
editors.AutoSave = Class.create({
  options : {
    /** Is the autosave enabled ? */
    enabled: false,
    /** If enabled, how frequent are the savings */
    frequency: 5, // minutes
    /** Is the UI for configuring the autosave enabled or not? */
    showConfigurationUI: true,
    /**
     * Form to autosave, either a DOM element or its ID.
     * By default the form containing the element with the "xwikieditcontent" ID is used.
     * If no valid form is specified, then the autosave won't do anything at all.
     */
    form: undefined
  },

  /** Initialization */
  initialize : function(options) {
    this.options = Object.extend(Object.clone(this.options), options || { });
    this.form = $(this.options.form) || ($("xwikieditcontent") && $("xwikieditcontent").up('form'));
    if (!this.form || this.form.down('#autosaveControl')) {
      return;
    }
    this.initVersionMetadataElements();
    if (this.options.showConfigurationUI) {
      this.createUIElements();
      this.addListeners();
    }
    this.toggleTimer();
  },

  /**
   * The metadata elements are the version comment input and the minor edit checkbox in the editor form.
   * They may be missing if the document is new or if the wiki was configured not to display them.
   * If they are missing, hidden inputs are created and introduced in the form in their place.
   * By means of these, every autosaved version is marked as minor and contains the text "(Autosaved)" in the comment.
   */
  initVersionMetadataElements : function() {
    var container = new Element("div", {"class" : "hidden"});
    // The element containing the edit comment (summary) from the edit form.
    this.editComment = this.form.down('input[name="comment"]');
    if (!this.editComment) {
      this.editComment = new Element('input', {type : "hidden", name: "comment"});
      this.customMetadataElementsContainer = container;
      container.insert(this.editComment);
    }
    // The minor edit checkbox from the edit form.
    this.minorEditCheckbox = this.form.down('input[name="minorEdit"]');
    if (!this.minorEditCheckbox) {
      // Value already set, does not need to be switched on/off
      this.minorEditCheckbox = new Element('input', {type : "checkbox", name: "minorEdit", checked: true});
      this.customMetadataElementsContainer = container;
      container.insert(this.minorEditCheckbox);
    }
  },

  /**
   * The UI of the autosave feature is created and introduced at the beginning of the edit form. It comprises a checkbox
   * for enabling / disabling the autosave and an input that allows to set the autosave frequency.
   */
  createUIElements : function() {
    // Checkbox to enable/disable the autosave
    this.autosaveCheckbox = new Element('input', {
      type: "checkbox",
      checked: this.options.enabled,
      name: "doAutosave",
      id: "doAutosave"
    });
    // Input for setting the autosave frequency
    this.autosaveInput = new Element('input', {
      type: "text",
      value: this.options.frequency,
      size: "2",
      "class": "autosave-frequency"
    });
    // Labels
    var autosaveLabel = new Element('label', {'class': 'autosave', 'for' : "doAutosave"});
    autosaveLabel.appendChild(this.autosaveCheckbox);
    autosaveLabel.appendChild(document.createTextNode(" $services.localization.render('core.edit.autosave')"));
    var frequencyLabel = new Element('label', {'class': 'frequency'});
    frequencyLabel.appendChild(document.createTextNode("$services.localization.render('core.edit.autosave.every') "));
    frequencyLabel.appendChild(this.autosaveInput);
    this.timeUnit = new Element('span');
    this.setTimeUnit();
    frequencyLabel.appendChild(document.createTextNode(" "));
    frequencyLabel.appendChild(this.timeUnit);
    // A paragraph containing the whole thing
    var container = new Element('div', {"id": "autosaveControl"});
    this.classNameAutosaveDisabled = 'autosaveDisabled';
    if (!this.options.enabled) {
      container.addClassName(this.classNameAutosaveDisabled);
    }
    container.appendChild(autosaveLabel);
    container.appendChild(document.createTextNode(" "));
    container.appendChild(frequencyLabel);
    container.appendChild(document.createTextNode(" "));
    // Insert in the editing UI
    this.form.down('.buttons').insert(container);
  },

  /**
   * Adds listeners to the elements in the autosave UI, allowing to acknowledge when the user changes the settings.
   */
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
      this.toggleTimer(this.autosaveCheckbox.checked);
    }.bindAsEventListener(this));

    // Set autosave frequency
    Event.observe(this.autosaveInput, "blur", function() {
      // is the given value valid?
      var newFrequency = new Number(this.autosaveInput.value);
      if (newFrequency > 0) {
        // yes: memorize it
        this.options.frequency = newFrequency;
        this.setTimeUnit();
        // reset autosave loop
        this.startTimer();
      } else {
        // no: restore the previous value in the input
        this.autosaveInput.value = this.options.frequency;
      }
    }.bindAsEventListener(this));

    this._toggleTimerWhenSaveButtonIsEnabledOrDisabled();
  },

  /**
   * Changes the label text displaying the time measure unit for autosave freaquency,
   * according to the value introduced by the user in the input (singular or plural).
   * TODO This is bad, very difficult to internationalize.
   */
  setTimeUnit : function() {
    if (this.options.frequency == 1) {
      this.timeUnit.update("minute");
    } else {
      this.timeUnit.update("minutes");
    }
  },

  /**
   * Stop the timer when the save button is disabled (e.g. because there is another save in progress or because the save
   * button was hidden) and restart the timer, if needed, when the save button is enabled.
   */
  _toggleTimerWhenSaveButtonIsEnabledOrDisabled: function() {
    var self = this;
    var observer = new MutationObserver(function(mutations) {
      mutations.forEach(function(mutation) {
        if (mutation.target.disabled) {
          self.stopTimer();
        } else {
          self.toggleTimer();
        }
      });
    });
    var saveButton = this.form.down('input[name="action_saveandcontinue"]');
    observer.observe(saveButton, {
      attributes: true,
      attributeFilter: ['disabled']
    });
  },

  toggleTimer: function(enabled) {
    if (typeof enabled === 'boolean') {
      this.options.enabled = enabled;
    }
    if (this.options.enabled) {
      this.startTimer();
      if (this.autosaveInput) {
        this.autosaveInput.up(1).removeClassName(this.classNameAutosaveDisabled);
      }
    } else {
      this.stopTimer();
      if (this.autosaveInput) {
        this.autosaveInput.up(1).addClassName(this.classNameAutosaveDisabled);
      }
    }
  },

  /**
   * Start autosave timer when the autosave is enabled.
   * Every (this.options.frequency * 60) seconds, the callback function doAutosave is called.
   */
  startTimer : function() {
    // Make sure we stop the existing timer.
    this.stopTimer();
    this.timer = new PeriodicalExecuter(this.doAutosave.bind(this),
      this.options.frequency * 60 /* seconds in a minute */);
  },

  /**
   * Stop the autosave loop when the autosave is disabled or when the autosave frequency is changed
   * and the loop needs to be restarted.
   */
  stopTimer : function() {
    if (this.timer) {
      this.timer.stop();
      delete this.timer;
    }
  },

  /**
   * The function that performs the actual automatic save, if the content has changed. It marks the version as minor and
   * updates the version comment with "(Autosaved)". Then it clicks on the Save & Continue button. Afterwards, it resets
   * the version metadata elements to their previous state.
   */
  doAutosave : function() {
    this.updateVersionMetadata();
    try {
      // Click the Save & Continue button. We don't trigger the save event ourselves because:
      // * we don't want to save if the save button is disabled (e.g. if there's another save in progress or if there
      //   are no changes)
      // * the save button might have additional click event listeners, so custom behavior (e.g. validation) that we
      //   want to execute.
      this.form.down('input[name="action_saveandcontinue"]').click();
    } finally {
      // Restore comment and minor edit to previous values.
      this.resetVersionMetadata();
    }
  },

  /**
   * Marks the version as minor and updates the version comment with "(Autosaved)".
   */
  updateVersionMetadata : function() {
    if (this.customMetadataElementsContainer) {
      this.form.insert(this.customMetadataElementsContainer);
    }
    this.userEditComment = this.editComment.value;
    this.userMinorEdit = this.minorEditCheckbox.checked;
    // Add "(Autosaved)" in the comment field
    this.editComment.value += " (Autosaved)";
    // Check the minor edit checkbox
    this.minorEditCheckbox.checked = true;
  },

  /**
   * Resets the version metadata elements to their previous state and the contentChanged to false.
   */
  resetVersionMetadata : function() {
    if (this.customMetadataElementsContainer) {
      this.customMetadataElementsContainer.remove();
    }
    this.editComment.value = this.userEditComment;
    this.minorEditCheckbox.checked = this.userMinorEdit;
  }
});

function init() {
  return new editors.AutoSave();
}

// When the document is loaded, create the Autosave control
(XWiki.domIsLoaded && init())
|| document.observe("xwiki:dom:loaded", init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
