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
 * Full screen editing for textareas or maximizable elements.
 */
widgets.FullScreen = Class.create({
  // Some layout settings, to be customized for other skins
  /** Maximized element margins */
  margin : 0,
  /** Full screen activator / deactivator button size */
  buttonSize : 16,
  editFullScreenLabel: $jsontool.serialize($services.localization.render('core.editors.fullscreen.editFullScreen')),
  exitFullScreenLabel: $jsontool.serialize($services.localization.render('core.editors.fullscreen.exitFullScreen')),
  /**
   * Full screen control initialization
   * Identifies the elements that must be visible in full screen: the textarea or the rich text editor, along with their
   * toolbar and the form buttons.
   * Creates two buttons for closing the fullscreen: one (image) to insert in the toolbar, and one (plain form button)
   * to add next to the form's action buttons.
   * Finally, the textareas and rich text editors in the form are equipped with their own fullscreen activators,
   * inserted in the corresponding toolbar, if there is any, or simply next to the textarea in the document
   * (see the {@link #addBehavior} function),
   */
  initialize : function() {
    // The action buttons need to be visible in full screen
    this.buttons = $(document.body).down(".bottombuttons");
    // If there are no buttons, at least the Exit FS button should be visible, so create an empty button container
    if (!this.buttons) {
      this.buttons = new Element("div", {"class" : "bottombuttons"}).update(new Element("div", {"class" : "buttons"}));
      this.buttons._x_isCustom = true;
      // It doesn't matter where the container is, it will only be needed in fullScreen.
      document.body.appendChild(this.buttons.hide());
    }
    // When the full screen is activated, the buttons will be brought in the fullscreen, thus removed from their parent
    // element, where they are replaced by a placeholder, so that we know exactly where to put them back.
    this.buttonsPlaceholder = new Element("span");
    // Placeholder for the toolbar, see above.
    this.toolbarPlaceholder = new Element("span");
    // The controls that will close the fullscreen
    this.createCloseButtons();
    // Prepare textareas / maximizable elements for full screen editing
    $$('textarea', '.maximizable').each(function(element) {
      this.addBehavior(element);
    }.bind(this));
    document.observe('xwiki:dom:updated', function(event) {
      event.memo.elements.each(function(element) {
        element.select('textarea', '.maximizable').each(function(element) {
          this.addBehavior(element);
        }.bind(this));
      }.bind(this));
    }.bind(this));
    // When coming back from preview, check if the user was in full screen before hitting preview, and if so restore
    // that full screen
    this.maximizedReference = $(document.body).down("input[name='x-maximized']");
    if (this.maximizedReference && this.maximizedReference.value != "") {
      var matches = $$(this.maximizedReference.value);
      if (matches && matches.length > 0) {
        this.makeFullScreen(matches[0]);
      }
    }
    // Cleanup before the window unloads.
    this.unloadHandler = this.cleanup.bind(this);
    Event.observe(window, 'unload', this.unloadHandler);
  },
  /** According to the type of each element being maximized, a button in created and attached to it. */
  addBehavior : function (item) {
    if (!this.isNotMaximizable(item)) {
      if (this.isWikiContent(item)) {
        this.addWikiContentButton(item);
      } else if (this.isWikiField(item)) {
        this.addWikiFieldButton(item);
      } else {
        // a div element with class maximazable
        this.addElementButton(item);
      }
    }
  },
  isNotMaximizable: function (item) {
    return item.hasClassName('not-maximizable');
  },
  // Some simple functions that help deciding what kind of editor is the target element
  isWikiContent : function (textarea) {
    // If the textarea is not visible, then the WYSIWYG editor is active.
    return textarea.name == 'content' && textarea.visible();
  },
  isWikiField : function (textarea) {
    // If the textarea is not visible, then the WYSIWYG editor is active.
    return textarea.visible();
  },
  /** Adds the fullscreen button in the Wiki editor toolbar. */
  addWikiContentButton : function (textarea) {
    textarea._toolbar = $(document.body).down(".leftmenu2");
    // Normally there should be a simple toolbar with basic actions
    if (textarea._toolbar) {
      textarea._toolbar.insert({top: this.createOpenButton(textarea)});
    } else {
      this.addWikiFieldButton(textarea);
    }
  },
  addElementButton: function(element) {
    Element.insert(element, {before: this.createOpenLink(element)});
  },
  addWikiFieldButton : function (textarea) {
    Element.insert(textarea, {before: this.createOpenLink(textarea)});
  },
  /** Creates a full screen activator button for the given element. */
  createOpenButton : function (targetElement) {
    // Create HTML element
    var fullScreenActivator = new Element('img', {
      'class': 'fullScreenEditButton',
      title: this.editFullScreenLabel,
      alt: this.editFullScreenLabel,
      src: $jsontool.serialize($xwiki.getSkinFile('icons/silk/arrow_out.png'))
    });
    // Add functionality
    fullScreenActivator.observe('click', this.makeFullScreen.bind(this, targetElement));
    fullScreenActivator.observe('mousedown', this.preventDrag.bindAsEventListener(this));
    // Remember the button associated with each maximizable element
    targetElement._x_fullScreenActivator = fullScreenActivator;
    fullScreenActivator._x_maximizedElement = targetElement;
    return fullScreenActivator;
  },
  createOpenLink : function (targetElement) {
    // Create HTML element
    var fullScreenActivatorContainer = new Element('div', {
      'class': 'fullScreenEditLinkContainer'
    });
    var fullScreenActivator = new Element('a', {
      'class': 'fullScreenEditLink',
      title: this.editFullScreenLabel
    }).update(this.editFullScreenLabel + ' &raquo;');
    // Add functionality
    fullScreenActivator.observe('click', this.makeFullScreen.bind(this, targetElement));
    // Add it to the container
    fullScreenActivatorContainer.update(fullScreenActivator);
    // Remember the button associated with each maximizable element
    targetElement._x_fullScreenActivator = fullScreenActivator;
    fullScreenActivator._x_maximizedElement = targetElement;
    return fullScreenActivatorContainer;
  },
  /**
   * Creates the full screen close buttons (which are generic, not attached to the maximized elements like the activators)
   */
  createCloseButtons : function () {
    // Toolbar image button
    // Create HTML element
    this.closeButton = new Element('img', {
      'class': 'fullScreenCloseButton',
      title: this.exitFullScreenLabel,
      alt: this.exitFullScreenLabel,
      src: $jsontool.serialize($xwiki.getSkinFile('icons/silk/arrow_in.png'))
    });
    // Add functionality
    this.closeButton.observe('click', this.closeFullScreen.bind(this));
    this.closeButton.observe('mousedown', this.preventDrag.bindAsEventListener(this));
    // Hide by default
    this.closeButton.hide();

    // Edit actions button
    // Create HTML element
    this.actionCloseButton = new Element('input', {
      type: 'button',
      'class': 'button',
      value: this.exitFullScreenLabel
    });
    this.actionCloseButtonWrapper = new Element('span', {
      'class': 'buttonwrapper'
    });
    this.actionCloseButtonWrapper.update(this.actionCloseButton);
    // Add functionality
    this.actionCloseButton.observe('click', this.closeFullScreen.bind(this));
    // Hide by default
    this.actionCloseButtonWrapper.hide();
    // Add it in the action bar
    this.buttons.down(".buttons").insert({top: this.actionCloseButtonWrapper});
  },

  /**
    * How this works:
    * - All the elements between the targetElement and the root element are maximized, and all the other nodes are hidden
    * - The parent element becomes a wrapper around the targetElement
    * - Move the toolbar (if it exists) and the action buttons in the wrapper
    * - Hide the overflows of the body element, so that a scrollbar doesn't appear
    * - All the initial styles of the altered elements are remembered, so that they can be restored when exiting fullscreen
    */
  makeFullScreen : function (targetElement) {
    document.fire("xwiki:fullscreen:enter", { "target" : targetElement });
    // Store the selector of the target element in the form, in the hidden input called 'x-maximized'.
    // This is needed so that the full screen can be reactivated when coming back from preview, if it was activate before
    // the user hit the preview button.
    if (this.maximizedReference) {
      if (targetElement.id) {
        // Using #ID fails since the IDs for the textareas in inline editing contain the '.' character, which marks a classname
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
    // Remember the cursor position and scroll offset (needed for circumventing https://bugzilla.mozilla.org/show_bug.cgi?id=633789 )
    if (typeof targetElement.setSelectionRange == 'function') {
      var selectionStart = targetElement.selectionStart;
      var selectionEnd = targetElement.selectionEnd;
      var scrollTop = targetElement.scrollTop;
    }
    // Remember the original dimensions of the maximized element
    targetElement._originalStyle = {
      'width' : targetElement.style['width'],
      'height' : targetElement.style['height']
    };
    // All the elements between the targetElement and the root element are set to position: static, so that the offset
    // parent of the targetElement will be the window. Remember the previous settings in order to be able to restore the
    // layout when exiting fullscreen.
    var wrapper = targetElement.up();
    wrapper.addClassName("fullScreenWrapper");
    if(targetElement._toolbar) {
      // The wiki editor has the toolbar outside the textarea element, unlike the other editors, which have it as a descendant
      if (targetElement._toolbar.hasClassName("leftmenu2")) {
        wrapper.insert({"top" : targetElement._toolbar.replace(this.toolbarPlaceholder)});
      }
      // Replace the Maximize button in the toolbar with the Restore one
      targetElement._x_fullScreenActivator.replace(this.closeButton);
    }
    wrapper.insert(this.buttons.replace(this.buttonsPlaceholder).show());
    var parent = targetElement.up();
    targetElement._x_fullScreenActivator.hide();
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
      parent.setStyle({'overflow': "visible", 'position': "absolute", width: "100%", height: "100%", left: 0, top:0, right:0, bottom: 0, padding: 0, margin: 0});
      parent.siblings().each(function(item) {
        item._originalDisplay = item.style['display'];
        item.setStyle({display: "none"});
        // We tag this element to know that we have hidden it, and that we should rollback the original style when we
        // close the fullscreen mode.
        // We have introduced this variable because _originalDisplay can be null so we cannot rely on this variable
        // to know if either or not we have hidden the element.
        item._fullscreenHidden = true;
      });
      parent = parent.up();
    }
    document.body._originalStyle = {
      'overflow' : parent.style['overflow'],
      'width' : parent.style['width'],
      'height' : parent.style['height']
    };
    var root = $(document.body).up();
    root._originalStyle = {
      'overflow' : root.style['overflow'],
      'width' : root.style['width'],
      'height' : root.style['height']
    };
    $(document.body).setStyle({'overflow': 'hidden', 'width': '100%', 'height': '100%'});
    root.setStyle({'overflow': "hidden", 'width': "100%", 'height': "100%"});

    // Make sure to resize the targetElement when the window dimensions are changed. Both document and window are monitored,
    // since different browsers send events to different elements.
    this.resizeListener = this.resizeTextArea.bind(this, targetElement);
    Event.observe(window, 'resize', this.resizeListener);
    // Show the exit buttons
    this.closeButton.show();
    this.actionCloseButtonWrapper.show();
    // Maximize the targetElement
    this.resizeTextArea(targetElement);
    // Reset the cursor and scroll offset
    if (typeof targetElement.setSelectionRange == 'function') {
      // This is approximate, since the textarea width changes, and more lines can fit in the same vertical space
      targetElement.scrollTop = scrollTop;
      targetElement.selectionStart = selectionStart;
      targetElement.selectionEnd = selectionEnd;
    }
    document.fire("xwiki:fullscreen:entered", { "target" : targetElement });
  },
  /** Restore the layout. */
  closeFullScreen : function() {
    var targetElement = this.maximized;
    document.fire("xwiki:fullscreen:exit", { "target" : targetElement });
    // Remember the cursor position and scroll offset (needed for circumventing https://bugzilla.mozilla.org/show_bug.cgi?id=633789 )
    if (typeof targetElement.setSelectionRange == 'function') {
      var selectionStart = targetElement.selectionStart;
      var selectionEnd = targetElement.selectionEnd;
      var scrollTop = targetElement.scrollTop;
    }
    // Hide the exit buttons
    this.closeButton.hide();
    this.actionCloseButtonWrapper.hide();
    // We're no longer interested in resize events
    Event.stopObserving(window, 'resize', this.resizeListener);
    // Restore the parent element (the wrapper)
    targetElement.up().removeClassName("fullScreenWrapper");

    // Restore the previous layout
    // NOTE: We restore the previous layout in reverse order (from the document body down to the target element) to
    // overcome a IE7 bug (see http://jira.xwiki.org/jira/browse/XWIKI-4346 ).
    var parent = targetElement.up();
    var parents = [];
    while (parent != document.body) {
      parents.push(parent);
      parent = parent.up();
    }
    var i = parents.length;
    while (i--) {
      parent = parents[i];
      parent.setStyle(parent._originalStyle);
      parent.siblings().each(function(item) {
        // if the element has been hidden by us, we should rollback its style
        if (item._fullscreenHidden) {
          item.style['display'] = item._originalDisplay;
        }
      });
    }
    document.body.setStyle(document.body._originalStyle);
    $(document.body).up().setStyle($(document.body).up()._originalStyle);
    // Restore the toolbar and action buttons to their initial position
    this.buttonsPlaceholder.replace(this.buttons);
    if (this.buttons._x_isCustom) {
      this.buttons.hide();
    }
    if (targetElement._toolbar) {
      if (targetElement._toolbar.hasClassName("leftmenu2")) {
        this.toolbarPlaceholder.replace(targetElement._toolbar);
      }
      // Replace the Restore button in the toolbar with the Maximize one
      this.closeButton.replace(targetElement._x_fullScreenActivator);
    }
    targetElement._x_fullScreenActivator.show();
    targetElement.setStyle(targetElement._originalStyle);
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
    document.fire("xwiki:fullscreen:exited", { "target" : targetElement });
  },
  /** In full screen, when the containers's dimensions change, the maximized element must be resized accordingly. */
  resizeTextArea : function(targetElement) {
    if (!this.maximized) {
      return;
    }
    // Compute the maximum space available for the textarea:
    var newHeight = document.viewport.getHeight();
    var newWidth = document.viewport.getWidth();
    // Window width - styling padding
    newWidth = newWidth - this.margin;
    // Window height - margin (for the toolbar) - styling padding - buttons
    newHeight = newHeight - targetElement.positionedOffset().top - this.margin - this.buttons.getHeight();
    targetElement.setStyle({'width' :  newWidth + 'px', 'height' :  newHeight + 'px'});
    document.fire("xwiki:fullscreen:resized", { "target" : targetElement });
  },
  /** onMouseDown handler that prevents dragging the button. */
  preventDrag : function(event) {
    event.stop();
  },
  /** Cleans up the DOM tree when the user leaves the current page. */
  cleanup : function() {
    Event.stopObserving(window, 'unload', this.unloadHandler);
    // Remove the "Exit full screen" action button because it can interfere with the browser's back-forward cache.
    // This can throw an exception in certain browsers (IE9 for one), since the DOM may be already cleaned
    try {
      this.actionCloseButtonWrapper.remove();
    } catch (ex) {
      // Not important, just ignore
    }
  }
});

function init() {
  return new widgets.FullScreen();
}

// When the document is loaded, enable the fullscreen behavior.
(XWiki.domIsLoaded && init())
|| document.observe("xwiki:dom:loaded", init);
// End XWiki augmentation.
return XWiki;
}(XWiki || {}));

