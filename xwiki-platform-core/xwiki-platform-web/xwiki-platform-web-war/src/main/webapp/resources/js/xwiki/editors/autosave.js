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

function createElement(tagName, properties) {
  return Object.assign(document.createElement(tagName), properties);
}

/**
 * Autosave feature.
 * TODO Improve i18n support
 *
 * Note that this class can be extended using Prototype.js' Class.create(XWiki.editors.AutoSave, {...}). This is why
 * the constructor only delegates to the initialize method (Prototype.js calls only the initialize method when creating
 * an instance of a subclass).
 */
class AutoSave {
  constructor(...args) {
    this.initialize(...args);
  }

  /** Initialization */
  initialize(options) {
    this.options = {...this.options, ...options};
    if (typeof this.options.form === 'string') {
      this.form = document.getElementById(this.options.form);
    } else {
      this.form = this.options.form;
    }
    this.form = this.form || document.getElementById("xwikieditcontent")?.closest('form');
    if (!this.form || this.form.querySelector('#autosaveControl')) {
      return;
    }
    this.initVersionMetadataElements();
    if (this.options.showConfigurationUI) {
      this.createUIElements();
      this.addListeners();
    }
    this.toggleTimer();
  }

  /**
   * The metadata elements are the version comment input and the minor edit checkbox in the editor form.
   * They may be missing if the document is new or if the wiki was configured not to display them.
   * If they are missing, hidden inputs are created and introduced in the form in their place.
   * By means of these, every autosaved version is marked as minor and contains the text "(Autosaved)" in the comment.
   */
  initVersionMetadataElements() {
    const container = createElement("div", {className: "hidden"});
    // The element containing the edit comment (summary) from the edit form.
    this.editComment = this.form.querySelector('input[name="comment"]');
    if (!this.editComment) {
      this.editComment = createElement('input', {type: "hidden", name: "comment"});
      this.customMetadataElementsContainer = container;
      container.append(this.editComment);
    }
    // The minor edit checkbox from the edit form.
    this.minorEditCheckbox = this.form.querySelector('input[name="minorEdit"]');
    if (!this.minorEditCheckbox) {
      // Value already set, does not need to be switched on/off
      this.minorEditCheckbox = createElement('input', {type: "checkbox", name: "minorEdit", checked: true});
      this.customMetadataElementsContainer = container;
      container.append(this.minorEditCheckbox);
    }
  }

  /**
   * The UI of the autosave feature is created and introduced towards the end of the edit form.
   * It contains a checkbox for toggling the autosave and an input that allows to set the autosave interval in minutes.
   */
  createUIElements() {
    // Toggle for the autosave feature.
    this.autosaveCheckbox = createElement('input', {
      type: "checkbox",
      checked: this.options.enabled,
      name: "doAutosave",
      id: "doAutosave"
    });
    // Input for setting the autosave frequency
    this.autosaveInput = createElement('input', {
      type: "text",
      value: this.options.frequency,
      size: 2,
      className: "autosave-frequency"
    });
    // Labels
    const autosaveLabel = createElement('label', {className: 'autosave'});
    autosaveLabel.append(this.autosaveCheckbox,
      "$escapetool.javascript($services.localization.render('core.edit.autosave'))");
    const frequencyLabel = createElement('label', {className: 'frequency'});
    frequencyLabel.append("$escapetool.javascript($services.localization.render('core.edit.autosave.frequency.label'))",
      this.autosaveInput);
    // A paragraph containing the whole thing
    const container = createElement('div', {id: "autosaveControl"});
    this.classNameAutosaveDisabled = 'autosaveDisabled';
    if (!this.options.enabled) {
      container.classList.add(this.classNameAutosaveDisabled);
    }
    container.append(autosaveLabel, " ", frequencyLabel, " ");
    // Insert in the editing UI
    this.form.querySelector('.buttons').append(container);
  }

  /**
   * Adds listeners to the elements in the autosave UI, allowing to acknowledge when the user changes the settings.
   */
  addListeners() {
    // Stop the Enter key from submitting the form
    const preventSubmit = function(event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        event.stopPropagation();
        event.target.blur();
      }
    };
    ["keydown", "keyup", "keypress"].forEach(eventName => {
      this.autosaveInput.addEventListener(eventName, preventSubmit);
      this.autosaveCheckbox.addEventListener(eventName, preventSubmit);
    });

