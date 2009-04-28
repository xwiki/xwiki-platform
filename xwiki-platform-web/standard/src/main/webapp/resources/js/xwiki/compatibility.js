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
if (typeof XWiki.widgets == "object" && typeof XWiki.widgets.LiveTable != "klass") {
  window.ASSTable = Class.create(XWiki.widgets.LiveTable, {
	initialize: function($super, url, limit, domNode, scrollNode, filterNode, getHandler, hasFilters, action) {
      // warn developers they are using deprecated code.
      warn("window.ASSTable is deprecated since XWiki 1.9M2. Use XWiki.widgets.LiveTable instead.");

      if($("showLimits")) {
        // inject an element for pagination since the scroller has been removed.
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
	    // replace the id of the limits element by the one expected by convention by the new LiveTable widget
	    $("showLimits").id = domNode + "-limits";
      }

      if ($('scrollbar1')) {
	     // remove that annoying pseudo-scroll, the new widget support normal pagination.
	     $('scrollbar1').up("td").next().remove(); // remove the buff td
	     $('scrollbar1').up("td").remove();  // remove the td that holds the scrollbar
      }
 
      if($('table-filters')) {
         // replace the id of the filters container by the one expected by convention by the new LiveTable widget
	     $('table-filters').id = domNode + "-filters";
      }

      // Ouf, that should be all for compatibility code, now we call father initialize method of new widget.
      // Some arguments are dropped since the new signature is different.
      $super(url, domNode, getHandler);
    } 
  });
}

})();
