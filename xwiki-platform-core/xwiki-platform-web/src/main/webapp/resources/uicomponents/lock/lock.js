var XWiki = (function (XWiki) {
// Start XWiki augmentation.
XWiki.EditLock = {
  lock: function() {
    if (!this._locked) {
      this._locked = true;
      new Ajax.Request(this._getURL('lock'), {method: 'get'});
    }
  },
  unlock: function() {
    if (this._locked) {
      this._locked = false;
      new Ajax.Request(this._getURL('cancel'), {
        method: 'get',
        // Keep the request synchronous because otherwise the page can unload before the request is sent.
        // See https://developer.mozilla.org/en/DOM/XMLHttpRequest/Synchronous_and_Asynchronous_Requests#Irreplaceability_of_the_synchronous_use
        asynchronous: false
      });
    }
  },
  setLocked : function(locked) {
    this._locked = !!locked;
  },
  isLocked : function() {
    return this._locked;
  },
  _getURL : function(action) {
    return XWiki.currentDocument.getURL(action, 'ajax=1&action=' + XWiki.contextaction + '&' + (XWiki.docvariant || ''));
  }
};

function init() {
  // Lock the current page.
  XWiki.EditLock.lock();

  // Unlock when we leave the page.
  var unlock = XWiki.EditLock.unlock.bind(XWiki.EditLock);
  Event.observe(window, 'unload', unlock);
  Event.observe(window, 'pagehide', unlock);

  // Unlock before logging out because afterwards we don't have rights.
  // Note that the logout action doesn't target the current document so it can't remove its lock.
  $('tmLogout').down('a').observe('click', unlock);

  // The page is automatically unlocked when the form is submitted.
  var markUnlocked = XWiki.EditLock.setLocked.bind(XWiki.EditLock, false);
  $$('.withLock').each(function(form) {
    form.observe('submit', markUnlocked);
  });

  return true;
}

(XWiki.domIsLoaded && init()) || document.observe('xwiki:dom:loaded', init);
// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
