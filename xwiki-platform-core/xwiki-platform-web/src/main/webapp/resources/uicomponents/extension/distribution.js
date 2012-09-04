var XWiki = (function (XWiki) {
// Start XWiki augmentation.
/**
 * Enhances the distribution step where the main UI is installed, upgraded or downgraded.
 */
XWiki.MainUIStep = Class.create({
  initialize : function (container) {
    this.container = container;
    document.observe('xwiki:extension:statusChanged', this._onExtensionStatusChanged.bindAsEventListener(this));
  },

  /**
   * Checks if the main UI extension has been installed and enables the continue button.
   */
  _onExtensionStatusChanged : function(event) {
    if (event.memo.extension.getId() != '$services.distribution.getUIExtensionId().id') {
      return;
    }
    var status = event.memo.extension.getStatus();
    var stepButtons = $('stepButtons').select('button');
    if (status == 'loading') {
      // Disable all step buttons.
      stepButtons.invoke('disable');
    } else {
      // Enable all step buttons.
      stepButtons.invoke('enable');
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
  }
});

/**
 * Enhances the distribution step where the outdated extensions are upgraded.
 */
XWiki.OutdatedExtensionsStep = Class.create({
  initialize : function (container) {
    this.container = container;
    // Listen to extension status change to be able to update the step buttons.
    document.observe('xwiki:extension:statusChanged', this._updateStepButtons.bind(this));
    // Refresh the upgrade plan job status.
    this._maybeScheduleRefresh(false);
  },

  _maybeScheduleRefresh : function(afterUpdate) {
    if (this.container.down('.xdialog-content > .ui-progress')) {
      // Disable the step buttons while the upgrade plan is being created.
      $('stepButtons').up().disable();
      this._refresh.bind(this).delay(1);
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
      onFailure : this._maybeScheduleRefresh.bind(this, false)
    });
  },

  _update : function(html) {
    var content = this.container.down('.xdialog-content');
    var form = $('stepButtons').up();
    content.update(html);
    document.fire('xwiki:dom:updated', {elements: [content]});
    content.insert(form);
    this._maybeScheduleRefresh(true);
  },

  _updateStepButtons : function() {
    var stepButtons = $('stepButtons');
    if (!stepButtons) return;
    stepButtons = stepButtons.select('button');
    var prepareUpgradeButton = $('prepareUpgradeButton');
    prepareUpgradeButton && stepButtons.push(prepareUpgradeButton);
    // Disable the step buttons if there is any extension loading.
    if (this.container.select('.extension-item-loading').size() > 0) {
      // Disable all step buttons.
      stepButtons.invoke('disable');
    } else {
      // Enable all step buttons.
      stepButtons.invoke('enable');
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
  var stepContainer = $('extension.mainui');
  stepContainer && new XWiki.MainUIStep(stepContainer);
  stepContainer = $('extension.outdatedextensions');
  stepContainer && new XWiki.OutdatedExtensionsStep(stepContainer);
  return true;
}

// When the document is loaded, trigger the enhancements.
(XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
