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
    constructor(tabListNode) {
      this.tabListNode = tabListNode;
      this.tabs = Array.from(this.tabListNode.find('[role=tab]'));
      this.firstTab = null;
      this.lastTab = null;
      var selectedTab;
      this.tabpanels = [];
      for (var i = 0; i < this.tabs.length; i += 1) {
        var tab = this.tabs[i];
        var tabpanel = document.getElementById(tab.getAttribute('aria-controls'));
        this.tabpanels.push(tabpanel);
        tab.observe('keydown', this.onKeyDown.bindAsEventListener(this));
        tab.observe('click', this.onClick.bindAsEventListener(this));
        if (!selectedTab && $(tab).hasClass('active')) {
          selectedTab = tab;
        }
      }
      if (this.tabs.length != 0) {
        if (!selectedTab) {
          selectedTab=this.tabs[0];
        }
        this.setSelectedTab(selectedTab);
        this.firstTab = this.tabs[0];
        this.lastTab = this.tabs[this.tabs.length - 1];
      }
    }

    setSelectedTab(currentTab) {
      this.tabs.forEach((tab, index) => {
        if (currentTab === tab) {
          tab.removeAttribute('tabindex');
          tab.setAttribute('aria-selected', 'true');
        } else {
          tab.tabIndex = -1;
          tab.setAttribute('aria-selected', 'false');
        }
      })
    }

    moveFocusToTab(currentTab) {
      currentTab.focus();
    }

    moveFocusToTabShifted(currentTab, deltaIndex) {
      this.moveFocusToTab(this.tabs[this.tabs.indexOf(currentTab) + deltaIndex]);
    }

    moveFocusToPreviousTab(currentTab) {
      if (currentTab === this.firstTab) {
        this.moveFocusToTab(this.lastTab);
      } else {
        this.moveFocusToTabShifted(currentTab, -1);
      }
    }

    moveFocusToNextTab(currentTab) {
      if (currentTab === this.lastTab) {
        this.moveFocusToTab(this.firstTab);
      } else {
        this.moveFocusToTabShifted(currentTab, +1);
      }
    }

    /* EVENT HANDLERS */
    onKeyDown(event) {
      let target = event.currentTarget;
      let controlOfTab = true;
      let key = event.keyCode;
      const KEY_SPACE = 32;
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
        case KEY_SPACE:
          target.click();
          break;
        default:
          controlOfTab = false;
          break;
      }
      if (controlOfTab) {
        event.stopPropagation();
        event.preventDefault();
      }
    }

    onClick(event) {
      this.setSelectedTab(event.currentTarget);
    }
  };
  return KeyboardAccessibleTabList;
});
