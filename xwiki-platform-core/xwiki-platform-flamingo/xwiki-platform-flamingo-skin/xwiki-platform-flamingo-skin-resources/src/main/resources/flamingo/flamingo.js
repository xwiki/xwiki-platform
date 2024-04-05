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
require(['jquery', 'bootstrap'], function($) {
  $(function() {

    // Fix the bad location of the dropdown menu when the trigger is close to the end of the screen.
    // See: http://jira.xwiki.org/browse/XWIKI-12609
    $(document).on('shown.bs.dropdown', function (event) {
      var toggle    = $(event.relatedTarget);
      var menu      = toggle.next('.dropdown-menu');
      // The menu might be not found if it is not located where it is expected.
      if (menu.length > 0) {
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
      }
    });
    // Activate the popover when hovering the Translate button.
    var translateButton = $('#tmTranslate [data-toggle="popover"]');
    translateButton.attr('title', translateButton.attr('data-title')).popover();
  });
});

/*
  Handle the behavior of all drawers on the page.
  In order to create a drawer that is compatible with this script, two elements are necessary:
  * A drawer opener button, that will have:
   ** a reference to the drawer container in its attribute 'aria-control'
   ** the class 'drawer-opener'
  * A drawer container, that will have:
   ** a unique ID
   ** the class 'drawer-nav' (for style) 
   ** an accessible name 
   ** The content to display inside.
  In addition, the drawer can contain a `.drawer-close` button that will close the drawer on click.
  It's expected for the drawer containers not to be nested in each other, this might lead to inconsistencies 
  when closing them with the ESC key, and inconsistencies on the behavior of subitems in different drawers.
  For an example of drawer creation, see #tmDrawerActivator and #tmDrawer.
 */
require(['jquery'], function($) {
  $(document).ready(function() {
    $('.drawer-opener').each(function (index) {
      // Setting up the drawer.
      let drawerOpener = $(this);
      let drawerId = drawerOpener.attr('aria-controls');
      let drawerContainer = $(document.getElementById(drawerId));

      let openDrawer = () => drawerContainer.trigger('drawer' + index + '.opened');
      let closeDrawer = () => drawerContainer.trigger('drawer' + index + '.closed');
      drawerOpener.on('click', openDrawer);
      // Close drawer when clicking the backdrop (or any element outside of the drawer itself).
      drawerContainer.on('click', (event) => {
        let drawerzone = event.target.getBoundingClientRect();
        if (event.target === drawerContainer.get(0) &&
            (drawerzone.left > event.clientX || drawerzone.right < event.clientX ||
            drawerzone.top > event.clientY || drawerzone.bottom < event.clientY)) {
          closeDrawer();
        }
      });
      // Close drawer when clicking on a close button inside it
      drawerContainer.find('.drawer-close').on('click', closeDrawer);
      
      drawerContainer.on('drawer' + index + '.opened', function (event) {
        // We use the drawer-transitioning class to make sure the transition to 
        // slide in is not shortcut when showing the modal
        drawerContainer.addClass('drawer-transitioning');
        drawerContainer.get(0).showModal();
        drawerContainer.removeClass('drawer-transitioning');
        // The drawer can be closed by pressing the ESC key
        drawerContainer.on('keydown.drawer' + index + 'Close', function (event) {
          if (event.key === 'Escape') {
            closeDrawer();
          }
        });
        // At last, we indicate the new state of the drawer on the opener
        drawerOpener.attr('aria-expanded', 'true');
      }).on('drawer' + index + '.closed', function (event) {
        // When the drawer is closed, collapse sub items
        drawerContainer.find('.drawer-menu-sub-item').removeClass('in').attr('aria-expanded', 'false');

        function waitTransition() {
          drawerContainer.get(0).close();
          drawerContainer.removeClass('drawer-transitioning');
          drawerContainer.get(0).removeEventListener('transitionend', waitTransition);
          // At last, we indicate the new state of the drawer on the opener
          drawerOpener.attr('aria-expanded', 'false');
        }

        // We use the drawer-transitioning class to give time for the hide modal transition to play.
        drawerContainer.addClass('drawer-transitioning');
        drawerContainer.get(0).addEventListener('transitionend', waitTransition);
        // We remove the listener that was created when the drawer opened up
        drawerContainer.off('keydown.drawer' + index + 'Close');
      });
    });
  });
});

