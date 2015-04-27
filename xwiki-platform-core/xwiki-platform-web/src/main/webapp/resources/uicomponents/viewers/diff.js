require(['jquery', 'xwiki-events-bridge'], function($) {
  // Collapsible diff summary.
  var enhanceDiffSummaryItem = function() {
    var details = $(this).next('ul');
    if (details.size() > 0) {
      details.hide();
      $(this).find('a').click(function(event) {
        event.preventDefault();
        details.toggle();
      });
    }
  };

  var enhanceDiffSummaryItems = function(elements) {
    $(elements).find('div.diff-summary-item').each(enhanceDiffSummaryItem);
  }

  $(document).on('xwiki:dom:updated', function(event, data) {
    enhanceDiffSummaryItems(data.elements);
  });

  enhanceDiffSummaryItems([document.body]);
});
