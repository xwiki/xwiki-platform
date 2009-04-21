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
window.displayDocExtra = XWiki.displayDocExtra;
window.displayDocExtra = window.displayDocExtra.wrap(
  function(){
    warn("window.displayDocExtra is deprecated since XWiki 1.9M2. Use XWiki.displayDocExtra instead.");
    var args = $A(arguments), proceed = args.shift();
    return proceed.apply(window, args);
  }
);

})();
