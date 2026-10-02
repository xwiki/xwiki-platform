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

  function createElement(tagName, className) {
    const element = document.createElement(tagName);
    if (className) {
      element.className = className;
    }
    return element;
  }

  function show(element) {
    element.style.display = '';
  }

  function hide(element) {
    element.style.display = 'none';
  }

  function isVisible(element) {
    return window.getComputedStyle(element).display !== 'none';
  }

  /**
   * @return the closest previous sibling element that matches the given selector, or undefined if there's none
   */
  function getPreviousSibling(element, selector) {
    let sibling = element.previousElementSibling;
    while (sibling && !sibling.matches(selector)) {
      sibling = sibling.previousElementSibling;
    }
    return sibling || undefined;
  }

  function getSiblings(element) {
    return Array.from(element.parentElement.children).filter(child => child !== element);
  }

  /**
   * @return the top offset of the given element relative to its closest positioned ancestor (excluding the element's
   *   top margin)
   */
  function getPositionedOffsetTop(element) {
    let top = 0;
    let current = element;
    do {
      top += current.offsetTop || 0;
      current = current.offsetParent;
    } while (current && current !== document.body && window.getComputedStyle(current).position === 'static');
    return top - (parseFloat(window.getComputedStyle(element).marginTop) || 0);
  }

  function stopEvent(event) {
    event.preventDefault();
    event.stopPropagation();
  }

  /**
   * Full screen editing for textareas or maximizable elements.
   *
   * Note that this class can be extended using Prototype.js' Class.create(XWiki.widgets.FullScreen, {...}). This is
   * why the constructor only delegates to the initialize method (Prototype.js calls only the initialize method when
   * creating an instance of a subclass).
   */
  class FullScreen {
    constructor(...args) {
      this.initialize(...args);
    }

    initialize() {
      // Nothing to initialize. The DOM is initialized lazily, see initDom.
    }

    /**
     * Full screen control initialization
     * Identifies the elements that must be visible in full screen: the textarea or the rich text editor, along with
     * their toolbar and the form buttons.
     * Creates two buttons for closing the fullscreen: one (image) to insert in the toolbar, and one (plain form button)
     * to add next to the form's action buttons.
     * Finally, the textareas and rich text editors in the form are equipped with their own fullscreen activators,
     * inserted in the corresponding toolbar, if there is any, or simply next to the textarea in the document
     * (see the {@link #addBehavior} function),
     */
    initDom() {
      if (!this.domInitialized) {
        // The action buttons need to be visible in full screen
        this.buttons = document.body.querySelector(".bottombuttons");
        // If there are no buttons, at least the Exit FS button should be visible, so create an empty button container
        if (!this.buttons) {
          this.buttons = createElement("div", "bottombuttons");
          this.buttons.append(createElement("div", "buttons"));
          this.buttons._x_isCustom = true;
          // It doesn't matter where the container is, it will only be needed in fullScreen.
          hide(this.buttons);
          document.body.appendChild(this.buttons);
        }
        // When the full screen is activated, the buttons will be brought in the fullscreen, thus removed from their
        // parent element, where they are replaced by a placeholder, so that we know exactly where to put them back.
        this.buttonsPlaceholder = createElement("span");
        // Placeholder for the toolbar, see above.
        this.toolbarPlaceholder = createElement("span");
        // The controls that will close the fullscreen
        this.createCloseButtons();
        this.maximizedReference = document.body.querySelector("input[name='x-maximized']");
        // Cleanup before the window unloads.
        this.unloadHandler = () => this.cleanup();
        window.addEventListener('unload', this.unloadHandler);
        this.domInitialized = true;
      }
    }

    /** According to the type of each element being maximized, a button in created and attached to it. */
    addBehavior(item) {
      if (!this.isNotMaximizable(item) && !this.isAlreadyAugmented(item)) {
        if (this.isWikiContent(item)) {
          this.addWikiContentButton(item);
        } else if (this.isWikiField(item)) {
          this.addWikiFieldButton(item);
        } else {
          // a div element with class maximazable
          this.addElementButton(item);
        }
      }
    }

    restoreFullscreenFromPreview() {
      // When coming back from preview, check if the user was in full screen before hitting preview, and if so restore
      // that full screen
      if (this.maximizedReference && this.maximizedReference.value != "") {
        const matches = document.querySelectorAll(this.maximizedReference.value);
        if (matches.length > 0) {
          this.makeFullScreen(matches[0]);
        }
      }
    }

    isAlreadyAugmented(item) {
      return typeof item._x_fullScreenActivator !== 'undefined';
    }

    isNotMaximizable(item) {
      return item.classList.contains('not-maximizable');
    }

    // Some simple functions that help deciding what kind of editor is the target element
    isWikiContent(textarea) {
      // If there's a toolbar and the textarea is visible
      return getPreviousSibling(textarea, '.leftmenu2') !== undefined && isVisible(textarea);
    }

    isWikiField(textarea) {
      return isVisible(textarea);
    }

    /** Adds the fullscreen button in the Wiki editor toolbar. */
    addWikiContentButton(textarea) {
      textarea._toolbar = getPreviousSibling(textarea, '.leftmenu2');
      // Normally there should be a simple toolbar with basic actions
      if (textarea._toolbar) {
        getPreviousSibling(textarea, '.fullScreenEditLinkContainer')?.remove();
        textarea._toolbar.prepend(this.createOpenButton(textarea));
      } else {
        this.addWikiFieldButton(textarea);
      }
    }

    addElementButton(element) {
      element.before(this.createOpenLink(element));
    }

    addWikiFieldButton(textarea) {
      textarea.before(this.createOpenLink(textarea));
    }

    /** Creates a full screen activator button for the given element. */
    createOpenButton(targetElement) {
      // Create HTML element
      const fullScreenActivator = createElement('img', 'fullScreenEditButton');
      fullScreenActivator.title = this.editFullScreenLabel;
      fullScreenActivator.alt = this.editFullScreenLabel;
      fullScreenActivator.src = $jsontool.serialize($xwiki.getSkinFile('icons/silk/arrow_out.png'));
      // Add functionality
      fullScreenActivator.addEventListener('click', () => this.makeFullScreen(targetElement));
      fullScreenActivator.addEventListener('mousedown', event => this.preventDrag(event));
      // Remember the button associated with each maximizable element
      targetElement._x_fullScreenActivator = fullScreenActivator;
      fullScreenActivator._x_maximizedElement = targetElement;
      return fullScreenActivator;
    }

    createOpenLink(targetElement) {
      // Create HTML element
      const fullScreenActivatorContainer = createElement('div', 'fullScreenEditLinkContainer');
      const fullScreenActivator = createElement('a', 'fullScreenEditLink');
      fullScreenActivator.title = this.editFullScreenLabel;
      fullScreenActivator.textContent = this.editFullScreenLabel + ' »';
      // Add functionality
      fullScreenActivator.addEventListener('click', () => this.makeFullScreen(targetElement));
      // Add it to the container
      fullScreenActivatorContainer.replaceChildren(fullScreenActivator);
      // Remember the button associated with each maximizable element
      targetElement._x_fullScreenActivator = fullScreenActivator;
      fullScreenActivator._x_maximizedElement = targetElement;
      return fullScreenActivatorContainer;
    }

    /**
     * Creates the full screen close buttons (which are generic, not attached to the maximized elements like the
     * activators)
     */
    createCloseButtons() {
      // Toolbar image button
      // Create HTML element
      this.closeButton = createElement('img', 'fullScreenCloseButton');
      this.closeButton.title = this.exitFullScreenLabel;
      this.closeButton.alt = this.exitFullScreenLabel;
      this.closeButton.src = $jsontool.serialize($xwiki.getSkinFile('icons/silk/arrow_in.png'));
      // Add functionality
      this.closeButton.addEventListener('click', () => this.closeFullScreen());
      this.closeButton.addEventListener('mousedown', event => this.preventDrag(event));
      // Hide by default
      hide(this.closeButton);

      // Edit actions button
      // Create HTML element
      this.actionCloseButton = createElement('input', 'button');
      this.actionCloseButton.type = 'button';
      this.actionCloseButton.value = this.exitFullScreenLabel;
      this.actionCloseButtonWrapper = createElement('span', 'buttonwrapper');
      this.actionCloseButtonWrapper.replaceChildren(this.actionCloseButton);
      // Add functionality
      this.actionCloseButton.addEventListener('click', () => this.closeFullScreen());
      // Hide by default
      hide(this.actionCloseButtonWrapper);
      // Add it in the action bar
      this.buttons.querySelector(".buttons").prepend(this.actionCloseButtonWrapper);
    }

    /**
      * How this works:
      * - All the elements between the targetElement and the root element are maximized, and all the other nodes are
      *   hidden
      * - The parent element becomes a wrapper around the targetElement
      * - Move the toolbar (if it exists) and the action buttons in the wrapper
      * - Hide the overflows of the body element, so that a scrollbar doesn't appear
      * - All the initial styles of the altered elements are remembered, so that they can be restored when exiting
      *   fullscreen
      */
    makeFullScreen(targetElement) {
      $(document).trigger("xwiki:fullscreen:enter", [{ "target" : targetElement }]);
      // Store the selector of the target element in the form, in the hidden input called 'x-maximized'.
      // This is needed so that the full screen can be reactivated when coming back from preview, if it was activate
      // before the user hit the preview button.
      if (this.maximizedReference) {
        if (targetElement.id) {
          // Using #ID fails since the IDs for the textareas in inline editing contain the '.' character, which marks a
          // classname
          this.maximizedReference.value = targetElement.tagName + "[id='" + targetElement.id + "']";
        } else if (targetElement.name) {
          this.maximizedReference.value = targetElement.tagName + "[name='" + targetElement.name + "']" ;
        } else if (targetElement.className) {
          // No id, no name. This must be the WYSIWYG editor...
          this.maximizedReference.value = targetElement.tagName + "." + targetElement.className ;
        }
      }
      // Remember the maximized element
      this.maximized = targetElement;
      // Remember the cursor position and scroll offset (needed for circumventing
      // https://bugzilla.mozilla.org/show_bug.cgi?id=633789 )
      let selectionStart, selectionEnd, scrollTop;
      if (typeof targetElement.setSelectionRange == 'function') {
        selectionStart = targetElement.selectionStart;
        selectionEnd = targetElement.selectionEnd;
        scrollTop = targetElement.scrollTop;
      }
      // Remember the original dimensions of the maximized element
      targetElement._originalStyle = {
        'width' : targetElement.style['width'],
        'height' : targetElement.style['height']
      };
      // All the elements between the targetElement and the root element are set to position: static, so that the
      // offset parent of the targetElement will be the window. Remember the previous settings in order to be able to
      // restore the layout when exiting fullscreen.
      const wrapper = targetElement.parentElement;
      wrapper.classList.add("fullScreenWrapper");
      if (targetElement._toolbar) {
        // The wiki editor has the toolbar outside the textarea element, unlike the other editors, which have it as a
        // descendant
        if (targetElement._toolbar.classList.contains("leftmenu2")) {
          targetElement._toolbar.replaceWith(this.toolbarPlaceholder);
          wrapper.prepend(targetElement._toolbar);
        }
        // Replace the Maximize button in the toolbar with the Restore one
        targetElement._x_fullScreenActivator.replaceWith(this.closeButton);
      }
      this.buttons.replaceWith(this.buttonsPlaceholder);
      show(this.buttons);
      wrapper.append(this.buttons);
      let parent = targetElement.parentElement;
      hide(targetElement._x_fullScreenActivator);
      while (parent != document.body) {
        parent._originalStyle = {
          'overflow' : parent.style['overflow'],
          'position' : parent.style['position'],
          'width' : parent.style['width'],
          'height' : parent.style['height'],
          'left' : parent.style['left'],
          'right' : parent.style['right'],
          'top' : parent.style['top'],
          'bottom' : parent.style['bottom'],
          'padding' : parent.style['padding'],
          'margin' : parent.style['margin']
        };
        Object.assign(parent.style, {'overflow': "visible", 'position': "absolute", width: "100%", height: "100%",
          left: 0, top:0, right:0, bottom: 0, padding: 0, margin: 0});
        getSiblings(parent).forEach(function(item) {
          item._originalDisplay = item.style['display'];
          hide(item);
          // We tag this element to know that we have hidden it, and that we should rollback the original style when
          // we close the fullscreen mode.
          // We have introduced this variable because _originalDisplay can be null so we cannot rely on this variable
          // to know if either or not we have hidden the element.
          item._fullscreenHidden = true;
        });
        parent = parent.parentElement;
      }
      document.body._originalStyle = {
        'overflow' : parent.style['overflow'],
        'width' : parent.style['width'],
        'height' : parent.style['height']
      };
      const root = document.documentElement;
      root._originalStyle = {
        'overflow' : root.style['overflow'],
        'width' : root.style['width'],
        'height' : root.style['height']
      };
      Object.assign(document.body.style, {'overflow': 'hidden', 'width': '100%', 'height': '100%'});
      Object.assign(root.style, {'overflow': "hidden", 'width': "100%", 'height': "100%"});

      // Make sure to resize the targetElement when the window dimensions are changed.
      this.resizeListener = () => this.resizeTextArea(targetElement);
      window.addEventListener('resize', this.resizeListener);
      // Show the exit buttons
      show(this.closeButton);
      show(this.actionCloseButtonWrapper);
      // Maximize the targetElement
      this.resizeTextArea(targetElement);
      // Reset the cursor and scroll offset
      if (typeof targetElement.setSelectionRange == 'function') {
        // This is approximate, since the textarea width changes, and more lines can fit in the same vertical space
        targetElement.scrollTop = scrollTop;
        targetElement.selectionStart = selectionStart;
        targetElement.selectionEnd = selectionEnd;
      }
      $(document).trigger("xwiki:fullscreen:entered", [{ "target" : targetElement }]);
    }

    /** Restore the layout. */
    closeFullScreen() {
      const targetElement = this.maximized;
      $(document).trigger("xwiki:fullscreen:exit", [{ "target" : targetElement }]);
      // Remember the cursor position and scroll offset (needed for circumventing
      // https://bugzilla.mozilla.org/show_bug.cgi?id=633789 )
      let selectionStart, selectionEnd, scrollTop;
      if (typeof targetElement.setSelectionRange == 'function') {
        selectionStart = targetElement.selectionStart;
        selectionEnd = targetElement.selectionEnd;
        scrollTop = targetElement.scrollTop;
      }
      // Hide the exit buttons
      hide(this.closeButton);
      hide(this.actionCloseButtonWrapper);
      // We're no longer interested in resize events
      window.removeEventListener('resize', this.resizeListener);
      // Restore the parent element (the wrapper)
      targetElement.parentElement.classList.remove("fullScreenWrapper");

      // Restore the previous layout
      // NOTE: We restore the previous layout in reverse order (from the document body down to the target element) to
      // overcome a IE7 bug (see http://jira.xwiki.org/jira/browse/XWIKI-4346 ).
      let parent = targetElement.parentElement;
      const parents = [];
      while (parent != document.body) {
        parents.push(parent);
        parent = parent.parentElement;
      }
      let i = parents.length;
      while (i--) {
        parent = parents[i];
        Object.assign(parent.style, parent._originalStyle);
        getSiblings(parent).forEach(function(item) {
          // if the element has been hidden by us, we should rollback its style
          if (item._fullscreenHidden) {
            item.style['display'] = item._originalDisplay;
          }
        });
      }
      Object.assign(document.body.style, document.body._originalStyle);
      Object.assign(document.documentElement.style, document.documentElement._originalStyle);
      // Restore the toolbar and action buttons to their initial position
      this.buttonsPlaceholder.replaceWith(this.buttons);
      if (this.buttons._x_isCustom) {
        hide(this.buttons);
      }
      if (targetElement._toolbar) {
        if (targetElement._toolbar.classList.contains("leftmenu2")) {
          this.toolbarPlaceholder.replaceWith(targetElement._toolbar);
        }
        // Replace the Restore button in the toolbar with the Maximize one
        this.closeButton.replaceWith(targetElement._x_fullScreenActivator);
      }
      show(targetElement._x_fullScreenActivator);
      Object.assign(targetElement.style, targetElement._originalStyle);
      // No element is maximized anymore
      delete this.maximized;
      if (this.maximizedReference) {
        this.maximizedReference.value = '';
      }
      // Reset the cursor and scroll offset
      if (typeof targetElement.setSelectionRange == 'function') {
        // This is approximate, since the textarea width changes, and more lines can fit in the same vertical space
        targetElement.scrollTop = scrollTop;
        targetElement.selectionStart = selectionStart;
        targetElement.selectionEnd = selectionEnd;
      }
      $(document).trigger("xwiki:fullscreen:exited", [{ "target" : targetElement }]);
    }

    /** In full screen, when the containers's dimensions change, the maximized element must be resized accordingly. */
    resizeTextArea(targetElement) {
      if (!this.maximized) {
        return;
      }
      // Compute the maximum space available for the textarea:
      let newHeight = document.documentElement.clientHeight;
      let newWidth = document.documentElement.clientWidth;
      // Window width - styling padding
      newWidth = newWidth - this.margin;
      // Window height - margin (for the toolbar) - styling padding - buttons
      newHeight = newHeight - getPositionedOffsetTop(targetElement) - this.margin - this.buttons.offsetHeight;
      Object.assign(targetElement.style, {'width' :  newWidth + 'px', 'height' :  newHeight + 'px'});
      $(document).trigger("xwiki:fullscreen:resized", [{ "target" : targetElement }]);
    }

    /** onMouseDown handler that prevents dragging the button. */
    preventDrag(event) {
      stopEvent(event);
    }

    /** Cleans up the DOM tree when the user leaves the current page. */
    cleanup() {
      window.removeEventListener('unload', this.unloadHandler);
      // Remove the "Exit full screen" action button because it can interfere with the browser's back-forward cache.
      this.actionCloseButtonWrapper.remove();
    }
  }

  // Some layout settings, to be customized for other skins
  Object.assign(FullScreen.prototype, {
    /** Maximized element margins */
    margin : 0,
    /** Full screen activator / deactivator button size */
    buttonSize : 16,
    editFullScreenLabel: $jsontool.serialize($services.localization.render('core.editors.fullscreen.editFullScreen')),
    exitFullScreenLabel: $jsontool.serialize($services.localization.render('core.editors.fullscreen.exitFullScreen')),
    domInitialized: false
  });

  /** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
  FullScreen.subclasses = [];

  widgets.FullScreen = FullScreen;
  const fullScreen = widgets.__fullscreenInstance = new FullScreen();

  const init = function(event, data) {
    fullScreen.initDom();
    for (const container of (data?.elements || [document])) {
      container.querySelectorAll('textarea, .maximizable').forEach(item => fullScreen.addBehavior(item));
    }
    fullScreen.restoreFullscreenFromPreview();
  };

  $(document).on('xwiki:dom:updated', init);
  $(init);
});
