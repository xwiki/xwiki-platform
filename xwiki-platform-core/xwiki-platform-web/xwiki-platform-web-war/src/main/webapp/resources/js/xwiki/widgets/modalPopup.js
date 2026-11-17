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

function createElement(tagName, classNames) {
  const element = document.createElement(tagName);
  addClassNames(element, classNames);
  return element;
}

/** Adds one or more (space separated) class names to the given element. */
function addClassNames(element, classNames) {
  element.classList.add(...(classNames || '').split(/\s+/).filter(className => className));
}

/** Replaces the content of the given element with the given DOM node or HTML string. */
function setElementContent(element, content) {
  if (content instanceof Node) {
    element.replaceChildren(content);
  } else {
    element.innerHTML = content;
  }
}

function stopEvent(event) {
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }
}

/**
 * Note that this class can be extended using Prototype.js' Class.create(XWiki.widgets.ModalPopup, {...}). This is why
 * the constructor only delegates to the initialize method (Prototype.js calls only the initialize method when creating
 * an instance of a subclass).
 */
class ModalPopup {
  constructor(...args) {
    this.initialize(...args);
  }

  /** Constructor. Registers the key listener that pops up the dialog. */
  initialize(content, shortcuts, options) {
    /** Shortcut configuration. Action name -&gt; {method: function(evt), keys: string[]}. */
    this.shortcuts = {
      "show" : { method : this.showDialog, keys : ['Ctrl+G', 'Meta+G']},
      "close" : { method : this.closeDialog, keys : ['Esc']},
      // Add the new shortcuts
      ...shortcuts
    };
    this.content = content || "Hello world!";
    // Add the custom options
    this.options = {...this.options, ...options};
    // Register a shortcut for showing the dialog.
    this.registerShortcuts("show");
  }

  /** Create the dialog, if it is not already loaded. Otherwise, just make it visible again. */
  createDialog(event) {
    this.dialog = createElement('div', 'xdialog-modal-container');
    // A full-screen semi-transparent screen covering the main document
    const screen = createElement('div', 'xdialog-screen');
    Object.assign(screen.style, {
      opacity : this.options.screenOpacity,
      backgroundColor : this.options.screenColor
    });
    this.dialog.replaceChildren(screen);
    // The dialog chrome
    this.dialogBox = createElement('div', 'xdialog-box');
    if (this.options.extraClassName) {
      addClassNames(this.dialogBox, this.options.extraClassName);
    }
    // Insert the content
    this.dialogBox._x_contentPlug = createElement('div', 'xdialog-content');
    this.dialogBox.replaceChildren(this.dialogBox._x_contentPlug);
    setElementContent(this.dialogBox._x_contentPlug, this.content);
    // Add the dialog title
    let title;
    if (this.options.title) {
      title = createElement('div', 'xdialog-title');
      setElementContent(title, this.options.title);
      title.style.color = this.options.titleColor;
      this.dialogBox.insertBefore(title, this.dialogBox.firstChild);
    }
    // Add the close button
    if (this.options.displayCloseButton) {
      const closeButton = createElement('button', 'close xdialog-close');
      closeButton.title = 'Close';
      closeButton.innerHTML = "$!escapetool.javascript($services.icon.renderHTML('cross'))";
      closeButton.addEventListener('click', event => this.closeDialog(event));
      if (this.options.title) {
        title.append(closeButton);
        if (this.options.titleColor) {
          closeButton.style.color = this.options.titleColor;
        }
      } else {
        this.dialogBox.insertBefore(closeButton, this.dialogBox.firstChild);
      }
    }
    this.dialog.appendChild(this.dialogBox);
    Object.assign(this.dialogBox.style, {
      "textAlign": "left",
      "borderColor": this.options.borderColor,
      "backgroundColor" : this.options.backgroundColor
    });
    switch(this.options.verticalPosition) {
      case "top":
        this.dialogBox.style.top = "30px";
        break;
      case "bottom":
        this.dialogBox.style.bottom = "30px";
        break;
      default:
        // TODO: smart alignment according to the actual height
        this.dialogBox.style.top = "35%";
        break;
    }
    switch(this.options.horizontalPosition) {
      case "left":
        this.dialog.style.textAlign = "left";
        break;
      case "right":
        this.dialog.style.textAlign = "right";
        break;
      default:
        this.dialog.style.textAlign = "center";
        this.dialogBox.style.margin = "auto";
      break;
    }
    // Append to the end of the document body.
    document.body.appendChild(this.dialog);
    this.dialog.style.display = 'none';
  }

