var XWiki = (function (XWiki) {
// Start XWiki augmentation.
var getDocument = function(document) {
  if (!document) {
    return XWiki.currentDocument;
  } else if (typeof document == 'string') {
    var reference = XWiki.Model.resolve(document, XWiki.EntityType.DOCUMENT);
    var pageName = reference.name;
    var spaceName = reference.extractReferenceValue(XWiki.EntityType.SPACE);
    var wikiName = reference.extractReferenceValue(XWiki.EntityType.WIKI);
    return new XWiki.Document(pageName, spaceName, wikiName);
  }
  return document;
};

XWiki.DocumentLock = Class.create({
  initialize: function(document) {
    this._document = getDocument(document);

    // Unlock when we leave the page.
    var unlock = this.unlock.bind(this);
    Event.observe(window, 'unload', unlock);
    Event.observe(window, 'pagehide', unlock);

    // Unlock before logging out because afterwards we don't have rights.
    // Note that the logout action doesn't target the current document so it can't remove its lock.
    $('tmLogout') && $('tmLogout').down('a') && $('tmLogout').down('a').observe('click', unlock);

    // The page is automatically unlocked when the form is submitted.
    var markUnlocked = this.setLocked.bind(this, false);
    $$('form.withLock').each(function(form) {
      form.observe('submit', markUnlocked);
    });

    var reference = new XWiki.DocumentReference(this._document.wiki, this._document.space, this._document.page);
    XWiki.DocumentLock._instances[XWiki.Model.serialize(reference)] = this;
  },

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

  setLocked: function(locked) {
    this._locked = !!locked;
  },

  isLocked: function() {
    return this._locked;
  },

  _getURL: function(action) {
    return this._document.getURL(action, 'ajax=1&action=' + XWiki.contextaction + '&' + (XWiki.docvariant || ''));
  }
});

XWiki.DocumentLock._instances = {};
XWiki.DocumentLock.get = function(document) {
  document = getDocument(document);
  var reference = new XWiki.DocumentReference(document.wiki, document.space, document.page);
  return XWiki.DocumentLock._instances[XWiki.Model.serialize(reference)];
}

var init = function() {
  // Edit lock for the current document.
  XWiki.EditLock = new XWiki.DocumentLock();

  // Lock the current document.
  XWiki.EditLock.lock();

  return true;
};

(XWiki.domIsLoaded && init()) || document.observe('xwiki:dom:loaded', init);
// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
