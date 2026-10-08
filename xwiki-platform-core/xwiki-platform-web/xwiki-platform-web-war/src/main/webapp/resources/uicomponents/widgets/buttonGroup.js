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
require(['jquery', 'xwiki-events-bridge'], function($) {
const XWiki = window.XWiki = window.XWiki || {};
const widgets = XWiki.widgets = XWiki.widgets || {};
const l10n = {
  "core.widgets.buttonGroup.dropDown.toggle.hint" :
    $jsontool.serialize($services.localization.render('core.widgets.buttonGroup.dropDown.toggle.hint')),
};

/**
 * A static button group. Both the drop-down toggle and the drop-down menu must be present in the DOM document. This
 * widget works with JavaScript disabled, provided you specify an id on the drop-down menu as indicated in the example
 * below. The only enhancement the JavaScript code brings is the ability to close the drop-down menu when the Escape key
 * is pressed or when the user clicks outside the drop-down menu.
 *
 * Example:
 *
 * <span class="buttonwrapper button-group">
 *   <button>Action</button><a href="#foo" class="dropdown-toggle" tabindex="0"><span/></a>
 *   <span id="foo" class="dropdown-menu">
 *     <button>First item</button>
 *     <input type="submit" value="Second item" class="button" />
 *     <a href="#third">Third item</a>
 *   </span>
 * </span>
 *
 * Note that this class can be extended using Prototype.js' Class.create(XWiki.widgets.ButtonGroup, {...}). This is why
 * the constructor only delegates to the initialize method (Prototype.js calls only the initialize method when creating
 * an instance of a subclass).
 */
class ButtonGroup {
  constructor(...args) {
    this.initialize(...args);
  }

  initialize(container) {
    this.container = container;
    this.displayInsideParent = container.classList.contains('inside');
    this._dropDownMenu = container.querySelector('.dropdown-menu');
    this._dropDownToggle = container.querySelector('.dropdown-toggle');
    if (this._dropDownMenu && this._dropDownToggle) {
      // Toggle the drop down menu on click.
      this._dropDownToggle.addEventListener('click', event => this._onClick(event));
      // Close the drop down menu when pressing the Escape key.
      this._dropDownToggle.addEventListener('keydown', event => this._onKeyDown(event));
      // Close the drop down menu when the toggle button looses the focus.
      this._dropDownToggle.addEventListener('blur', () => this._scheduleClose());
      this._dropDownToggle.addEventListener('focus', () => this._cancelClose());
      // Close the drop down menu when an item is clicked.
      this._dropDownMenu.addEventListener('click', event => this._scheduleClose(event));
      // Keep the drop down menu open if one of the items is focused (in order to support Tab key navigation).
      // The focus and blur events don't bubble so we have to catch them on the source element.
      this._dropDownMenu.querySelectorAll('a, input, button').forEach(item => {
        item.addEventListener('blur', () => this._scheduleClose());
        item.addEventListener('focus', () => this._cancelClose());
      });
    }
  }

  /**
   * Toggle the drop down menu.
   */
  _onClick(event) {
    event.preventDefault();
    event.stopPropagation();
    this._toggle();
  }

  /**
   * Close the drop down menu when pressing the Escape key.
   */
  _onKeyDown(event) {
    event.key === 'Escape' && this._toggle(false);
  }

  /**
   * Don't close the drop down menu immediately because:
   * - the focus could be moving from one item to another (Tag key navigation)
   * - in case an item is clicked we need to keep it visible for a while so that its default behaviour is executed.
   */
  _scheduleClose(event) {
    // In case of an item being clicked we just delay the close.
    const forceClose = event?.type == 'click';
    // We let the focus event cancel the close if it follows immediately after the blur (e.g. when navigating through
    // the menu items using the Tab key).
    this._closing = true;
    setTimeout(() => {
      (this._closing || forceClose) && this._toggle(false);
      delete this._closing;
    }, 150);
    // NOTE: A lower delay time doesn't work well in Chrome.
  }

  /**
   * We got the focus back so no need to close the drop down menu for the moment.
   */
  _cancelClose() {
    this._closing = false;
  }

  _toggle(open) {
    this._dropDownMenu.classList.toggle('open', open);
    if (this.displayInsideParent) {
      const parent = this.container.parentElement;
      if (this._dropDownMenu.classList.contains('open')) {
        parent.style.height = (parent.offsetHeight + this._dropDownMenu.offsetHeight) + 'px';
      } else {
        parent.style.height = '';
      }
    }
  }
}

/** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
ButtonGroup.subclasses = [];

widgets.ButtonGroup = ButtonGroup;

/**
 * A dynamic button group. This widget looks for all the buttons inside a 'dynamic-button-group' container and creates
 * the drop-down toggle and drop-down menu dynamically if there are at least two buttons found. If the first button is
 * secondary then the entire group is displayed as secondary.
 *
 * Example:
 *
 * <span class="dynamic-button-group">
 *   <span class="buttonwrapper">
 *     <button>One</button>
 *   </span>
 *   <span class="buttonwrapper">
 *     <input type="submit" class="button secondary" value="Two" />
 *   </span>
 *   <span class="buttonwrapper">
 *     <a href="#three" class="secondary">Three</a>
 *   </span>
 * </span>
 *
 * Note that this class can be extended using Prototype.js' Class.create(XWiki.widgets.DynamicButtonGroup, {...}). This
 * is why the constructor only delegates to the initialize method (Prototype.js calls only the initialize method when
 * creating an instance of a subclass).
 */
class DynamicButtonGroup {
  constructor(...args) {
    this.initialize(...args);
  }

  initialize(container) {
    // Collect the visible buttons.
    const buttons = Array.from(container.querySelectorAll('button, input.button, a'))
      .filter(button => button.offsetWidth > 0);
    if (buttons.length < 2) return;

    // Unwrap the buttons.
    buttons.forEach(button => {
      button.parentElement.classList.contains('buttonwrapper') && button.parentElement.replaceWith(button);
    });

    // Initialize the container.
    container.classList.remove('dynamic-button-group');
    container.classList.add('buttonwrapper', 'button-group', 'initialized');

    // Insert the dropdown menu toggle.
    const dropDownToggle = document.createElement('a');
    dropDownToggle.href = '#dropDownMenu';
    dropDownToggle.className = 'dropdown-toggle' + (buttons[0].classList.contains('secondary') ? ' secondary' : '');
    dropDownToggle.tabIndex = 0;
    dropDownToggle.innerHTML = "<span class='caret'></span><span class='sr-only'>"
      + l10n['core.widgets.buttonGroup.dropDown.toggle.hint'] + "</span>";
    buttons[0].after(dropDownToggle);

    // Insert the drop down menu.
    const dropDownMenu = document.createElement('span');
    dropDownMenu.className = 'dropdown-menu';
    for (const button of buttons.slice(1)) {
      button.classList.remove('secondary');
      dropDownMenu.append(button);
    }
    dropDownToggle.after(dropDownMenu);

    new widgets.ButtonGroup(container);
  }
}

/** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
DynamicButtonGroup.subclasses = [];

widgets.DynamicButtonGroup = DynamicButtonGroup;

const init = function(event, data) {
  for (const element of (data?.elements || [document.body])) {
    element.querySelectorAll('.button-group').forEach(buttonGroup => {
      if (!buttonGroup.classList.contains('initialized')) {
        new XWiki.widgets.ButtonGroup(buttonGroup);
        buttonGroup.classList.add('initialized');
      }
    });
    element.querySelectorAll('.dynamic-button-group').forEach(dynamicButtonGroup => {
      new XWiki.widgets.DynamicButtonGroup(dynamicButtonGroup);
    });
  }
};

$(init);
$(document).on('xwiki:dom:updated', init);
});
