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
    _xwk = new Object();
  }
} else {
  _xwk = window;
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

})();
