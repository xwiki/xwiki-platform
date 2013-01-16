var XWiki = (function (XWiki) {
// Start XWiki augmentation.
/**
 * Enhances the distribution step where the main UI is installed, upgraded or downgraded.
 */
XWiki.MainUIStep = Class.create({
  initialize : function () {
    document.observe('xwiki:extension:statusChanged', this._onExtensionStatusChanged.bindAsEventListener(this));
    this._maybeEnhancePreviousUiForm();
  },

  /**
   * Reacts to the actions taken on the extensions displayed on the main UI step.
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
        this._onMainUiExtensionStatusChanged(stepButtons, status);
      } else if (this._previousUiExtensionId && extension.getId() == this._previousUiExtensionId.id
          && extension.getVersion() == this._previousUiExtensionId.version) {
        this._onPreviousUiExtensionStatusChanged(status);
      }
    }
  },

  /**
   * Enables the continue button after the main UI extension is installed.
   */
  _onMainUiExtensionStatusChanged : function(stepButtons, status) {
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
    this._enhancePreviousUiInput();
    // Enhance and show the upgrade question.
    var question = form.previous().removeClassName('hidden');
    question.down('.button').observe('click', function(event) {
      event.stop();
      question.hide();
      form.show().focusFirstElement();
    }).activate();
    question.down('.button.secondary').observe('click', hidePreviousUiForm);
    // Hide the form.
    form.hide()
    // Hide the recommended UI.
    var next = form.next();
    while (next && next.tagName.toLowerCase() != 'form') {
      next = next.hide().next();
    }
  },

  _enhancePreviousUiInput : function() {
    // The element used to toggle advanced input.
    var pencil = new Element('input', {
      type: 'image',
      'class': 'icon',
      src: '$xwiki.getSkinFile("icons/silk/pencil.png")',
      alt: '$escapetool.javascript($msg.get("platform.extension.distributionWizard.uiStepPreviousUIAdvancedInputHint"))',
      title: '$escapetool.javascript($msg.get("platform.extension.distributionWizard.uiStepPreviousUIAdvancedInputHint"))'
    });
    var idInput = $('previousUiId');
    var versionInput = $('previousUiVersion');
    var versionList = $('previousUiVersionList');
    // Display the version list by default instead of the version input.
    versionList.up('dd').removeClassName('hidden').previous().removeClassName('hidden');
    versionInput.up('dd').hide().previous().hide();
    // Hide the id input and its hint by default because we auto-complete the id based on the selected version.
    idInput.hide().up('dd').previous().down('.xHint').hide();
    // Display a pencil next to the id value to let the user change it.
    idInput.insert({after: pencil}).insert({after: new Element('span')});
    // Hide the id label and value by default. Display it when the user selects a version.
    idInput.up('dd').hide().previous().hide();
    versionList.observe('change', this._onSelectPreviousUiVersion.bind(this));
    // Allow advanced input.
    pencil.observe('click', this._switchToAdvancedPreviousUiInput.bindAsEventListener(this));
  },

  _onSelectPreviousUiVersion : function() {
    var idInput = $('previousUiId');
    idInput.up('dd').show().previous().show();
    var versionList = $('previousUiVersionList');
    $('previousUiVersion').value = versionList.options[versionList.selectedIndex].value;
    // Auto-complete the id based on the selected version.
    if (versionList.selectedIndex == 0) {
      var id = '';
    } else if (versionList.selectedIndex < 27) {
      // 4.2M2 -> 3.3-milestone-1
      var id = 'org.xwiki.enterprise:xwiki-enterprise-ui';
    } else if (versionList.selectedIndex < 53) {
      // 3.2.1 -> 2.6-rc-1
      var id = 'org.xwiki.enterprise:xwiki-enterprise-wiki';
    } else {
      // 2.5.2 -> 1.1-milestone-3
      var id = 'com.xpn.xwiki.products:xwiki-enterprise-wiki';
    }
    // Update the value of the hidden input.
    idInput.value = id;
    // Update the displayed value.
    idInput.next().update(id);
  },

  _switchToAdvancedPreviousUiInput : function(event) {
    event.stop();
    event.element().hide().previous().hide();
    // Show the id input and its hint.
    $('previousUiId').show().activate().up('dd').previous().down('.xHint').show();
    // Show the version input and its hint.
    $('previousUiVersion').up('dd').show().previous().show();
    // Hide the version list.
    $('previousUiVersionList').up('dd').hide().previous().hide();
  },

  _hidePreviousUiForm : function(event) {
    event && event.stop();
    var form = $('previousUi');
    form.hide().previous().hide();
    var next = form.next();
    while (next && next.tagName.toLowerCase() != 'form') {
      next = next.show().next();
    }
  },

  _resolvePreviousUiExtension : function(event) {
    event.stop();
    var form = $('previousUi');
    var formData = form.serialize(true);
    new Ajax.Request(form.action, {
      parameters: {
        extensionId: formData.previousUiId,
        extensionVersion: formData.previousUiVersion
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
          if (previousUiExtension.down('input[name="actionInstall"]')) {
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
        new XWiki.widgets.Notification('$escapetool.javascript($msg.get("platform.extension.distributionWizard.uiStepPreviousUIRequestFailed"))', 'error');
      }
    });
  },

  _displayPreviousUiExtension : function(previousUiExtension) {
    var form = $('previousUi');
    var hint = new Element('div', {'class': 'xHint'}).update('$escapetool.javascript($msg.get("platform.extension.distributionWizard.uiStepPreviousUIHint"))'.escapeHTML());
    var container = new Element('div').insert(hint).insert(previousUiExtension);
    form.hide().insert({after: container});
    // Enhance the extension display.
    document.fire('xwiki:dom:updated', {elements: [container]});
    // Hack the install button to perform a fake install (only mark the extension as installed).
    var installButton = previousUiExtension.down('input[name="actionInstall"]');
    installButton.value = '$escapetool.javascript($msg.get("platform.extension.distributionWizard.uiStepPreviousUIRepairLabel"))';
    installButton.title = '$escapetool.javascript($msg.get("platform.extension.distributionWizard.uiStepPreviousUIRepairHint"))';
    installButton.name = 'actionRepairXAR';
    installButton.activate();
    // Add the form token (for CSRF protection) and the confirm hidden input (to execute the job without confirmation,
    // since the repair job doesn't have a plan job).
    var fieldSet = installButton.up('fieldset');
    fieldSet.insert({top: new Element('input', {
      type: 'hidden',
      name: 'form_token',
      value: document.head.down('meta[name="form_token"]').readAttribute('content')
    })});
    fieldSet.insert({top: new Element('input', {
      type: 'hidden',
      name: 'confirm',
      value: '1'
    })});
  },

  _onPreviousUiExtensionStatusChanged : function(status) {
    if (status == 'installed') {
      var form = $('previousUi');
      // Remove the previous UI extension display.
      form.next().remove();
      // Display the recommended UI extension.
      var next = form.next();
      while (next && next.tagName.toLowerCase() != 'form') {
        next = next.show().next();
      }
    }
  }
});

/**
 * Enhances the distribution step where the outdated extensions are upgraded.
 */
XWiki.OutdatedExtensionsStep = Class.create({
  initialize : function () {
    this.container = $('distributionWizard');
    // Listen to extension status change to be able to update the step buttons.
    document.observe('xwiki:extension:statusChanged', this._updateStepButtons.bind(this));
    // Refresh the upgrade plan job status.
    this._maybeScheduleRefresh(false);
  },

  _maybeScheduleRefresh : function(afterUpdate, timeout) {
    if (this.container.down('.xdialog-content > .ui-progress')) {
      // Disable the step buttons while the upgrade plan is being created.
      $('stepButtons').up().disable();
      this._refresh.bind(this).delay(timeout || 1);
    } else if (afterUpdate) {
      // Enhance the behaviour of the extensions.
      this.container.select('.extension-item').each(function(extension) {
        new XWiki.ExtensionBehaviour(extension);
      });
      this._updateStepButtons();
    }
  },

  _refresh : function() {
    new Ajax.Request('', {
      onSuccess: function(response) {
        this._update(response.responseText);
      }.bind(this),
      onFailure : this._maybeScheduleRefresh.bind(this, false, 10)
    });
  },

  _update : function(html) {
    var content = this.container.down('.xdialog-content');
    var progressBar = content.childElements().find(function(child) {
      return child.hasClassName('ui-progress');
    })
    if (progressBar) {
      // Remove the loading message.
      progressBar.previous().remove();
      // Insert the HTML response before the progress bar.
      progressBar.insert({before: html});
      // Remove the old upgrade log and the old progress bar.
      progressBar.next().remove();
      progressBar.remove();
      // Enhance the inserted HTML content.
      document.fire('xwiki:dom:updated', {elements: [content]});
      this._maybeScheduleRefresh(true);
    }
  },

  _updateStepButtons : function() {
    var stepButtons = $('stepButtons');
    if (!stepButtons) return;
    stepButtons = stepButtons.select('button');
    // Disable the step buttons if there is any extension loading.
    if (this.container.select('.extension-item-loading').size() > 0) {
      // Disable all step buttons.
      stepButtons.invoke('disable');
      $('prepareUpgradeLink') && $('prepareUpgradeLink').up().hide();
    } else {
      // Enable all step buttons.
      stepButtons.invoke('enable');
      $('prepareUpgradeLink') && $('prepareUpgradeLink').up().show();
      // Show the Continue button if all the invalid extensions have been fixed.
      if (this._noInvalidExtensions()) {
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
  },

  _noInvalidExtensions : function() {
    var invalidExtensionsWrapper = $('invalidExtensions');
    if (!invalidExtensionsWrapper) {
      return true;
    } else {
      var invalidExtensions = invalidExtensionsWrapper.childElements();
      return invalidExtensions.size() == invalidExtensions.filter(function(extension) {
        return extension.hasClassName('extension-item-installed');
      }).size();
    }
  }
});

function init() {
  $('extension.mainui') && new XWiki.MainUIStep();
  $('extension.outdatedextensions') && new XWiki.OutdatedExtensionsStep();
  return true;
}

// When the document is loaded, trigger the enhancements.
(XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
