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
/**
 * Contains all backward-compatibility code for deprecated methods and objects.
 * This is somehow similar to the server side compatibility aspects.
 * All usage of deprecated code should log warnings in the console to help developers
 * moving to new code.
 */
(function(){

/**
 * If possible, log a warning message to the console.
 * If not, does nothing.
 */
function warn(message){
  if (typeof console != "undefined" && typeof console.warn == "function") {
    console.warn(message);
  }
}

/**
 * Deprecated since 2.6RC2
 */
if (typeof XWiki.widgets == 'object' && typeof XWiki.widgets.FullScreen == 'function') {
  XWiki.editors = XWiki.editors || {};
  XWiki.editors.FullScreenEditing = Class.create(XWiki.widgets.FullScreen, {
    initialize: function($super){
      warn("XWiki.editors.FullScreenEditing is deprecated since XWiki 2.6RC2. Use XWiki.widgets.FullScreen instead.");
      $super();
    }
  });
}


/**
 * _xwk is an old namespace for XWiki code that was optional
 * Deprecated since 2.6RC1
 */
if (window.useXWKns) {
  warn("_xwk namespace is deprecated since XWiki 2.6RC1. Use the XWiki namespace instead.");
  if (typeof _xwk == "undefined") {
    window._xwk = new Object();
  }
} else {
  window._xwk = window;
}

/**
 * Deprecated since 2.6RC1
 */
_xwk.ajaxSuggest = Class.create(XWiki.widgets.Suggest, {
  initialize: function($super){
    warn("ajaxSuggest is deprecated since XWiki 2.6RC1. Use XWiki.widgets.Suggest instead.");
    var args = $A(arguments)
    args.shift();
    $super.apply( _xwk, args );
  }
});

/**
 * Deprecated since 1.9M2
 */
window.displayDocExtra = XWiki.displayDocExtra.wrap(
  function(){
    warn("window.displayDocExtra is deprecated since XWiki 1.9M2. Use XWiki.displayDocExtra instead.");
    var args = $A(arguments), proceed = args.shift();
    return proceed.apply(window, args);
  }
);

/**
 * window.ASSTable
 * Deprecated since 1.9M2
 */
if (typeof XWiki.widgets == "object" && typeof XWiki.widgets.LiveTable == "function") {
  window.ASSTable = Class.create(XWiki.widgets.LiveTable, {
      initialize: function($super, url, limit, domNode, scrollNode, filterNode, getHandler, hasFilters, action) {
      // warn developers they are using deprecated code.
      warn("window.ASSTable is deprecated since XWiki 1.9M2. Use XWiki.widgets.LiveTable instead.");

      if($("showLimits")) {
        // inject an element for pagination since the scroller has been removed.
        if($("showLimits").up("tr")) {
          $("showLimits").up("tr").insert({'after':
        new Element("tr").update(
              new Element("td").update(
                new Element("div", {
                  'id': domNode + "-pagination",
                  'class': "xwiki-grid-pagination-content"
                })
              )
            )
          });
        }
        // replace the id of the limits element by the one expected by convention by the new LiveTable widget
        $("showLimits").id = domNode + "-limits";
      }

      if ($('scrollbar1') && $('scrollbar1').up("td")) {
         // if it present, remove that annoying pseudo-scroll, the new widget support normal pagination.
         if($('scrollbar1').up("td").next()) {
           $('scrollbar1').up("td").next().remove(); // remove the buff td
         }
         $('scrollbar1').up("td").remove();  // remove the td that holds the scrollbar
      }

      if($('table-filters')) {
         // replace the id of the filters container by the one expected by convention by the new LiveTable widget
         $('table-filters').id = domNode + "-filters";
      }

      // Ouf, that should be all for compatibility code, now we call father initialize method of new widget.
      // Some arguments are dropped since the new signature is different.
      $super(url, domNode, getHandler, {"action" : action});
    }
  });
}

/**
 * Hide the fieldset inside the given form.
 *
 * @param form  {element} The form element.
 *
 * Deprecated since 2.6 RC1
 */
window.hideForm = function(form){
    warn("window.hideForm is deprecated since XWiki 2.6RC1. Use a CSS selector + Element#toggleClassName instead.");
    form.getElementsByTagName("fieldset").item(0).className = "collapsed";
}

/**
 * Hide the fieldset inside the given form if visible, show it if it's not.
 *
 * @param form  {element} The form element.
 *
 * Deprecated since 2.6 RC1
 */
window.toggleForm= function(form){
    warn("window.toggleForm is deprecated since XWiki 2.6RC1. Use a CSS selector + Element#toggleClassName instead.");
    var fieldset = form.getElementsByTagName("fieldset").item(0);
    if(fieldset.className == "collapsed"){
        fieldset.className = "expanded";
    }
    else{
        fieldset.className = "collapsed";
    }
}

/**
 * Deprecated since 2.6RC1
 */
window.createCookie = XWiki.cookies.create.wrap(
  function(){
    warn("window.createCookie is deprecated since XWiki 2.6RC1. Use XWiki.cookies.create instead.");
    var args = $A(arguments), proceed = args.shift();
    return proceed.apply(window, args);
  }
);

/**
 * Deprecated since 2.6RC1
 */
window.readCookie = XWiki.cookies.read.wrap(
  function(){
    warn("window.readCookie is deprecated since XWiki 2.6RC1. Use XWiki.cookies.read instead.");
    var args = $A(arguments), proceed = args.shift();
    return proceed.apply(window, args);
  }
);

/**
 * Deprecated since 2.6RC1
 */
window.eraseCookie = XWiki.cookies.erase.wrap(
  function(){
    warn("window.eraseCookie is deprecated since XWiki 2.6RC1. Use XWiki.cookies.erase instead.");
    var args = $A(arguments), proceed = args.shift();
    return proceed.apply(window, args);
  }
);

/**
 * Deprecated since 2.6RC1
 */
window.togglePanelVisibility = XWiki.togglePanelVisibility.wrap(
  function(){
    warn("window.togglePanelVisibility is deprecated since XWiki 2.6RC1. Use XWiki.togglePanelVisibility instead.");
    var args = $A(arguments), proceed = args.shift();
    return proceed.apply(window, args);
  }
);

/**
 * Deprecated since 4.1M1
 */
window.cancelEdit = function(){
  warn("window.cancelEdit is deprecated since XWiki 4.1M1. Use XWiki.EditLock.unlock instead.");
  XWiki.EditLock.unlock();
}

/**
 * Deprecated since 4.1M1
 */
window.lockEdit = function(){
  warn("window.lockEdit is deprecated since XWiki 4.1M1. Use XWiki.EditLock.lock instead.");
  XWiki.EditLock.lock();
}

/**
 * Deprecated since 4.1M1
 */
window.cancelCancelEdit = function(){
  warn("window.cancelCancelEdit is deprecated since XWiki 4.1M1. Use XWiki.EditLock.setLocked(false) instead.");
  XWiki.EditLock.setLocked(false);
}

XWiki.resource = XWiki.resource || {};
Object.extend(XWiki.resource, {
  /**
   * Extract the name of the wiki from a resource name. Examples: returns "xwiki" with "xwiki:Main.WebHome",
   * returns null with "Main.WebHome".
   *
   * @deprecated since 4.2M1, use {@code XWiki.resource.get(name).wiki} instead
   */
  getWikiFromResourceName: function(name) {
    if (name.include(XWiki.constants.wikiSpaceSeparator)) {
      return name.substring(0, name.indexOf(XWiki.constants.wikiSpaceSeparator));
    }
    return null;
  },

  /**
   * Extract the name of the space from a resource name. Examples: returns "Main" with "xwiki:Main.WebHome",
   * returns "Main" with "Main.WebHome", returns null with "WebHome".
   *
   * @deprecated since 4.2M1, use {@code XWiki.resource.get(name).space} instead
   */
  getSpaceFromResourceName: function(name) {
    var originalName = name;
    // Remove wiki if any.
    if (name.include(XWiki.constants.wikiSpaceSeparator)) {
      name = name.substring(name.indexOf(XWiki.constants.wikiSpaceSeparator) + 1, name.length);
    }
    // If the resource contains an attachment, make sure the dot is not part of the attachment name.
    if (name.include(XWiki.constants.spacePageSeparator)) {
      if (name.include(XWiki.constants.pageAttachmentSeparator) && name.indexOf(XWiki.constants.spacePageSeparator)
          > name.indexOf(XWiki.constants.pageAttachmentSeparator)) {
        return null;
      }
      return name.substring(0, name.indexOf(XWiki.constants.spacePageSeparator));
    }
    // If the resource name looks like "xwiki:Main" we return "Main".
    if (originalName.include(XWiki.constants.wikiSpaceSeparator)
        && !originalName.include(XWiki.constants.pageAttachmentSeparator)
        && !originalName.include(XWiki.constants.anchorSeparator)) {
      return name;
    }
    return null;
  },

  /**
   * Extract the name of the page from a resource name. Examples: returns "WebHome" with "xwiki:Main.WebHome",
   * returns "WebHome" with "Main.WebHome", returns null with "xwiki:Main".
   *
   * @deprecated since 4.2M1, use {@code XWiki.resource.get(name).name} instead
   */
  getNameFromResourceName: function(name) {
    var originalName = name;
    // Remove wiki if any.
    if (name.include(XWiki.constants.wikiSpaceSeparator)) {
      name = name.substring(name.indexOf(XWiki.constants.wikiSpaceSeparator) + 1, name.length);
    }
    // remove attachment if any.
    if (name.include(XWiki.constants.pageAttachmentSeparator)) {
      name = name.substring(0, name.indexOf(XWiki.constants.pageAttachmentSeparator));
    }
    // remove anchor if any.
    if (name.include(XWiki.constants.anchorSeparator)) {
      name = name.substring(0, name.indexOf(XWiki.constants.anchorSeparator));
    }
    if (name.include(XWiki.constants.spacePageSeparator)) {
      return name.substring(name.indexOf(XWiki.constants.spacePageSeparator) + 1, name.length);
    } else {
      if (originalName.include(XWiki.constants.wikiSpaceSeparator)) {
        // If the resource name looks like "xwiki:Main" it does not contain page info.
        return null;
      } else {
        return name;
      }
    }
  },

  /**
   * Extract the name of the attachment from a resource name. Examples: returns "test.zip" with
   * "Main.WebHome@test.zip", returns null with "Main.WebHome".
   *
   * @deprecated since 4.2M1, use {@code XWiki.resource.get(name).attachment} instead
   */
  getAttachmentFromResourceName: function(name) {
    if (name.include(XWiki.constants.pageAttachmentSeparator)) {
      return name.substring(name.indexOf(XWiki.constants.pageAttachmentSeparator) + 1, name.length);
    }
    return null;
  },

  /**
   * Extract the name of the anchor from a resource name. Examples: returns "Comments" with
   * "Main.WebHome#Comments", returns null with "Main.WebHome".
   *
   * @deprecated since 4.2M1, use {@code XWiki.resource.get(name).anchor} instead
   */
  getAnchorFromResourceName: function(name) {
    if (name.include(XWiki.constants.anchorSeparator)) {
      return name.substring(name.indexOf(XWiki.constants.anchorSeparator) + 1, name.length);
    }
    return null;
  }
});

XWiki.constants = XWiki.constants || {};
Object.extend(XWiki.constants, {
  /**
   * Character that separates wiki from space in a page fullName (example: the colon in xwiki:Main.WebHome).
   *
   * @deprecated since 4.2M1, your code shouldn't be aware of this separator, use {@code XWiki.Model}
   *             to resolve/serialize entity references
   */
  wikiSpaceSeparator: ":",

  /**
   * Character that separates space from page in a page fullName (example: the dot in xwiki:Main.WebHome).
   *
   * @deprecated since 4.2M1, your code shouldn't be aware of this separator, use {@code XWiki.Model}
   *             to resolve/serialize entity references
   */
  spacePageSeparator: ".",

  /**
   * Character that separates page from attachment in an attachment fullName (example: the @ in xwiki:Main.WebHome@Archive.tgz).
   *
   * @deprecated since 4.2M1, your code shouldn't be aware of this separator, use {@code XWiki.Model}
   *             to resolve/serialize entity references
   */
  pageAttachmentSeparator: "@"
});

/**
 * Add some deprecated <meta> tags in the header of the page so that old script can still work. It is added via the
 * JavaScript since these <meta> tags are invalid with HTML5 and we want to have valid static HTML code.
 *
 * Note: we do not use require JS here because this script have to be executed very quickly, before the execution of the
 * scripts that need these meta tags.
 */
(function(){
  // Maybe the meta tags already exist
  if ($$("meta[name='document']").length > 0) {
    return;
  }

  // Get the DOM elements we need
  var html = $$('html')[0];
  var head = $$('head')[0];

  // Function that creates a meta tag with the value taken from the new HTML data-* attrribute
  var addMetaTag = function(name, value) {
    head.insert(new Element('meta', {'name': name, 'content': html.readAttribute('data-xwiki-'+value)}));
  };

  // Add the new meta tags
  addMetaTag('document',   'document');
  addMetaTag('wiki',       'wiki');
  addMetaTag('space',      'space');
  addMetaTag('page',       'page');
  addMetaTag('version',    'version');
  addMetaTag('restURL',    'rest-url');
  addMetaTag('form_token', 'form-token');

  // Add the language
  head.insert(new Element('meta', {'name': 'language', 'content': html.readAttribute('lang')}));
})();

/**
 * Use to hold the list of blacklistedSpaces: this concept has been removed in favor of the hidden attribute in pages.
 * @deprecated since 16.0RC1
 */
XWiki.blacklistedSpaces = XWiki.blacklistedSpaces || [];

})();
