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
var XWiki = (function (XWiki) {
// Start XWiki augmentation.
var widgets = XWiki.widgets = XWiki.widgets || {};

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
 */
widgets.ButtonGroup = Class.create({
  initialize : function(container) {
    this.container = container;
    this.displayInsideParent = container.hasClassName('inside');
    this._dropDownMenu = container.down('.dropdown-menu');
    this._dropDownToggle = container.down('.dropdown-toggle');
    if (this._dropDownMenu && this._dropDownToggle) {
      // Toggle the drop down menu on click.
      this._dropDownToggle.observe('click', this._onClick.bindAsEventListener(this));
      // Close the drop down menu when pressing the Escape key.
      this._dropDownToggle.observe('keydown', this._onKeyDown.bindAsEventListener(this));
      // Close the drop down menu when the toggle button looses the focus.
      this._dropDownToggle.observe('blur', this._scheduleClose.bind(this));
      this._dropDownToggle.observe('focus', this._cancelClose.bind(this));
      // Close the drop down menu when an item is clicked.
      this._dropDownMenu.observe('click', this._scheduleClose.bindAsEventListener(this));
      // Keep the drop down menu open if one of the items is focused (in order to support Tab key navigation).
      // The focus and blur events don't bubble so we have to catch them on the source element.
      this._dropDownMenu.select('a, input, button').each(function(item) {
        item.observe('blur', this._scheduleClose.bind(this));
        item.observe('focus', this._cancelClose.bind(this));
      }.bind(this));
    }
  },

  /**
   * Toggle the drop down menu.
   */
  _onClick : function(event) {
    event.stop();
    this._toggle();
  },

  /**
   * Close the drop down menu when pressing the Escape key.
   */
  _onKeyDown : function(event) {
    event.keyCode == 27 && this._toggle(false);
  },

  /**
   * Don't close the drop down menu immediately because:
   * - the focus could be moving from one item to another (Tag key navigation)
   * - in case an item is clicked we need to keep it visible for a while so that its default behaviour is executed.
   */
  _scheduleClose : function(event) {
    // In case of an item being clicked we just delay the close.
    var forceClose = event && event.type == 'click';
    // We let the focus event cancel the close if it follows immediately after the blur (e.g. when navigating through
    // the menu items using the Tab key).
    this._closing = true;
    (function() {
      (this._closing || forceClose) && this._toggle(false);
      delete this._closing;
    }).bind(this).delay(0.15);
    // NOTE: A lower delay time doesn't work well in Chrome.
  },

  /**
   * We got the focus back so no need to close the drop down menu for the moment.
   */
  _cancelClose : function() {
    this._closing = false;
  },

  _toggle: function(open) {
    this._dropDownMenu.toggleClassName('open', open);
    if (this.displayInsideParent) {
      if (this._dropDownMenu.hasClassName('open')) {
        this.container.up().setStyle({
          'height': (this.container.up().getHeight() + this._dropDownMenu.getHeight()) + 'px'
        })
      } else {
        this.container.up().setStyle({'height': ''});
      }
    }
  }
});

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
 */
widgets.DynamicButtonGroup = Class.create({
  initialize : function(container) {
    // Collect the visible buttons.
    var buttons = container.select('button, input.button, a').filter(function(button) {
      return button.offsetWidth > 0;
    });
    if (buttons.length < 2) return;

    // Unwrap the buttons.
    buttons.each(function(button) {
      button.up().hasClassName('buttonwrapper') && button.up().insert({before: button}).remove();
    });

    // Initialize the container.
    container.removeClassName('dynamic-button-group').addClassName('buttonwrapper button-group initialized');

    // Insert the drop down menu toggle.
    buttons[0].insert({after: new Element('a', {
      href: '#dropDownMenu',
      'class': 'dropdown-toggle' + (buttons[0].hasClassName('secondary') ? ' secondary' : ''),
      tabindex: 0
    }).insert(new Element('span'))});

    // Insert the drop down menu.
    var dropDownMenu = new Element('span', {'class': 'dropdown-menu'});
    for (var i = 1; i < buttons.length; i++) {
      dropDownMenu.insert(buttons[i].removeClassName('secondary'));
    }
    buttons[0].next().insert({after: dropDownMenu});

    new widgets.ButtonGroup(container);
  }
});

var init = function(event) {
  ((event && event.memo.elements) || [$('body')]).each(function(element) {
    element.select('.button-group').each(function(buttonGroup) {
      if (!buttonGroup.hasClassName('initialized')) {
        new XWiki.widgets.ButtonGroup(buttonGroup);
        buttonGroup.addClassName('initialized');
      }
    });
    element.select('.dynamic-button-group').each(function(dynamicButtonGroup) {
      new XWiki.widgets.DynamicButtonGroup(dynamicButtonGroup);
    });
  });
  return true;
};

(XWiki.domIsLoaded && init()) || document.observe("xwiki:dom:loaded", init);
document.observe('xwiki:dom:updated', init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
