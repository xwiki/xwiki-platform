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
var XWiki = (function(XWiki) {
  // Start XWiki augmentation.
  var widgets = XWiki.widgets = XWiki.widgets || {};
  /**
   * KeyboardAccessibleTabList class.
   * Provides keyboard support for horizontal tab lists.
   */
  widgets.KeyboardAccessibleTabList = Class.create({
    initialize: function (tabListNode) {
      this.tabListNode = tabListNode;
      this.tabs = Array.from(this.tabListNode.querySelectorAll('[role=tab]'));
      this.firstTab = null;
      this.lastTab = null;
      var selectedTab;
      this.tabpanels = [];
      for (var i = 0; i < this.tabs.length; i += 1) {
        var tab = this.tabs[i];
        var tabpanel = document.getElementById(tab.getAttribute('aria-controls'));
        this.tabpanels.push(tabpanel);
        tab.observe('keyup', this.onKeyUp.bindAsEventListener(this));
        tab.observe('click', this.onClick.bindAsEventListener(this));
        if (!selectedTab && tab.getAttribute('aria-selected') == 'true') {
          selectedTab = tab;
        }
      }
      this.setSelectedTab(selectedTab);
      if (this.tabs.length != 0) {
        this.firstTab = this.tabs[0];
        this.lastTab = this.tabs[this.tabs.length - 1];
      }
    },

    setSelectedTab: function (currentTab) {
      this.tabs.forEach((tab, index) => {
        if (currentTab === tab) {
          tab.removeAttribute('tabindex');
        } else {
          tab.tabIndex = -1;
        }
      })
    },

    moveFocusToTab: function (currentTab) {
      currentTab.focus();
    },

    moveFocusToTabShifted: function (currentTab, deltaIndex) {
      this.moveFocusToTab(this.tabs[this.tabs.indexOf(currentTab) + deltaIndex]);
    },

    moveFocusToPreviousTab: function (currentTab) {
      if (currentTab === this.firstTab) {
        this.moveFocusToTab(this.lastTab);
      } else {
        this.moveFocusToTabShifted(currentTab, -1);
      }
    },

    moveFocusToNextTab: function (currentTab) {
      if (currentTab === this.lastTab) {
        this.moveFocusToTab(this.firstTab);
      } else {
        this.moveFocusToTabShifted(currentTab, +1);
      }
    },

    /* EVENT HANDLERS */
    onKeyUp: function(event) {
      let target = event.currentTarget;
      let controlOfTab = true;
      let key = even.keyCode;
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
      }
    },

    onClick: function(event) {
      this.setSelectedTab(event.currentTarget);
    }
  });
  // End XWiki augmentation.
  return XWiki;
}(XWiki || {}));