var XWiki = (function (XWiki) {
// Start XWiki augmentation.
/**
 * Enhances the distribution step where the main UI is installed, upgraded or downgraded.
 */
XWiki.MainUIStep = Class.create({
  initialize : function () {
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