  /** Set a class name to the dialog box */
  setClass(className) {
    this.dialogBox.classList.add('xdialog-box-' + className);
  }

  /** Remove a class name from the dialog box */
  removeClass(className) {
    this.dialogBox.classList.remove('xdialog-box-' + className);
  }

  /** Set the content of the dialog box */
  setContent(content) {
    this.content = content;
    setElementContent(this.dialogBox._x_contentPlug, this.content);
  }

  /** Called when the dialog is displayed. Enables the key listeners and gives focus to the (cleared) input. */
  showDialog(event) {
    stopEvent(event);
    // Only do this if the dialog is not already active.
    if (this.options.globalDialog) {
      if (ModalPopup.active) {
        return;
      } else {
        ModalPopup.active = true;
      }
    } else if (this.active) {
      return;
    } else {
      this.active = true;
    }
    if (!this.dialog) {
      // The dialog wasn't loaded, create it.
      this.createDialog();
    }
    // Start listening to keyboard events
    this.attachKeyListeners();
    // Display the dialog
    this.dialog.style.display = '';
  }

  /** Called when the dialog is closed. Disables the key listeners, hides the UI and re-enables the 'Show' behavior. */
  closeDialog(event) {
    stopEvent(event);
    // Call optional callback
    this.options.onClose.call(this);
    // Hide the dialog, without removing it from the DOM.
    this.dialog.style.display = 'none';
    if (this.options.removeOnClose) {
      this.dialog.remove();
    }
    // Stop the UI shortcuts (except the initial Show Dialog one).
    this.detachKeyListeners();
    // Re-enable the 'show' behavior.
    if (this.options.globalDialog) {
      ModalPopup.active = false;
    } else {
      this.active = false;
    }
  }

  /** Enables all the keyboard shortcuts, except the one that opens the dialog, which is already enabled. */
  attachKeyListeners() {
    for (const action in this.shortcuts) {
      if (action != "show") {
        this.registerShortcuts(action);
      }
    }
  }

  /** Disables all the keyboard shortcuts, except the one that opens the dialog. */
  detachKeyListeners() {
    for (const action in this.shortcuts) {
      if (action != "show") {
        this.unregisterShortcuts(action);
      }
    }
  }

  /**
   * Enables the keyboard shortcuts for a specific action.
   *
   * @param {String} action The action to register
   * {@see #shortcuts}
   */
  registerShortcuts(action) {
    const shortcuts = this.shortcuts[action].keys;
    const method = this.shortcuts[action].method;
    const listener = event => method.call(this, event, action);
    const options = this.shortcuts[action].options;
    for (const key of shortcuts) {
      shortcut.add(key, listener, options);
    }
  }

  /**
   * Disables the keyboard shortcuts for a specific action.
   *
   * @param {String} action The action to unregister {@see #shortcuts}
   */
  unregisterShortcuts(action) {
    for (const key of this.shortcuts[action].keys) {
      shortcut.remove(key);
    }
  }

  createButton(type, text, title, id, extraClass) {
    const wrapper = createElement("span", "buttonwrapper");
    const button = createElement("input", "button");
    button.type = type;
    button.value = text;
    if (title) {
      button.title = title;
    }
    if (id) {
      button.id = id;
    }
    if (extraClass) {
      addClassNames(button, extraClass);
    }
    wrapper.replaceChildren(button);
    return wrapper;
  }
}

/** Configuration. Empty values will fall back to the CSS. */
ModalPopup.prototype.options = {
  globalDialog : true,
  title : "",
  displayCloseButton : true,
  extraClassName : false,
  screenColor : "",
  borderColor : "",
  titleColor : "",
  backgroundColor : "",
  screenOpacity : "0.5",
  verticalPosition : "center",
  horizontalPosition : "center",
  removeOnClose : false,
  onClose : () => {}
};

/** Whether or not the dialog is already active (or activating). */
ModalPopup.active = false;

/** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
ModalPopup.subclasses = [];

widgets.ModalPopup = ModalPopup;
// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
