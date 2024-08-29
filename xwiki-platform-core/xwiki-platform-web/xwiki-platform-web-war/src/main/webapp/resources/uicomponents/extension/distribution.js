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
var XWiki = (function (XWiki) {
// Start XWiki augmentation.
/**
 * Enhances the distribution step where the flavor or the default UI is installed, upgraded or downgraded.
 */
XWiki.FlavorOrDefaultUIStep = Class.create({
  initialize : function (isFlavorStep) {
    this.isFlavorStep = isFlavorStep;
    document.observe('xwiki:extension:statusChanged', this._onExtensionStatusChanged.bindAsEventListener(this));

    // Disable the install button if no flavor is selected (and do it every time the picker is updated)
    require(['jquery'], function ($) {
      $('.xwiki-flavor-picker').on('xwiki:flavorpicker:updated', function() {
        var selectedItems = $(this).find('input[name="flavor"]:checked').length;
        $('input[name="installFlavor"]').prop('disabled', selectedItems === 0);
      });
    });
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
      if (this.isFlavorStep || (extension.getId() == '$services.distribution.getUIExtensionId().id'
          && extension.getVersion() == '$services.distribution.getUIExtensionId().version.value')) {
        this._onDefaultUiExtensionStatusChanged(stepButtons, status);
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
        // Update the step buttons if the updated element contains extensions or if it represents the extension updater.
        (element.down('.extension-item') || element.hasClassName('extensionUpdater')) && this._updateStepButtons();
      }.bind(this));
    }.bindAsEventListener(this));
  },

  _updateStepButtons : function() {
    var stepButtons = $('stepButtons');
    if (!stepButtons) return;
    stepButtons = stepButtons.select('button');
    // Disable the step buttons if there is any extension loading.
    if (this.container.down('.extension-item.extension-item-loading') || this.container.down('.job-log-item-loading')) {
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
      invalidExtensionsCount += invalidExtensions.length;
      invalidExtensionsFixedCount += invalidExtensions.filter(function(extension) {
        return extension.hasClassName('extension-item-installed');
      }).length;
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

var PreviousUIForm = Class.create({
  initialize : function (form) {
    this.form = form;
    this.versionList = form.down('select.versions');
    this.versionInput = form.down('input[name="previousUIVersion"]');
    this.idInput = form.down('input[name="previousUIId"]');
    this.recommendedUI = form.next('.recommendedUI').hide();
    this.upgradeQuestion = form.previous('form.upgradeQuestion');

    document.observe('xwiki:extension:statusChanged',
      this._onPreviousUIExtensionStatusChanged.bindAsEventListener(this));

    // Enhance the form actions.
    form.down('.button').observe('click', this._resolvePreviousUIExtension.bindAsEventListener(this));
    var secondaryButton = form.down('.button.secondary');
    secondaryButton.up().removeClassName('hidden');
    secondaryButton.observe('click', this._hidePreviousUIForm.bindAsEventListener(this));

    // Simplify the way the previous UI is specified.
    this.versionList && this._enhancePreviousUIInput();

    // Enhance and show the upgrade question.
    this.upgradeQuestion && this._enhanceUpgradeQuestion();
  },

  _enhancePreviousUIInput : function() {
    // The element used to toggle advanced input.
    var versionEditButton = new Element('input', {
      type: 'image',
      'class': 'icon',
      src: '$xwiki.getSkinFile("icons/silk/pencil.png")',
      alt: "$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIAdvancedInputHint'))",
      title: "$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIAdvancedInputHint'))"
    });
    var idEditButton = versionEditButton.cloneNode();
    // Display the version list by default instead of the version input. Allow the users to enter a different version.
    this.versionList.insert({'after': versionEditButton}).up('dd').removeClassName('hidden')
      .addClassName('versionSelector').previous().removeClassName('hidden');
    this.versionInput.up('dd').hide().previous().hide();
    // Hide the id input and its hint by default because we auto-complete the id based on the selected version.
    this.idInput.hide().up('dd').previous().down('.xHint').hide();
    // Display a pencil next to the id value to let the user change it.
    this.idInput.insert({after: idEditButton}).insert({after: new Element('span')});
    // Hide the id label and value by default. Display it when the user selects a version.
    this.idInput.up('dd').hide().previous().hide();
    this.versionList.observe('change', this._onSelectPreviousUIVersion.bind(this));
    // Allow advanced input.
    versionEditButton.observe('click', this._switchToAdvancedPreviousUIInput.bindAsEventListener(this));
    idEditButton.observe('click', this._switchToAdvancedPreviousUIInput.bindAsEventListener(this));
  },

  _enhanceUpgradeQuestion : function() {
    this.upgradeQuestion.removeClassName('hidden').down('.button').observe('click', function(event) {
      event.stop();
      this.upgradeQuestion.hide();
      // Make sure the form is enabled because the browser can cache the state of the form fields (see XWIKI-9717).
      this.form.enable().show().focusFirstElement();
    }.bindAsEventListener(this)).activate();
    this.upgradeQuestion.down('.button.secondary').observe('click', this._hidePreviousUIForm.bindAsEventListener(this));
    // Hide the form.
    this.form.hide()
  },

  _onSelectPreviousUIVersion : function() {
    var selectedVersion = this.versionList.options[this.versionList.selectedIndex];
    // Fill the version input with the selected value in case the user decides to modify it.
    this.versionInput.value = selectedVersion.value;
    // Show the extension id that corresponds to the selected version.
    this.idInput.next().update(selectedVersion.title).up('dd').show().previous().show();
    // Fill the id input with the extension id that corresponds to the selected version.
    this.idInput.value = selectedVersion.title;
  },

  _switchToAdvancedPreviousUIInput : function(event) {
    event.stop();
    // Hide the version list and its edit button.
    this.versionList.up('dd').hide().previous().hide();
    // Make sure the id and its edit button are hidden.
    this.idInput.next().hide().next().hide();
    // Show the version input and its hint.
    this.versionInput.up('dd').show().previous().show();
    // Show the id input and its hint.
    this.idInput.show().up('dd').show().previous().show().down('.xHint').show();
    // Focus the right input depending on which edit button was clicked.
    event.element().previous() == this.versionList ? this.versionInput.activate() : this.idInput.activate();
  },

  _hidePreviousUIForm : function(event) {
    event && event.stop();
    this.form.hide();
    this.upgradeQuestion && this.upgradeQuestion.hide();
    this.recommendedUI.show();
  },

  _resolvePreviousUIExtension : function(event) {
    event.stop();
    var formData = this.form.serialize(true);
    new Ajax.Request(this.form.action, {
      parameters: {
        extensionId: formData.previousUIId,
        extensionVersion: formData.previousUIVersion,
        extensionNamespace: 'wiki:' + formData.wiki,
        hideExtensionDetails: true
      },
      onCreate: function() {
        this.form.disable();
        // Remove the message corresponding to a failed search.
        var message = this.form.down('.buttons').next();
        message && message.remove();
      }.bind(this),
      onSuccess: function(response) {
        var container = new Element('div').update(response.responseText);
        var previousUIExtension = container.down('.extension-item');
        if (previousUIExtension) {
          if (previousUIExtension.down('button[name="extensionAction"][value="install"]')) {
            // The specified previous UI is not installed. We have to update the extension index.
            this.previousUIExtensionId = {
              id: formData.previousUIId,
              version: formData.previousUIVersion
            };
            this._displayPreviousUIExtension(previousUIExtension);
          } else {
            // The specified previous UI is probably already installed. Just show the recommended UI.
            this._hidePreviousUIForm();
          }
        } else {
          // Display the received message.
          this.form.enable().insert(container);
        }
      }.bind(this),
      on0: function(response) {
        response.request.options.onFailure(response);
      },
      onFailure: function() {
        this.form.enable();
        new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIRequestFailed'))", 'error');
      }.bind(this)
    });
  },

  _displayPreviousUIExtension : function(previousUIExtension) {
    var hint = new Element('div', {'class': 'xHint'}).update("$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIHint'))".escapeHTML());
    var container = new Element('div').insert(hint).insert(previousUIExtension);
    this.form.hide().insert({after: container});
    // Enhance the extension display.
    document.fire('xwiki:dom:updated', {elements: [container]});
    // Hack the install button to perform a fake install (only mark the extension as installed).
    var installButton = previousUIExtension.down('button[name="extensionAction"][value="install"]');
    installButton.update("$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIRepairLabel'))".escapeHTML());
    installButton.title = "$escapetool.javascript($services.localization.render('platform.extension.distributionWizard.uiStepPreviousUIRepairHint'))";
    installButton.value = 'repairXAR';
    installButton.activate();
    // Add the form token (for CSRF protection) to execute the job without confirmation (without a job plan).
    installButton.insert({after: new Element('input', {
      type: 'hidden',
      name: 'form_token',
      // FIXME: Use the xwiki-meta module to get the form token when we switch to jQuery/RequireJS.
      value: document.documentElement.getAttribute('data-xwiki-form-token')
    })});
  },

  _onPreviousUIExtensionStatusChanged : function(event) {
    var extension = event.memo.extension;
    var status = extension.getStatus();
    // The previous UI can have the status 'installed-invalid' if one of its dependencies are not met anymore.
    if (this.previousUIExtensionId && extension.getId() == this.previousUIExtensionId.id
        && extension.getVersion() == this.previousUIExtensionId.version && status && status.startsWith('installed')) {
      // Remove the previous UI extension display.
      this.form.next().remove();
      // Display the default UI extension.
      for (var next = this.form.next(); next; next = next.show().next()) {};
      // Refresh the display of the recommended UI extension so that we get the upgrade button.
      var recommendedUIExtension = this.recommendedUI.down('.extension-item');
      recommendedUIExtension && recommendedUIExtension._extensionBehaviour.refresh({hideExtensionDetails: true});
    }
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

  // Enhance the form that repairs the previous UI.
  $('body').select('form.previousUI').each(function(previousUIForm) {
    new PreviousUIForm(previousUIForm);
  });

  $('extension.flavor') && new XWiki.FlavorOrDefaultUIStep(true);
  $('extension.defaultui') && new XWiki.FlavorOrDefaultUIStep(false);
  $('extension.defaultui.wikis') && new WikisStep();
  $('extension.outdatedextensions') && new XWiki.OutdatedExtensionsStep();

  return true;
}

// When the document is loaded, trigger the enhancements.
(XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
