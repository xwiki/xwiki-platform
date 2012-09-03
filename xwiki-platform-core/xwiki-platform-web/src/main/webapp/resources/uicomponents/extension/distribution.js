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

function init() {
  var stepContainer = $('extension.mainui');
  stepContainer && new XWiki.MainUIStep(stepContainer);
  return true;
}

// When the document is loaded, trigger the enhancements.
(XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
