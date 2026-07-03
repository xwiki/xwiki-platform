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
var getDocument = function(document) {
  if (!document) {
    return XWiki.currentDocument;
  } else if (typeof document == 'string') {
    var reference = XWiki.Model.resolve(document, XWiki.EntityType.DOCUMENT);
    return new XWiki.Document(reference);
  }
  return document;
};

XWiki.DocumentLock = Class.create({
  initialize: function(document) {
    this._document = getDocument(document);

    // Unlock when we leave the page.
    var unlock = this.unlock.bind(this);
    // We may need to look into 'visibilitychange' event in the future, as per
    // https://www.igvita.com/2015/11/20/dont-lose-user-and-app-state-use-page-visibility/
    // in order to cover the mobile usage, but then we need to decide what to do when the user switches browser tabs.
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

    XWiki.DocumentLock._instances[XWiki.Model.serialize(this._document.documentReference)] = this;
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
      navigator.sendBeacon(this._getURL('cancel'));
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

  // Lock the current document if we're editing.
  if (XWiki.editor) {
    XWiki.EditLock.lock();
  }

  return true;
};

(XWiki.domIsLoaded && init()) || document.observe('xwiki:dom:loaded', init);
// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
