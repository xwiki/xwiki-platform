require(['jquery'], function($) {
  // Collapsible diff summary.
  $('div.diff-summary-item').each(function() {
    var details = $(this).next('ul');
    if (details.size() > 0) {
      details.hide();
      $(this).find('a').click(function(event) {
        event.preventDefault();
        details.toggle();
      });
    }
  });
});
