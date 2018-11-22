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
 * TODO Don't save if there were no changes
 * TODO Support for the WYSIWYG editors
 * TODO Don't show in the class editor, if there is no class defined
 */
editors.AutoSave = Class.create({
  options : {
    /** Is the autosave enabled ? */
    enabled: false,
    /** If enabled, how frequent are the savings */
    frequency: 5, // minutes
    /** Is the UI for configuring the autosave enabled or not? */
    showConfigurationUI: true,
    /** Disabled text opacity **/
    disabledOpacity: 0.2,
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
    if (this.options.enabled) {
      this.startTimer();
    }
  },
  /**
   * The metadata elements are the version comment input and the minor edit checkbox in the editor form.
   * They may be missing if the document is new or if the wiki was configured not to display them.
   * If they are missing, hidden inputs are created and introduced in the form in their place.
   * By means of these, every autosaved version is marked as minor and contains the text "(Autosaved)" in the comment.
   */
  initVersionMetadataElements : function() {
    var container = new Element("div", {"class" : "hidden"});
    this.editComment = this.form.comment; // The element containing the edit comment from the edit form
    if (!this.editComment) {
      this.editComment = new Element('input', {type : "hidden", name: "comment"});
      this.customMetadataElementsContainer = container;
      container.insert(this.editComment);
    }
    this.minorEditCheckbox = this.form.minorEdit; // The minor edit checkbox from the edit form
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
    this.autosaveCheckbox = new Element('input', {type: "checkbox", checked: this.options.enabled, name: "doAutosave", id: "doAutosave"});
    // Input for setting the autosave frequency
    this.autosaveInput = new Element('input', {type: "text", value: this.options.frequency, size : "2", "class": "autosave-frequency"});
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
    if (!this.options.enabled) {
      frequencyLabel.setOpacity(this.options.disabledOpacity);
    }
    // A paragraph containing the whole thing
    var container = new Element('div', {"id": "autosaveControl"});
    container.appendChild(autosaveLabel);
    container.appendChild(document.createTextNode(" "));
    container.appendChild(frequencyLabel);
    container.appendChild(document.createTextNode(" "));
    // Insert in the editing UI
    $(document.body).down(".bottombuttons .buttons").insert({bottom : container});
    // If we keep the autosave control in the form, the fast back-forward is broken in FF, so we lose the edited content
    // when pressing the browser Back button, instead of the form Back to Edit. Catch the form submission and remove the
    // controls.
    this.form.observe("submit", function() {
      container.remove();
    });
    // When hitting cancel, the form isn't submitted anymore, instead the location is changed directly. In order to fix
    // the fastback problem above for Cancel, we need to also listen to this event:
    document.observe("xwiki:actions:cancel", function() {
      container.remove();
    });
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
      this.options.enabled = this.autosaveCheckbox.checked;
      if (this.options.enabled) {
        this.startTimer();
        this.autosaveInput.up("label").setOpacity('1.0');
      } else {
        this.stopTimer();
        this.autosaveInput.up("label").setOpacity(this.options.disabledOpacity);
      }
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
        this.restartTimer();
      } else {
        // no: restore the previous value in the input
        this.autosaveInput.value = this.options.frequency;
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
   * Start autosave timer when the autosave is enabled.
   * Every (this.options.frequency * 60) seconds, the callback function doAutosave is called.
   */
  startTimer : function() {
    this.timer = new PeriodicalExecuter(this.doAutosave.bind(this), this.options.frequency * 60 /* seconds in a minute */);
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
   * Restart the timer when the autosave frequency is changed, to take into account the new frequency.
   */
  restartTimer : function() {
    this.stopTimer();
    this.startTimer();
  },

  /**
   * The function that performs the actual automatic save, if the content has changed.
   * It marks the version as minor and updates the version comment with "(Autosaved)".
   * Then, it fires the custom event <tt>xwiki:actions:save</tt> to invoke the
   * AjaxSaveAndContinue. Afterwards, it resets the version metadata elements to their
   * previous state.
   */
  doAutosave : function() {
    this.updateVersionMetadata();
    // Hacks to force the rich text editors dump the data into the textarea
    // TODO Write me!
    // Call save and continue
    document.fire("xwiki:actions:save", {"continue": true, form: this.editComment.form});
    // Restore comment and minor edit to previous values
    this.resetVersionMetadata();
  },
  /**
   * Marks the version as minor and updates the version comment with "(Autosaved)".
   */
  updateVersionMetadata : function() {
    if(this.customMetadataElementsContainer) {
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
    if(this.customMetadataElementsContainer) {
      this.customMetadataElementsContainer.remove();
    }
    this.editComment.value = this.userEditComment;
    this.minorEditCheckbox.checked = this.userMinorEdit;
  }
});

function init() {
  // Make sure the AjaxSaveAndContinue class exist.
  if (!XWiki.actionButtons || !XWiki.actionButtons.AjaxSaveAndContinue) {
    if (console && console.warn) {
      console.warn("[Autosave feature] Required class missing: XWiki.actionButtons.AjaxSaveAndContinue");
    }
  } else {
    return new editors.AutoSave();
  }
}

// When the document is loaded, create the Autosave control
(XWiki.domIsLoaded && init())
|| document.observe("xwiki:dom:loaded", init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
