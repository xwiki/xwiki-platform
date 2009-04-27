// ======================================
// Full screen editing for page content
// 
if (typeof(XWiki) == 'undefined') {
  XWiki = new Object();
}
if (typeof(XWiki.dataEditors) == 'undefined') {
  XWiki.dataEditors = new Object();
}

/**
 * Full screen editing for textarea.
 * 
 * TODO Make it work for textareas in inline editing
 * TODO Make it work for textareas in the object editor
 */
XWiki.dataEditors.FullScreenEditing = Class.create({
  /** Some layout settings */
  margin : 0, /** Maximized element margins */
  buttonSize : 16, /** Full screen activator / deactivator button size */
  /**
   * Full screen control initialization
   * Identifies the elements that must be visible in full screen: the textarea or the rich text editor, along with their
   * toolbar and the form buttons.
   * Creates Two buttons for closing the fullscreen: one (image) to insert in the toolbar, and one (plain form button)
   * to add next to the form's action buttons.
   * Finally, the textareas and rich text editors in the form are equipped with their own fullscreen activators,
   * inserted in the corresponding toolbar, if there is any, or simply next to the textarea in the document
   * (see the addBehavior function),
   */
  initialize : function() {
    // The action buttons need to be visible in full screen
    this.buttons = $(document.body).down(".bottombuttons");
    if (!this.buttons) {
      this.buttons = new Element("div", {"class" : "bottombuttons"}).update(new Element("div", {"class" : "buttons"}));
      document.body.appendChild(this.buttons);
    }
    // When the full screen is activated, the buttons will be brought in the fullscreen, thus removed from their parent
    // element, where they are replaced by a placeholder, so that we know exactly where to put them back.
    this.buttonsPlaceholder = new Element("span");
    // The editor toolbar needs to be visible in full screen. Might not exist at all.
    this.toolbar = $(document.body).down(".leftmenu2") || $(document.body).down(".gwt-MenuBar") || $(document.body).down(".mceToolbar");
    // Placeholder for the toolbar, see above.
    this.toolbarPlaceholder = new Element("span");
    // The controls that will close the fullscreen
    this.createCloseButtons();
    // Prepare textareas in the form for full screen editing
    $$('textarea').each(function(textarea) {
      this.addBehavior(textarea);
    }.bind(this));
    // The GWT editor removes the textarea from the document, thus should be treated separately
    $$('.xRichTextEditor').each(function(item) {
      this.addBehavior(item);
    }.bind(this));
  },
  /**
   * According to the type of each element being maximized, a button in created and attached to it.
   */
  addBehavior : function (item) {
    if (this.isWysiwyg20Content(item)) {
      this.addWysiwyg20ContentButton(item);
    } else if (this.isWysiwyg10Content(item)) {
      this.addWysiwyg10ContentButton(item);
    } else if (this.isWikiContent(item)) {
      this.addWikiContentButton(item);
    }
    /* Deactivating temporarily full screen edit for textareas other than wiki content
    else if (this.isWikiTextarea(textarea)) {
      this.addWikiTextareaButton(textarea);
    } */
  },
  addWikiContentButton : function (textarea) {
    if (this.toolbar) {
      this.toolbar.appendChild(this.createOpenButton(textarea));
    } else {
      this.addWikiTextareaButton(textarea);
    }
  },
  addWysiwyg10ContentButton : function (item) {
    var container = item.next(".mceEditorContainer") || item.previous(".mceEditorContainer");
    if (!container) {
      return false;
    }
    var toolbar = container.down(".mceToolbar");
    if (!toolbar) {
      return false;
    }
    // Create a tinymce-like toolbar to contain the fullscreen button
    var newToolbar = new Element('span', {'class': 'mce_editor_fullscreentoolbar'});
    newToolbar.insert(new Element('img', {
       'class': 'mceSeparatorLine',
       height: 15,
       width: 1,
       src: toolbar.down('img.mceSeparatorLine').src
    }));
    newToolbar.insert(this.createOpenButton(container));
    toolbar.insert(newToolbar);
    return true;
  },
  addWysiwyg20ContentButton : function (item) {
    var toolbar = item.down(".gwt-MenuBar");
    // Sometimes the toolbar isn't loaded when this method executes (in IE). Schedule a periodical reatempt.
    if (!toolbar) {
      // Only schedule once
      if (!item.__x_fullScreenLoader) {
        item.__x_fullScreenLoader_iterations = 0;
        item.__x_fullScreenLoader = new PeriodicalExecuter(function(item) {
          // Give up after 20 seconds
          if (item.__x_fullScreenLoader_iteration > 100) {
            item.__x_fullScreenLoader.stop();
            item.__x_fullScreenLoader = false;
            return;
          }
          item.__x_fullScreenLoader_iteration++;
          this.addWysiwyg20ContentButton(item);
        }.bind(this, item), 0.2);
      }
      return false;
    }
    toolbar.insert({"top" : this.createOpenButton(item)});
    this.toolbar = toolbar;
    if (item.__x_fullScreenLoader) {
      item.__x_fullScreenLoader.stop();
      item.__x_fullScreenLoader = false;
    }
    return true;
  },
  addWikiTextareaButton : function (textarea) {
    Element.insert(textarea, {before: this.createOpenButton(textarea)});
  },
  /**
   * Some simple functions that help deciding what kind of editor is the target element
   */
  isWikiContent : function (textarea) {
    // if the textarea is not visible, then the WYSIWIG editor is active.
    return textarea.name == 'content' && textarea.visible();
  },
  isWysiwyg10Content : function (textarea) {
    // if the textarea is not visible, then the WYSIWIG editor is active.
    return textarea.name == 'content' && (textarea.next(".mceEditorContainer") || textarea.previous(".mceEditorContainer"));
  },
  isWysiwyg20Content : function (item) {
    // return item.getAttribute("id") == "content_container";
    return item.hasClassName("xRichTextEditor");
  },
  isWikiTextarea : function (textarea) {
    // if the textarea is not visible, then the WYSIWIG editor is active.
    return textarea.visible();
  },
  isWysiwygTextarea : function (textarea) {
    return !textarea.visible() && textarea.next('.mceEditorContainer');
  },
  /**
   * Creates a full screen activator button for the given element.
   */
  createOpenButton : function (targetElement) {
    // Create HTML element
    var fullScreenButton = new Element('img', {
      'class': 'fullScreenEditButton',
      title: "$msg.get('core.editors.fullscreen.editFullScreen')",
      alt: "$msg.get('core.editors.fullscreen.editFullScreen')",
      src: "$xwiki.getSkinFile('icons/silk/arrow_out.gif')"
    });
    // Add functionality
    fullScreenButton.observe('click', this.makeFullScreen.bindAsEventListener(this, targetElement));
    fullScreenButton.observe('mousedown', this.preventDrag.bindAsEventListener(this));
    // Set position
    fullScreenButton.setStyle({
      'left' : targetElement.positionedOffset().left + targetElement.getWidth() - this.buttonSize + 'px',
      'top' : targetElement.positionedOffset().top - this.buttonSize + 'px'
    });
    // Remember the button associated with each maximizable element
    targetElement._fullScreenButton = fullScreenButton;
    fullScreenButton._targetElement = targetElement;
    return fullScreenButton;
  },
  /**
   * Creates the full screen close buttons (which are generic, not attached to the maximized elements like the activators)
   */
  createCloseButtons : function () {
    // Toolbar image button
    // Create HTML element
    this.closeButton = new Element('img', {
      'class': 'fullScreenCloseButton',
      title: "$msg.get('core.editors.fullscreen.exitFullScreen')",
      alt: "$msg.get('core.editors.fullscreen.exitFullScreen')",
      src: "$xwiki.getSkinFile('icons/silk/arrow_in.gif')"
    });
    // Add functionality
    this.closeButton.observe('click', this.closeFullScreen.bind(this));
    this.closeButton.observe('mousedown', this.preventDrag.bindAsEventListener(this));
    // Hide by default
    this.closeButton.hide();

    // Edit actions button
    // Create HTML element
    this.actionCloseButton = new Element('input', {
      "type" : "button",
      'class': 'button',
      value: "$msg.get('core.editors.fullscreen.exitFullScreen')"
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
    this.buttons.down(".buttons").insert(this.actionCloseButtonWrapper);
  },

  /**
    * How this works:
    * - All the elements between the targetElement and the root element are set to position: static, so that the offset
    *   parent of the targetElement will be the window.
    * - Insert a wrapper around the targetElement, and make it full screen
    * - Move the toolbar (if it exists) and the action buttons in the wrapper
    * - Hide the overflows of the body element, so that a scrollbar doesn't appear
    */
  makeFullScreen : function (event, targetElement) {
    event.stop();
    // Remember the maximized element
    this.maximized = targetElement;
    targetElement._originalStyle = targetElement.getAttribute('style');
    targetElement._originalStyle = {
      'width' : targetElement.getStyle('width'),
      'height' : targetElement.getStyle('height')
    };
    // All the elements between the targetElement and the root element are set to position: static, so that the offset
    // parent of the targetElement will be the window. Remember the previous settings in order to be able to restore the
    // layout when exiting fullscreen.
    if (targetElement.hasClassName("xRichTextEditor")) {
      var iframe = targetElement.down(".gwt-RichTextArea");
      iframe._originalStyle = {
        'width' : iframe.getStyle('width'),
        'height' : iframe.getStyle('height')
      };
    } else if (targetElement.hasClassName("mceEditorContainer")) {
      var iframe = targetElement.down(".mceEditorIframe");
      iframe._originalStyle = {
        'width' : iframe.getStyle('width'),
        'height' : iframe.getStyle('height')
      };
    }
    var parent = $(targetElement.parentNode);
    while (parent != document.body) {
      parent._originalStyle = {'overflow' : parent.getStyle('overflow'), 'position' : parent.getStyle('position')};
      parent.setStyle({'overflow': "visible", 'position': "static"});
      parent = parent.parentNode;
    }
    document.body._originalStyle = {
      'overflow' : parent.getStyle('overflow'),
      'width' : parent.getStyle('width'),
      'height' : parent.getStyle('height')
    };
    document.body.setStyle({'overflow' : 'hidden', 'width': '100%', 'height': '100%'});

    // In IE, since position: fixed doesn't work, use position absolute, and scroll the window to the top.
    if (Prototype.Browser.IE) {
      document.body.scrollTo();
    }
    // Wrap the elements that should be visible in full screen in a div and maximize it
    // This causes problems with the GWT editor.
    // var wrapper = new Element('div', {"class" : "fullScreenWrapper"});
    // Element.wrap(targetElement, wrapper);
    var wrapper = targetElement.up();
    wrapper.addClassName("fullScreenWrapper");
    wrapper._originalPadding = wrapper.getStyle("paddingTop");
    if ($("actionmenu")) {
      wrapper.setStyle({"paddingTop" : $("actionmenu").getHeight() + "px"});
    }
    if (this.toolbar) {
      if (this.toolbar.hasClassName("leftmenu2")) {
        wrapper.insert({"top" : this.toolbar.replace(this.toolbarPlaceholder)});
      }
      // Replace the Maximize button in the toolbar with the Restore one
      targetElement._fullScreenButton.replace(this.closeButton);
    }
    wrapper.insert(this.buttons.replace(this.buttonsPlaceholder));
    // Maximize the targetElement
    this.resizeTextArea(targetElement);
    // Make sure to resize the targetElement when the window dimensions are changed. Both document and window are monitored,
    // since different browsers send events to different elements.
    this.resizeListener = this.resizeTextArea.bind(this, targetElement);
    // document.observe('resize', this.resizeListener);
    Event.observe(window, 'resize', this.resizeListener);
    // Show the exit buttons
    this.closeButton.show();
    this.actionCloseButtonWrapper.show();
  },
  /**
   * Restore the layout.
   */
  closeFullScreen : function() {
    var targetElement = this.maximized;
    // Hide the exit buttons
    this.closeButton.hide();
    this.actionCloseButtonWrapper.hide();
    // We're no longer interested in resize events
    document.stopObserving('resize', this.resizeListener);
    Event.stopObserving(window, 'resize', this.resizeListener);
    // Restore the toolbar and action buttons to their initial position
    this.buttonsPlaceholder.replace(this.buttons);
    if (this.toolbar) {
      if (this.toolbar.hasClassName("leftmenu2")) {
        this.toolbarPlaceholder.replace(this.toolbar);
      }
      // Replace the Restore button in the toolbar with the Maximize one
      this.closeButton.replace(targetElement._fullScreenButton);
    }
    // Remove the wrapper
    // targetElement.parentNode.parentNode.replaceChild(targetElement, targetElement.parentNode);
    targetElement.parentNode.removeClassName("fullScreenWrapper");
    if (targetElement.hasClassName("xRichTextEditor")) {
      var iframe = targetElement.down(".gwt-RichTextArea");
      iframe.setStyle(iframe._originalStyle);
      iframe.setStyle({width : "100%"});
    } else if (targetElement.hasClassName("mceEditorContainer")) {
      var iframe = targetElement.down(".mceEditorIframe");
      iframe.setStyle(iframe._originalStyle);
    }

    // Restore the previous layout
    var parent = targetElement.parentNode;
    parent.setStyle({"paddingTop" : parent._originalPadding});
    while (parent != document.body) {
      parent.setStyle(parent._originalStyle);
      parent = parent.parentNode;
    }
    document.body.setStyle(document.body._originalStyle);
    document.body.setStyle({width : "auto", height: "auto"});
    // targetElement.setStyle(targetElement._originalStyle)
    if (Prototype.Browser.IE){
      // IE crashes if we try to resize this without a bit of delay.
      setTimeout(function() {
        this.setStyle({width : "100%", height: "auto"});
      }.bind(targetElement), 500);
    } else {
      targetElement.setStyle({width : "100%", height: "auto"});
    }
    // No element is maximized anymore
    delete this.maximized;
  },
  /**
   * In full screen, when the containers's dimensions change, the maximized element must be resized accordingly.
   */
  resizeTextArea : function(targetElement) {
    if (!this.maximized) {
      return;
    }
    var topMargin = targetElement.positionedOffset().top;
    // Window height - margin (for the toolbar) - styling padding - buttons
    var newHeight = document.viewport.getHeight() - topMargin - this.margin - this.buttons.getHeight();
    var newWidth = document.viewport.getWidth() - this.margin;
    // More IE bugs: the textarea scrollbar is outside the visible area
	  if (Prototype.Browser.IE && targetElement.tagName.toLowerCase() == "textarea") {
      newWidth -= 24;
    }
    // IE under Wine doesn't work well, returning 0 for viewport.getWidth
    if(newWidth <= 0) {
      targetElement.setStyle({'width' :  '100%', 'height' :  '100%'});
    } else {
      targetElement.setStyle({'width' :  newWidth + 'px', 'height' :  newHeight + 'px'});
      if (targetElement.hasClassName("xRichTextEditor")) {
        targetElement.down(".gwt-RichTextArea").setStyle({'width' :  newWidth + 'px', 'height' : newHeight - targetElement.down(".xToolbar").getHeight() - targetElement.down(".gwt-MenuBar").getHeight() + 'px'});
      } else if (targetElement.hasClassName("mceEditorContainer")) {
        targetElement.down(".mceEditorIframe").setStyle({'width' :  newWidth + 'px', 'height' : newHeight - this.toolbar.getHeight() + 'px'});
      }
    }
  }.
  /**
   * onMouseDown handler that prevents dragging the button.
   */
  preventDrag : function(event) {
    event.stop();
  }
});

// Create the fullscreen behavior on startup.
// Don't listen to dom:loaded, since the WYSIWYG editors are loaded later.
Event.observe(window, 'load', function() {
  if(Prototype.Browser.IE) {
    // In IE there is a race condition between the WYSIWYGs and this script. Allow the WYSIWYG to initialize before inserting buttons.
    setTimeout("new XWiki.dataEditors.FullScreenEditing();", 500);
  } else {
    new XWiki.dataEditors.FullScreenEditing();
  }
});