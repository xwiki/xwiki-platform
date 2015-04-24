define('jQueryNoConflict', ['jquery'], function($) {
  $.noConflict();
  return $;
});
