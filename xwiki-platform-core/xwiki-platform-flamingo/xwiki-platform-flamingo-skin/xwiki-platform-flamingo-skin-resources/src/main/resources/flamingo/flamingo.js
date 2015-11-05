require(['jquery', 'bootstrap'], function($) {
  $(document).ready(function() {

    // Fix the bad location of the dropdown menu when the trigger is close to the end of the screen.
    // See: http://jira.xwiki.org/browse/XWIKI-12609
    $(document).on('shown.bs.dropdown', function (event, data) {
      var toggle    = $(data.relatedTarget);
      var menu      = toggle.next('.dropdown-menu');
      var menuWidth = menu.outerWidth();
      // if the right corner of the menu is after the end of the screen
      if (menu.offset().left + menuWidth > $(document.body).outerWidth()) {
        // we put that corner at the same place than the toggle's right corner
        var newLocation = toggle.offset().left + toggle.outerWidth() - menuWidth;
        // but don't put it negative, or the user will have to scroll to the left!
        if (newLocation < 0) {
          newLocation = 0;
        }
        menu.offset({'left': newLocation});
      }
    });

    // When the drawer is close, collapse sub items
    $(body).on('drawer.closed', function() {
      $('.drawer-menu-sub-item').removeClass('in').attr('aria-expanded', 'false');
    });

  });
});
