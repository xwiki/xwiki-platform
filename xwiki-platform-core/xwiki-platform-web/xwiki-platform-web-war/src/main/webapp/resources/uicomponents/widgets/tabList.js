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
class TabsManual {
  constructor(tabListNode) {
    this.tabListNode = tabListNode;
    this.tabs = Array.from(this.tabListNode.querySelectorAll('[role=tab]'));
    this.firstTab = null;
    this.lastTab = null;
    var selectedTab;
    this.tabpanels = [];
    for (var i = 0; i < this.tabs.length; i += 1) {
      var tab = this.tabs[i];
      var tabpanel = $('#'+tab.getAttribute('aria-controls'));
      this.tabpanels.push(tabpanel);
      tab.observe('keydown', this.onKeydown.bind(this));
      tab.observe('click', this.onClick.bind(this));
      if (!selectedTab && tab.getAttribute('aria-selected')=='true') {
        selectedTab = tab;
      }
    }
    this.setSelectedTab(selectedTab);
    if (this.tabs.length>0) {
      this.firstTab = this.tabs[0];
      this.lastTab = this.tabs[this.tabs.length-1];
    }


  }
  setSelectedTab(currentTab)
  {
    for (var i = 0; i < this.tabs.length; i += 1) {
      var tab = this.tabs[i];
      if (currentTab === tab) {
        tab.removeAttribute('tabindex');
        this.tabpanels[i].classList.remove('hidden');
      } else {
        tab.tabIndex = -1;
        this.tabpanels[i].classList.add('hidden');
      }
    }
  }
  moveFocusToTab(currentTab)
  {
    currentTab.focus();
  }

  moveFocusToPreviousTab(currentTab)
  {
    var index;

    if (currentTab === this.firstTab) {
      this.moveFocusToTab(this.lastTab);
    } else {
      index = this.tabs.indexOf(currentTab);
      this.moveFocusToTab(this.tabs[index - 1]);
    }
  }

  moveFocusToNextTab(currentTab)
  {
    var index;

    if (currentTab === this.lastTab) {
      this.moveFocusToTab(this.firstTab);
    } else {
      index = this.tabs.indexOf(currentTab);
      this.moveFocusToTab(this.tabs[index + 1]);
    }
  }

  /* EVENT HANDLERS */

  onKeydown(event)
  {
    var target = event.currentTarget,
        flag = false;

    switch (event.key) {
      case 'ArrowLeft':
        this.moveFocusToPreviousTab(target);
        flag = true;
        break;

      case 'ArrowRight':
        this.moveFocusToNextTab(target);
        flag = true;
        break;

      case 'Home':
        this.moveFocusToTab(this.firstTab);
        flag = true;
        break;

      case 'End':
        this.moveFocusToTab(this.lastTab);
        flag = true;
        break;

      case ' ':
        target.click();
        flag = true;
        break;

      default:
        break;
    }

    if (flag) {
      event.stopPropagation();
      event.preventDefault();
    }
  }

  // Since this example uses buttons for the tabs, the click onr also is activated
  // with the space and enter keys
  onClick(event)
  {
    this.setSelectedTab(event.currentTarget);
  }
}

var tablists = document.querySelectorAll('[role="tablist"]');
for (var i = 0; i < tablists.length; i++) {
  new TabsManual(tablists[i]);
}