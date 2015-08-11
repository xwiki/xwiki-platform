require(['jquery', 'bootstrap'], function($) {
  $(document).ready(function() {
  
    // Bugfix: TODO: remove this when https://github.com/twbs/bootstrap/issues/16968 is fixed
    $('.dropdown').on('shown.bs.dropdown', function () {
      var menu = $(this).find('.dropdown-menu');
      var menuLeft = menu.offset().left;
      var menuWidth = menu.outerWidth();
      var documentWidth = $(body).outerWidth();
      if (menuLeft + menuWidth > documentWidth) {
        menu.offset({'left': documentWidth - menuWidth});
      }
    });
    
    // Show/hide the global search
    var globalSearch = $('#globalsearch');
    globalSearch.hide();
    globalSearch.removeClass('hidden');
    $('#globalsearch-trigger').click(function(event) {
      var icon = $(this).find('span');
      if (icon.hasClass('glyphicon-search')) {
        icon.removeClass('glyphicon-search');
        icon.addClass('glyphicon-remove');
        $('#globalsearch').fadeIn();
      } else {
        icon.removeClass('glyphicon-remove');
        icon.addClass('glyphicon-search');
        $('#globalsearch').fadeOut();
      }
      
      return false;
    });
  
  });
});
