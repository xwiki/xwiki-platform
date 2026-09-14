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

/**
 * Makes a list of items reorderable without dragging, by adding a move up button and a move down button to each item.
 * Both buttons move the item with a single click or tap, and both also answer the up and down arrow keys once focused,
 * so that a series of moves can be performed without leaving the button.
 *
 * Lists that can also be reordered by dragging and dropping keep their own drag and drop implementation and pass the
 * same persistence callback to it, so that the two paths stay in sync.
 */
define('xwiki-reorder-controls', ['jquery'], function ($) {
  'use strict';

  const liveRegionId = 'reorder-controls-live-region';

  // How long the live region is left empty before the announcement is set again. Screen readers announce a region
  // whose content changed, so the same text has to be removed and set back to be announced twice in a row.
  const liveRegionResetDelay = 100;

  const offsets = {
    'up': -1,
    'down': 1
  };

  // The announcement waiting to be set in the live region, so that a new one can replace it.
  let pendingAnnouncement;

  /**
   * @return {jQuery} the region used to announce the moves, created on the first call
   */
  function getLiveRegion() {
    let liveRegion = $('#' + liveRegionId);
    if (!liveRegion.length) {
      liveRegion = $('<div></div>').attr({
        'id': liveRegionId,
        'aria-live': 'polite'
      }).addClass('sr-only').appendTo(document.body);
    }
    return liveRegion;
  }

  /**
   * Announces a text to the screen readers, and does nothing when there is nothing to announce.
   *
   * @param {string} [text] the text to announce
   */
  function announce(text) {
    if (!text) {
      return;
    }
    const liveRegion = getLiveRegion();
    liveRegion.text('');
    // A move performed while an announcement is still waiting replaces it, so that holding an arrow key down
    // announces the position the item ended up at instead of every position it went through.
    clearTimeout(pendingAnnouncement);
    pendingAnnouncement = setTimeout(function () {
      liveRegion.text(text);
    }, liveRegionResetDelay);
  }

  /**
   * @param {jQuery} button the button the user is operating
   * @param {Object} options the options the button was created with
   * @param {string} direction either up or down
   */
  function moveItem(button, options, direction) {
    const item = button.closest(options.item);
    // The reordered items are the siblings of the moved item that the caller declared as reorderable. Items that are
    // not displayed are taken into account, because the drag and drop path moves the item among them too.
    const items = item.parent().children(options.item);
    const oldIndex = items.index(item);
    const offset = offsets[direction];
    const newIndex = oldIndex + offset;
    if (oldIndex < 0) {
      return;
    }
    if (newIndex < 0 || newIndex >= items.length) {
      // The item is already at the end of the list it is moving towards. This is announced, because a keyboard user
      // has no other way to tell that the button or the key press was taken into account.
      announce(options.announceBoundary?.(item, direction));
      return;
    }
    if (offset < 0) {
      item.insertBefore(items.eq(newIndex));
    } else {
      item.insertAfter(items.eq(newIndex));
    }
    // Moving the item in the DOM moves the button with it, which makes some browsers drop the focus, so we set it back
    // on the button in order to let the user perform several moves in a row.
    button.trigger('focus');
    options.onMove?.(item, oldIndex, newIndex);
    announce(options.announce?.(item, newIndex, items.length));
  }

  /**
   * @param {jQuery} item the item the button moves
   * @param {Object} options the widget options
   * @param {string} direction either up or down
   * @return {jQuery} the button moving the given item in the given direction
   */
  function createButton(item, options, direction) {
    const labels = options.labels(item, direction);
    const button = $('<button></button>').attr({
      'type': 'button',
      'title': labels.title
    }).addClass('reorder-control reorder-control-' + direction).addClass(options.buttonClass);
    $('<span></span>').attr('role', 'presentation').html(options.icons[direction]).appendTo(button);
    $('<span></span>').addClass('sr-only').text(labels.text).appendTo(button);
    button.on('click', function (event) {
      // The button sits among the other actions of the item, whose container may react to a click of its own.
      event.stopPropagation();
      moveItem(button, options, direction);
    });
    button.on('keydown', function (event) {
      // Both buttons answer both arrow keys, so that an item moved one position too far can be moved back without
      // moving the focus to the other button.
      if (event.key === 'ArrowUp') {
        moveItem(button, options, 'up');
      } else if (event.key === 'ArrowDown') {
        moveItem(button, options, 'down');
      } else {
        return;
      }
      // Prevent the arrow keys from scrolling the page while the user is reordering the list.
      event.preventDefault();
    });
    return button;
  }

  /**
   * Adds a move up button and a move down button to each reorderable item found in the given scope. Items that already
   * have them are left untouched, so that the widget can be applied again after new items have been added to the list.
   *
   * @param {jQuery|Element|string} scope an element that is either a reorderable item itself or an ancestor of the
   *          reorderable items to enhance
   * @param {Object} options the widget options:
   *          <dl>
   *            <dt>item</dt><dd>the selector matching a reorderable item; the items it matches are expected to be
   *              siblings, since an item moves among the children of its own parent</dd>
   *            <dt>buttonContainer</dt><dd>the selector, relative to an item, of the element the buttons are appended
   *              to</dd>
   *            <dt>buttonClass</dt><dd>the CSS classes to set on both buttons</dd>
   *            <dt>icons</dt><dd>the HTML of the icon displayed inside each button, as an object with an up and a down
   *              entry</dd>
   *            <dt>labels</dt><dd>a callback, called with an item and a direction, returning the accessible name of the
   *              button as text and its tooltip as title</dd>
   *            <dt>onMove</dt><dd>an optional callback, called with the moved item and its old and new indexes, that
   *              persists the new order</dd>
   *            <dt>announce</dt><dd>an optional callback, called with the moved item, its new index and the number of
   *              items, that returns the text announced to screen readers after a move</dd>
   *            <dt>announceBoundary</dt><dd>an optional callback, called with the item and the direction, that returns
   *              the text announced to screen readers when the item is already at the end of the list it is moving
   *              towards</dd>
   *          </dl>
   */
  return function (scope, options) {
    // The region is created before the first move, because a region that is inserted and filled in the same task is
    // not announced.
    getLiveRegion();
    $(scope).find(options.item).addBack(options.item).each(function () {
      const item = $(this);
      // Only the first match is used, so that a nested list does not get the buttons of its ancestor item.
      const buttonContainer = item.find(options.buttonContainer).first();
      if (!buttonContainer.length || buttonContainer.find('.reorder-control').length) {
        return;
      }
      buttonContainer.append(createButton(item, options, 'up'), createButton(item, options, 'down'));
    });
  };
});
