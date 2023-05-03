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
define('xwiki-tabList', ['jquery'], function($) {
  class KeyboardAccessibleTabList {
    /**
     * Instantiates a keyboard navigable tabList.
     * @param tabListNode is the root of the list, with role = "tablist"
     * All of its children should be tab elements whose "aria-controls" attribute is properly set.
     * The state of the tabList can be initialized with one element already open. This element's tab should have the
     * class "active" on it.
     */
    constructor(tabListNode) {
      this.tabListNode = tabListNode;
      // Only the elements that are properly annotated are considered as tabs.
      // The correct use of this class is with children that are all properly annotated.
      this.tabs = Array.from(this.tabListNode.find('[role=tab]'));
      this.firstTab = null;
      this.lastTab = null;
      let selectedTab;
      this.tabpanels = [];
      for (var i = 0; i < this.tabs.length; i += 1) {
        var tab = this.tabs[i];
        var tabpanel = document.getElementById(tab.getAttribute('aria-controls'));
        this.tabpanels.push(tabpanel);
        tab.addEventListener('keydown', this.onKeyDown.bindAsEventListener(this));
        tab.addEventListener('click', this.onClick.bindAsEventListener(this));
        if (!selectedTab && $(tab).hasClass('active')) {
          selectedTab = tab;
        }
      }
      if (this.tabs.length != 0) {
        if (!selectedTab) {
          selectedTab = this.tabs[0];
        }
        this.setSelectedTab(selectedTab);
        this.firstTab = this.tabs[0];
        this.lastTab = this.tabs[this.tabs.length - 1];
      }
    }

    /**
     * Updates the attributes of the tab-list to reflect a change in the opened tab.
     * @param newlyOpenedTab
     */
    setSelectedTab(newlyOpenedTab) {
      newlyOpenedTab.removeAttribute('tabindex');
      newlyOpenedTab.setAttribute('aria-selected', 'true');
      this.tabs.forEach((tab) => {
        if (newlyOpenedTab !== tab) {
          tab.tabIndex = -1;
          tab.setAttribute('aria-selected', 'false');
        }
      })
    }

    /**
     * Move user focus to a new tab in the list.
     * @param newlyFocusedTab is the new tab to focus.
     */
    moveFocusToTab(newlyFocusedTab) {
      newlyFocusedTab.focus();
    }

    /**
     * Move user focus to a new tab in the list.
     * @param currentTab is the current tab.
     * @param deltaIndex is the difference in index from the current tab to the new tab to focus
     */
    moveFocusToTabShifted(currentTab, deltaIndex) {
      this.moveFocusToTab(this.tabs[this.tabs.indexOf(currentTab) + deltaIndex]);
    }

    moveFocusToPreviousTab(currentTab) {
      if (currentTab === this.firstTab) {
        // Loop around when the start of the list is reached.
        this.moveFocusToTab(this.lastTab);
      } else {
        this.moveFocusToTabShifted(currentTab, -1);
      }
    }

    moveFocusToNextTab(currentTab) {
      if (currentTab === this.lastTab) {
        // Loop around when the end of the list is reached.
        this.moveFocusToTab(this.firstTab);
      } else {
        this.moveFocusToTabShifted(currentTab, +1);
      }
    }

    /**
     * Event handler for keydown.
     * This switch contains the
     * @param event
     */
    onKeyDown(event) {
      let target = event.currentTarget;
      let controlOfTab = true;
      let key = event.keyCode;
      switch (key) {
        case Event.KEY_LEFT:
          this.moveFocusToPreviousTab(target);
          break;
        case Event.KEY_RIGHT:
          this.moveFocusToNextTab(target);
          break;
        case Event.KEY_HOME:
          this.moveFocusToTab(this.firstTab);
          break;
        case Event.KEY_END:
          this.moveFocusToTab(this.lastTab);
          break;
        default:
          controlOfTab = false;
          break;
      }
      if (controlOfTab) {
        // When a tab focus event is observed, we stop propagation of this event to avoid
        // multiple interactions through one input. E.g. This prevents the screen from scrolling right and left when
        // navigating through the tabs with right/left arrows
        event.stopPropagation();
        event.preventDefault();
      }
    }

    /**
     * Event handler for the click event.
     * @param event
     */
    onClick(event) {
      this.setSelectedTab(event.currentTarget);
      this.moveFocusToTab(event.currentTarget);
    }
  };
  return KeyboardAccessibleTabList;
});
