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

    // When the drawer is close, collapse sub items
    $(body).on('drawer.closed', function() {
      $('.drawer-menu-sub-item').removeClass('in').attr('aria-expanded', 'false');
    });

    // Activate the popover when hovering the Translate button.
    var translateButton = $('#tmTranslate [data-toggle="popover"]');
    translateButton.attr('title', translateButton.attr('data-title')).popover();
  });
});

require(['jquery', 'iscroll', 'drawer'], function($, IScroll) {
  // Unfortunately drawer doesn't declare the dependency on iscroll and expects it to be defined as a global variable.
  window.IScroll = IScroll;
  $(function() {
    // Note that the 'drawer-open' and 'drawer-close' CSS classes are added before the open and close animations end
    // which prevents us from using them in automated tests. We need something more reliable so we listen to
    // 'drawer.opened' and 'drawer.closed' events and add our own markers.
    var tmDrawer = $('#tmDrawer');
    $('.drawer-nav').closest('body').drawer().on('drawer.opened', function(event) {
      $('#tmDrawerActivator').attr('aria-expanded', 'true');
      //Focus the first focusable element in tmDrawer.
      tmDrawer.find('button, a, input:not([type="hidden"]), select, textarea, [tabindex]:not([tabindex="-1"])')
        .first()
        .focus();
      let closeDrawer = () => $('.drawer-nav').closest('body').drawer('close');
      // Drawer can be closed by pressing the ESC key.
      tmDrawer.on('keydown', function (event) {
        if (event.key === 'Escape') {
          closeDrawer();
        }
      });
      // It can also be closed by getting the focus out of it.
      document.getElementById('tmDrawer').addEventListener('focusout',
        function (event) {
        if(!this.contains(event.relatedTarget)) {
          closeDrawer();
        }
      });
    }).on('drawer.closed', function(event) {
      $('#tmDrawerActivator').attr('aria-expanded', 'false');
      // We remove the listeners
      tmDrawer.off('keydown focusout');
    });
  });
});
