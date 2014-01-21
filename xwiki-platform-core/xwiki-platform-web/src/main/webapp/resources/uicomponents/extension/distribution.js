var XWiki = (function (XWiki) {
// Start XWiki augmentation.
/**
 * Enhances the distribution step where the default UI is installed, upgraded or downgraded.
 */
XWiki.DefaultUIStep = Class.create({
  initialize : function () {
    document.observe('xwiki:extension:statusChanged', this._onExtensionStatusChanged.bindAsEventListener(this));
    this._maybeEnhancePreviousUiForm();
  },

  /**
   * Reacts to the actions taken on the extensions displayed on the default UI step.
   */
  _onExtensionStatusChanged : function(event) {
    var stepButtons = $('stepButtons') && $('stepButtons').select('button');
    if (!stepButtons) return;
    var extension = event.memo.extension;
    var status = extension.getStatus();
    if (status == 'loading') {
      // Disable all step buttons while an extension job is running.
      stepButtons.invoke('disable');
    } else {
      // Enable all step buttons after an extension job is finished.
      stepButtons.invoke('enable');
      if (extension.getId() == '$services.distribution.getUIExtensionId().id'
          && extension.getVersion() == '$services.distribution.getUIExtensionId().version.value') {
        this._onDefaultUiExtensionStatusChanged(stepButtons, status);
      } else if (this._previousUiExtensionId && extension.getId() == this._previousUiExtensionId.id
          && extension.getVersion() == this._previousUiExtensionId.version) {
        this._onPreviousUiExtensionStatusChanged(status);
      }
    }
  },

  /**
   * Enables the continue button after the default UI extension is installed.
   */
  _onDefaultUiExtensionStatusChanged : function(stepButtons, status) {
    if (status == 'installed') {
      // Show the Continue button.
      stepButtons[0].up().removeClassName('hidden');
      stepButtons[1].up().addClassName('hidden');
      stepButtons[2].up().addClassName('hidden');
    } else {
      // Show the Skip and Cancel buttons.
      stepButtons[0].up().addClassName('hidden');
      stepButtons[1].up().removeClassName('hidden');
      stepButtons[2].up().removeClassName('hidden');
    }
  },

  _maybeEnhancePreviousUiForm : function() {
    var form = $('previousUi');
    if (!form) return;
    // Enhance the form actions.
    form.down('.button').observe('click', this._resolvePreviousUiExtension.bindAsEventListener(this));
    var secondaryButton = form.down('.button.secondary');
    secondaryButton.up().removeClassName('hidden');
    var hidePreviousUiForm = this._hidePreviousUiForm.bindAsEventListener(this);
    secondaryButton.observe('click', hidePreviousUiForm);
    // Simplify the way the previous UI is specified.
    $('previousUiVersionList') && this._enhancePreviousUiInput();
    // Enhance and show the upgrade question.
    var question = form.previous().removeClassName('hidden');
    question.down('.button').observe('click', function(event) {
      event.stop();
      question.hide();
      // Make sure the form is enabled because the browser can cache the state of the form fields (see XWIKI-9717).
      form.enable().show().focusFirstElement();
    }).activate();
    question.down('.button.secondary').observe('click', hidePreviousUiForm);
    // Hide the form.
    form.hide()
    // Hide the recommended UI (everything up to the step buttons).
    var stop = $('stepButtons').up('form');
    var next = form.next();
    while (next && next != stop) {
      next = next.hide().next();
    }
  },

  _enhancePreviousUiInput : function() {
    // The element used to toggle advanced input.
    var versionEditButton = new Element('input', {
      type: 'image',
      'class': 'icon',
      src: '$xwiki.getSkinFile("icons/silk/pencil.png")',
      alt: "$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIAdvancedInputHint'))",
      title: "$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIAdvancedInputHint'))"
    });
    var versionInput = $('previousUiVersion');
    var versionList = $('previousUiVersionList');
    var idEditButton = versionEditButton.cloneNode();
    var idInput = $('previousUiId');
    // Display the version list by default instead of the version input. Allow the users to enter a different version.
    versionList.insert({'after': versionEditButton}).up('dd').removeClassName('hidden').addClassName('versionSelector')
      .previous().removeClassName('hidden');
    versionInput.up('dd').hide().previous().hide();
    // Hide the id input and its hint by default because we auto-complete the id based on the selected version.
    idInput.hide().up('dd').previous().down('.xHint').hide();
    // Display a pencil next to the id value to let the user change it.
    idInput.insert({after: idEditButton}).insert({after: new Element('span')});
    // Hide the id label and value by default. Display it when the user selects a version.
    idInput.up('dd').hide().previous().hide();
    versionList.observe('change', this._onSelectPreviousUiVersion.bind(this));
    // Allow advanced input.
    versionEditButton.observe('click', this._switchToAdvancedPreviousUiInput.bindAsEventListener(this));
    idEditButton.observe('click', this._switchToAdvancedPreviousUiInput.bindAsEventListener(this));
  },

  _onSelectPreviousUiVersion : function() {
    var versionList = $('previousUiVersionList');
    var selectedVersion = versionList.options[versionList.selectedIndex];
    // Fill the version input with the selected value in case the user decides to modify it.
    $('previousUiVersion').value = selectedVersion.value;
    // Show the extension id that corresponds to the selected version.
    var idInput = $('previousUiId');
    idInput.next().update(selectedVersion.title).up('dd').show().previous().show();
    // Fill the id input with the extension id that corresponds to the selected version.
    idInput.value = selectedVersion.title;
  },

  _switchToAdvancedPreviousUiInput : function(event) {
    event.stop();
    // Hide the version list and its edit button.
    $('previousUiVersionList').up('dd').hide().previous().hide();
    // Make sure the id and its edit button are hidden.
    $('previousUiId').next().hide().next().hide();
    // Show the version input and its hint.
    $('previousUiVersion').up('dd').show().previous().show();
    // Show the id input and its hint.
    $('previousUiId').show().up('dd').show().previous().show().down('.xHint').show();
    // Focus the right input depending on which edit button was clicked.
    event.element().previous('select') ? $('previousUiVersion').activate() : $('previousUiId').activate();
  },

  _hidePreviousUiForm : function(event) {
    event && event.stop();
    var form = $('previousUi');
    form.hide().previous().hide();
    for (var next = form.next(); next; next = next.show().next());
  },

  _resolvePreviousUiExtension : function(event) {
    event.stop();
    var form = $('previousUi');
    var formData = form.serialize(true);
    new Ajax.Request(form.action, {
      parameters: {
        extensionId: formData.previousUiId,
        extensionVersion: formData.previousUiVersion,
        hideExtensionDetails: true
      },
      onCreate: function() {
        form.disable();
        // Remove the message corresponding to a failed search.
        var message = form.down('.buttons').next();
        message && message.remove();
      },
      onSuccess: function(response) {
        var container = new Element('div').update(response.responseText);
        var previousUiExtension = container.down('.extension-item');
        if (previousUiExtension) {
          if (previousUiExtension.down('button[name="extensionAction"][value="install"]')) {
            // The specified previous UI is not installed. We have to update the extension index.
            this._previousUiExtensionId = {
              id: formData.previousUiId,
              version: formData.previousUiVersion
            };
            this._displayPreviousUiExtension(previousUiExtension);
          } else {
            // The specified previous UI is probably already installed. Just show the recommended UI.
            this._hidePreviousUiForm();
          }
        } else {
          // Display the received message.
          form.enable().insert(container);
        }
      }.bind(this),
      on0: function(response) {
        response.request.options.onFailure(response);
      },
      onFailure: function() {
        form.enable();
        new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIRequestFailed'))", 'error');
      }
    });
  },

  _displayPreviousUiExtension : function(previousUiExtension) {
    var form = $('previousUi');
    var hint = new Element('div', {'class': 'xHint'}).update("$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIHint'))".escapeHTML());
    var container = new Element('div').insert(hint).insert(previousUiExtension);
    form.hide().insert({after: container});
    // Enhance the extension display.
    document.fire('xwiki:dom:updated', {elements: [container]});
    // Hack the install button to perform a fake install (only mark the extension as installed).
    var installButton = previousUiExtension.down('button[name="extensionAction"][value="install"]');
    installButton.update("$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIRepairLabel'))".escapeHTML());
    installButton.title = "$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIRepairHint'))";
    installButton.value = 'repairXAR';
    installButton.activate();
    // Add the form token (for CSRF protection) to execute the job without confirmation (without a job plan).
    installButton.insert({after: new Element('input', {
      type: 'hidden',
      name: 'form_token',
      value: document.head.down('meta[name="form_token"]').readAttribute('content')
    })});
  },

  _onPreviousUiExtensionStatusChanged : function(status) {
    if (status == 'installed') {
      var form = $('previousUi');
      // Remove the previous UI extension display.
      form.next().remove();
      // Display the default UI extension.
      for (var next = form.next(); next; next = next.show().next());
      // Refresh the display of the default UI extension so that we get the upgrade button.
      var defaultUiExtension = form.next('.xform').previous().down('.extension-item');
      defaultUiExtension && defaultUiExtension._extensionBehaviour.refresh({hideExtensionDetails: true});
    }
  }
});

/**
 * Base class for steps that list extensions and that require those extensions to satisfy some condition before they
 * can be completed.
 */
var AbstractExtensionListStep = Class.create({
  initialize : function () {
    this.container = $('distributionWizard');
    // Listen to extension status change to be able to update the step buttons.
    document.observe('xwiki:extension:statusChanged', this._updateStepButtons.bind(this));
    // Listen to DOM changes to catch when the list of extensions is reloaded.
    document.observe('xwiki:dom:updated', function(event) {
      event.memo.elements.each(function(element) {
        // Update the step buttons only if the updated element contains extensions.
        element.down('.extension-item') && this._updateStepButtons();
      }.bind(this));
    }.bindAsEventListener(this));
  },

  _updateStepButtons : function() {
    var stepButtons = $('stepButtons');
    if (!stepButtons) return;
    stepButtons = stepButtons.select('button');
    // Disable the step buttons if there is any extension loading.
    if (this.container.down('.extension-item.extension-item-loading') || this.container.down('.extension-log-item-loading')) {
      // Disable all step buttons.
      stepButtons.invoke('disable');
      this._disable && this._disable();
    } else {
      // Enable all step buttons.
      stepButtons.invoke('enable');
      this._enable && this._enable();
      // Show the Continue button if the step is completed.
      if (this._isCompleted && this._isCompleted()) {
        // Show the Continue button.
        stepButtons[0].up().removeClassName('hidden');
        stepButtons[1].up().addClassName('hidden');
        stepButtons[2].up().addClassName('hidden');
      } else {
        // Show the Skip and Cancel buttons.
        stepButtons[0].up().addClassName('hidden');
        stepButtons[1].up().removeClassName('hidden');
        stepButtons[2].up().removeClassName('hidden');
      }
    }
  }
});

/**
 * Enhances the distribution step where the outdated extensions are upgraded.
 */
XWiki.OutdatedExtensionsStep = Class.create(AbstractExtensionListStep, {
  _enable : function() {
    var checkForUpdatesLink = this.container.down('.checkForUpdates');
    checkForUpdatesLink && checkForUpdatesLink.up('.xHint').show();
  },

  _disable: function() {
    var checkForUpdatesLink = this.container.down('.checkForUpdates');
    checkForUpdatesLink && checkForUpdatesLink.up('.xHint').hide();
  },

  _isCompleted : function() {
    var invalidExtensionsCount = 0;
    var invalidExtensionsFixedCount = 0;
    this.container.select('.invalidExtensions').each(function(invalidExtensionsWrapper) {
      var invalidExtensions = invalidExtensionsWrapper.childElements();
      invalidExtensionsCount += invalidExtensions.size();
      invalidExtensionsFixedCount += invalidExtensions.filter(function(extension) {
        return extension.hasClassName('extension-item-installed');
      }).size();
    });
    return invalidExtensionsFixedCount == invalidExtensionsCount;
  }
});

var WikisStep = Class.create(AbstractExtensionListStep, {
  _isCompleted : function() {
    // It's not mandatory to upgrade all the wikis in this step because the upgrade can be performed later by accessing
    // each wiki separately. So unless there is a subwiki upgrade in progress, this step is always completed.
    return true;
  }
});

function init() {
  // Make sure the users don't cancel the wizard by mistake.
  var cancelButton = $('body').down('#stepButtons button[value=CANCEL]');
  cancelButton && cancelButton.observe('click', function(event) {
    if (!window.confirm("$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.cancelConfirmation'))")) {
      event.stop();
    }
  });

  $('extension.defaultui') && new XWiki.DefaultUIStep();
  $('extension.defaultui.wikis') && new WikisStep();
  $('extension.outdatedextensions') && new XWiki.OutdatedExtensionsStep();
  return true;
}

// When the document is loaded, trigger the enhancements.
(XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);

$('body').select('.expandable .parent').each(function(parent) {
	parent.toggleClassName("collapsed");
	parent.next().toggle();
	parent.observe('click', function(event) {
		parent.toggleClassName("collapsed");
		parent.next().toggle();
	  });
	});


// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
