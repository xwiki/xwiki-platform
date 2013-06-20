// jQuery sets global variables even when the 'define' function is present. The reason it doesn't follow the AMD
// recommendation (no global variables) is because it wants to support plugins that are not defined (yet) as modules. In
// our case the global $ variable conflicts with Prototype.js so we are forced to create this proxy.
// See http://stackoverflow.com/questions/8767777/jquery-1-7-amd-requirejs-and-global-scope
define(['jquery'], function(jQuery) {
  // Restore the '$' global variable but keep the 'jQuery' one because some plugins (like those from Bootstrap) use it.
  return jQuery.noConflict(false);
});