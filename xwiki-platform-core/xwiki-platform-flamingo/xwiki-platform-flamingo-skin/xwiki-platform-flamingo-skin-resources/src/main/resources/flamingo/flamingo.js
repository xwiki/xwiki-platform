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
  * A drawer toggler button, that will have:
   ** a reference to the drawer container in its attribute 'aria-control'
   ** the class 'drawer-toggle'
  * A drawer container, that will have:
   ** a unique ID
   ** the class 'drawer-nav' (mostly for style) 
   ** the class 'closed' if the drawer is supposed to start in a closed state.
   ** a label 
   ** The content to display inside. It's asserted that at least one element in this content can receive focus.
  For an example of drawer creation, see #tmDrawerActivator and #tmDrawer.
 */
require(['jquery'], function($) {
  // The overlay is the same whatever the drawer opened.
  const drawerOverlay = $(".drawer-overlay");
  $('.drawer-toggle').each(function(index) {
    // Setting up the drawer.
    let drawerContainerToggler = $(this);
    let drawerId = drawerContainerToggler.attr('aria-controls');
    let drawerContainer = $('#'+drawerId);
    let focusableElements = drawerContainer.find('button, a, input:not([type="hidden"]), ' +
     'select, textarea, [tabindex]:not([tabindex="-1"])');

    // Note that the 'drawer-open' and 'drawer-close' CSS classes are added before the open and close animations end
    // which prevents us from using them in automated tests. We need something more reliable so we listen to
    // 'drawer1.opened' and 'drawer1.closed' events and add our own markers.
    let openDrawer = () => drawerContainer.trigger('drawer'+index+'.opened');
    let closeDrawer = () => drawerContainer.trigger('drawer'+index+'.closed');
    drawerContainerToggler.on('click', openDrawer);
    drawerOverlay.on('click', closeDrawer);

    drawerContainer.on('drawer'+index+'.opened', function(event) {
      drawerContainerToggler.attr('aria-expanded', 'true');
      // We update the state of the drawer (using setAttribute since it's faster)
      drawerContainer.get(0)
        .setAttribute('class', 'drawer-nav opened');
      // We need to set a timeout for the class update to finish properly before trying to focus an element that would
      // have no visibility before the class change. We use an interval so that the focus is moved no matter the
      // performance of the client.
      let focusInterval = setInterval(()=>{
        if (drawerContainer.hasClass('opened') && focusableElements.length !== 0) {
          focusableElements.first().trigger('focus');
          clearInterval(focusInterval);
          focusInterval = null;
        }
      }, 50);
      // The drawer can be closed by pressing the ESC key.
      $("body").on('keydown', function (event) {
        if (event.key === 'Escape') {
          closeDrawer();
        }
      });
      // The drawer can be closed by setting focus outside of it
      focusableElements.on('focusout', function (event) {
        if (event.relatedTarget != null && event.relatedTarget.closest('#'+drawerId) == null) {
          closeDrawer();
        }
      });
    }).on('drawer'+index+'.closed', function(event) {
      // We update the state of the drawer
      drawerContainer
        .removeClass('opened')
        .addClass('closed');
      drawerContainerToggler.attr('aria-expanded', 'false');
      // We remove the listeners that were created when the drawer opened up
      $("body").off('keydown');
      focusableElements.off('focusout');
    });
    
    // When the drawer is closed, collapse sub items
    $(body).on('drawer'+index+'.closed', function() {
      $('.drawer-menu-sub-item').removeClass('in').attr('aria-expanded', 'false');
    });
  });
});

