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

// The real locale of the current document translation. This must match the real locale computed by the document API
// used by the in-place editor, otherwise the same lock would be represented by two different lock instances.
const getDefaultLocale = () => new URLSearchParams(XWiki.docvariant || '').get('language') ||
  document.documentElement.lang;

// Use the same format as LocalizedStringEntityReferenceSerializer, for consistency with the server side. Note that
// this format doesn't escape the parenthesis, so a page named "Page(en)" gets the same key as the "en" translation of
// a page named "Page". We favored consistency here, but we may have to deal with this edge case later.
const getKey = (document, locale) => `${XWiki.Model.serialize(document.documentReference)}(${locale})`;

XWiki.DocumentLock = Class.create({
  /**
   * @param document the document to lock, defaulting to the current document; can be either an XWiki.Document instance
   *          or a serialized document reference
   * @param locale the locale of the document translation to lock, defaulting to the locale of the current document
   *          translation; the empty string targets the default translation
   */
  initialize: function(document, locale) {
    this._document = getDocument(document);
    this._locale = locale ?? getDefaultLocale();
    this._options = {};
    // The functions to call in order to revert what we do below, see #destroy().
    this._cleanupCallbacks = [];

    // There must be a single lock instance per document translation, otherwise each of them would send its own unlock
    // request when the page is unloaded, and all but the first would fail because the lock has already been removed.
    XWiki.DocumentLock.get(this._document, this._locale)?.destroy();

    // Unlock when we leave the page.
    const unlock = this.unlock.bind(this);
    // We may need to look into 'visibilitychange' event in the future, as per
    // https://www.igvita.com/2015/11/20/dont-lose-user-and-app-state-use-page-visibility/
    // in order to cover the mobile usage, but then we need to decide what to do when the user switches browser tabs.
    Event.observe(window, 'unload', unlock);
    Event.observe(window, 'pagehide', unlock);
    this._cleanupCallbacks.push(() => {
      Event.stopObserving(window, 'unload', unlock);
      Event.stopObserving(window, 'pagehide', unlock);
    });

    // Unlock before logging out because afterwards we don't have rights.
    // Note that the logout action doesn't target the current document so it can't remove its lock.
    const logoutLink = $('tmLogout')?.down('a');
    logoutLink?.observe('click', unlock);
    this._cleanupCallbacks.push(() => logoutLink?.stopObserving('click', unlock));

    // The page is automatically unlocked when the form is submitted. We pin the lock options, otherwise the submit
    // event would be passed as options by the event listener.
    const markUnlocked = this.setLocked.bind(this, false, undefined);
    $$('form.withLock').forEach(form => {
      form.observe('submit', markUnlocked);
      this._cleanupCallbacks.push(() => form.stopObserving('submit', markUnlocked));
    });

    const key = getKey(this._document, this._locale);
    XWiki.DocumentLock._instances[key] = this;
    this._cleanupCallbacks.push(() => delete XWiki.DocumentLock._instances[key]);
  },

  /**
   * Stops listening to the events that trigger the unlock and forgets this instance. Note that this doesn't remove the
   * lock, it only stops this instance from removing it.
   */
  destroy: function() {
    this._cleanupCallbacks.forEach(cleanup => cleanup());
    this._cleanupCallbacks = [];
  },

  /**
   * Locks the document, unless we already hold the lock.
   *
   * @param options the lock options, see {@link #setLocked}
   */
  lock: function(options) {
    if (!this._locked) {
      this.setLocked(true, options);
      new Ajax.Request(this._getURL('lock'), {method: 'get'});
    }
  },

  unlock: function() {
    if (this._locked) {
      this._locked = false;
      navigator.sendBeacon(this._getURL('cancel'));
    }
  },

  /**
   * Marks this document as locked or unlocked, without sending any request. Use this when the lock is acquired or
   * released by someone else (e.g. the in-place editor sends its own lock request, because it needs to handle the lock
   * confirmation), or to force the next call to {@link #lock} to send the lock request again (e.g. when someone else
   * removed the lock in the mean time).
   *
   * @param locked whether the document translation is locked or not
   * @param options the lock options, holding the edit action the lock is taken for ({@code action}); they are left
   *          unchanged when not specified
   */
  setLocked: function(locked, options) {
    this._locked = !!locked;
    if (options) {
      this._options = options;
    }
  },

  isLocked: function() {
    return this._locked;
  },

  /**
   * @param action the action to build the URL for (e.g. 'lock' or 'cancel')
   * @return the URL to call in order to perform the given action on the locked document translation
   */
  _getURL: function(action) {
    const parameters = new URLSearchParams({
      ajax: 1,
      // The edit action the lock is taken for, which is not the action from the URL we build here.
      action: this._options.action || XWiki.contextaction
    });
    if (this._locale) {
      parameters.set('language', this._locale);
    }
    return this._document.getURL(action, parameters.toString());
  }
});

XWiki.DocumentLock._instances = {};

/**
 * @param document the document to look for, defaulting to the current document
 * @param locale the locale of the document translation to look for, defaulting to the locale of the current document
 *          translation
 * @return the lock instance associated with the given document translation, if any
 */
XWiki.DocumentLock.get = function(document, locale) {
  return XWiki.DocumentLock._instances[getKey(getDocument(document), locale ?? getDefaultLocale())];
};

/**
 * @param document the document to lock, defaulting to the current document
 * @param locale the locale of the document translation to lock, defaulting to the locale of the current document
 *          translation
 * @return the lock instance associated with the given document translation, creating it if needed
 */
XWiki.DocumentLock.getOrCreate = function(document, locale) {
  return XWiki.DocumentLock.get(document, locale) || new XWiki.DocumentLock(document, locale);
};

var init = function() {
  // Edit lock for the current document.
  XWiki.EditLock = XWiki.DocumentLock.getOrCreate();

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