    // Enable/disable autosave
    this.autosaveCheckbox.addEventListener("click", () => {
      this.toggleTimer(this.autosaveCheckbox.checked);
    });

    // Set autosave frequency
    this.autosaveInput.addEventListener("blur", () => {
      // is the given value valid?
      let newFrequency = Number(this.autosaveInput.value);
      if (newFrequency > 0) {
        // yes: memorize it
        this.options.frequency = newFrequency;
        // reset autosave loop
        this.startTimer();
      } else {
        // no: restore the previous value in the input
        this.autosaveInput.value = this.options.frequency;
      }
    });

    this._toggleTimerWhenSaveButtonIsEnabledOrDisabled();
  }

  /**
   * Stop the timer when the save button is disabled (e.g. because there is another save in progress or because the save
   * button was hidden) and restart the timer, if needed, when the save button is enabled.
   */
  _toggleTimerWhenSaveButtonIsEnabledOrDisabled() {
    const observer = new MutationObserver(mutations => {
      mutations.forEach(mutation => {
        if (mutation.target.disabled) {
          this.stopTimer();
        } else {
          this.toggleTimer();
        }
      });
    });
    const saveButton = this.form.querySelector('input[name="action_saveandcontinue"]');
    observer.observe(saveButton, {
      attributes: true,
      attributeFilter: ['disabled']
    });
  }

  toggleTimer(enabled) {
    if (typeof enabled === 'boolean') {
      this.options.enabled = enabled;
    }
    if (this.options.enabled) {
      this.startTimer();
      if (this.autosaveInput) {
        this.autosaveInput.closest('#autosaveControl').classList.remove(this.classNameAutosaveDisabled);
      }
    } else {
      this.stopTimer();
      if (this.autosaveInput) {
        this.autosaveInput.closest('#autosaveControl').classList.add(this.classNameAutosaveDisabled);
      }
    }
  }

  /**
   * Start autosave timer when the autosave is enabled.
   * Every (this.options.frequency * 60) seconds, the callback function doAutosave is called.
   */
  startTimer() {
    // Make sure we stop the existing timer.
    this.stopTimer();
    this.timer = setInterval(() => this.doAutosave(), this.options.frequency * 60 /* seconds in a minute */ * 1000);
  }

  /**
   * Stop the autosave loop when the autosave is disabled or when the autosave frequency is changed
   * and the loop needs to be restarted.
   */
  stopTimer() {
    if (this.timer) {
      clearInterval(this.timer);
      delete this.timer;
    }
  }

  /**
   * The function that performs the actual automatic save, if the content has changed. It marks the version as minor and
   * updates the version comment with "(Autosaved)". Then it clicks on the Save & Continue button. Afterwards, it resets
   * the version metadata elements to their previous state.
   */
  doAutosave() {
    this.updateVersionMetadata();
    try {
      // Click the Save & Continue button. We don't trigger the save event ourselves because:
      // * we don't want to save if the save button is disabled (e.g. if there's another save in progress or if there
      //   are no changes)
      // * the save button might have additional click event listeners, so custom behavior (e.g. validation) that we
      //   want to execute.
      this.form.querySelector('input[name="action_saveandcontinue"]').click();
    } finally {
      // Restore comment and minor edit to previous values.
      this.resetVersionMetadata();
    }
  }

  /**
   * Marks the version as minor and updates the version comment with "(Autosaved)".
   */
  updateVersionMetadata() {
    if (this.customMetadataElementsContainer) {
      this.form.append(this.customMetadataElementsContainer);
    }
    this.userEditComment = this.editComment.value;
    this.userMinorEdit = this.minorEditCheckbox.checked;
    // Add "(Autosaved)" in the comment field
    this.editComment.value += " (Autosaved)";
    // Check the minor edit checkbox
    this.minorEditCheckbox.checked = true;
  }

  /**
   * Resets the version metadata elements to their previous state and the contentChanged to false.
   */
  resetVersionMetadata() {
    if (this.customMetadataElementsContainer) {
      this.customMetadataElementsContainer.remove();
    }
    this.editComment.value = this.userEditComment;
    this.minorEditCheckbox.checked = this.userMinorEdit;
  }
}

AutoSave.prototype.options = {
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
};

/** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
AutoSave.subclasses = [];

editors.AutoSave = AutoSave;

function init() {
  return new editors.AutoSave();
}

// When the document is loaded, create the Autosave control
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
