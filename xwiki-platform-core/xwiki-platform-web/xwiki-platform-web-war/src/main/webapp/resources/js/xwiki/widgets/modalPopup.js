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
widgets.ModalPopup = Class.create({
  /** Configuration. Empty values will fall back to the CSS. */
  options : {
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
    onClose : Prototype.emptyFunction
  },
  /** Constructor. Registers the key listener that pops up the dialog. */
  initialize : function(content, shortcuts, options) {
    /** Shortcut configuration. Action name -&gt; {method: function(evt), keys: string[]}. */
    this.shortcuts = {
      "show" : { method : this.showDialog, keys : ['Ctrl+G', 'Meta+G']},
      "close" : { method : this.closeDialog, keys : ['Esc']}
    };

    this.content = content || "Hello world!";
    // Add the new shortcuts
    this.shortcuts = Object.extend(Object.clone(this.shortcuts), shortcuts || { });
    // Add the custom options
    this.options = Object.extend(Object.clone(this.options), options || { });
    // Register a shortcut for showing the dialog.
    this.registerShortcuts("show");
  },
  /** Create the dialog, if it is not already loaded. Otherwise, just make it visible again. */
  createDialog : function() {
    // The dialog
    this.dialog = new Element('dialog', {'class': 'xdialog-box'});
    // A full-screen semi-transparent screen covering the main document
    if (this.options.screenOpacity > 0) {
      /* We cannot just change the pseudo element color like that. We need to use a CSS property here. */
      this.dialog.style.setProperty('--xdialog-backdrop-opacity', this.options.screenColor);
    }
    if (this.options.screenColor) {
      /* We cannot just change the pseudo element color like that. We need to use a CSS property here. */
      this.dialog.style.setProperty('--xdialog-backdrop-color', this.options.screenColor);
    }
    
    if (this.options.extraClassName) {
      this.dialog.addClassName(this.options.extraClassName);
    }
    // Insert the content
    this.dialog._x_contentPlug = new Element('div', {'class' : 'xdialog-content'});
    this.dialog.update(this.dialog._x_contentPlug);
    this.dialog._x_contentPlug.update(this.content);
    // Add the dialog title
    if (this.options.title) {
      var title = new Element('div', {'class': 'xdialog-title'}).update(this.options.title);
      title.setStyle({"color" : this.options.titleColor});
      this.dialog.insertBefore(title, this.dialog.firstChild);
    }
    // Add the close button
    if (this.options.displayCloseButton) {
      let closeButtonHint = "$!escapetool.javascript($services.localization.render('core.xdialog.close.hint'))";
      var closeButton = new Element('button', {'class': 'close xdialog-close', 'title': closeButtonHint})
        .update("$!escapetool.javascript($services.icon.renderHTML('cross'))");
      let textAlternative = new Element("div", {
        "class" : "sr-only"
      }).update(closeButtonHint);
      closeButton.appendChild(textAlternative);
      closeButton.observe("click", this.closeDialog.bindAsEventListener(this));
      if (this.options.title) {
        title.insert({bottom: closeButton});
        if (this.options.titleColor) {
          closeButton.setStyle({"color": this.options.titleColor});
        }
      } else {
        this.dialog.insertBefore(closeButton, this.dialog.firstChild);
      }
    }
    this.dialog.setStyle({
      "borderColor": this.options.borderColor,
      "backgroundColor" : this.options.backgroundColor
    });
    switch(this.options.verticalPosition) {
      case "top":
        this.dialog.setStyle({"margin-top": "30px"});
        break;
      case "bottom":
        this.dialog.setStyle({"margin-bottom": "30px"});
        break;
      default:
        break;
    }
    switch(this.options.horizontalPosition) {
      case "left":
        this.dialog.setStyle({"margin-left": "10%", "margin-right": "auto"});
        break;
      case "right":
        this.dialog.setStyle({"margin-right": "10%", "margin-left": "auto"});
        break;
      default:
        this.dialog.setStyle({"margin-right": "auto", "margin-left": "auto"});
        break;
    }
    // Append to the end of the document body.
    $('body').appendChild(this.dialog);
    this.dialog.hide();
  },
  /** Set a class name to the dialog box */
  setClass : function(className) {
    this.dialog.addClassName('xdialog-box-' + className);
  },
  /** Remove a class name from the dialog box */
  removeClass : function(className) {
    this.dialog.removeClassName('xdialog-box-' + className);
  },
  /** Set the content of the dialog box */
  setContent : function(content) {
     this.content = content;
     this.dialog._x_contentPlug.update(this.content);
  },
  /** Called when the dialog is displayed. Enables the key listeners and gives focus to the (cleared) input. */
  showDialog : function(event) {
    if (event) {
      Event.stop(event);
    }
    // Only do this if the dialog is not already active.
    if (this.options.globalDialog) {
      if (widgets.ModalPopup.active) {
        return;
      } else {
        widgets.ModalPopup.active = true;
      }
    } else {
      if (this.active) {
        return;
      } else {
        this.active = true;
      }
    }
    if (!this.dialog) {
      // The dialog wasn't loaded, create it.
      this.createDialog();
    }
    // Start listening to keyboard events
    this.attachKeyListeners();
    // Display the dialog
    // We cannot rely on prototype .show() to remove the unwanted style since there's a collision with the 
    // native HTML dialog method .show()
    this.dialog.style.removeProperty('display');
    this.dialog.showModal();
  },
  /** Called when the dialog is closed. Disables the key listeners, hides the UI and re-enables the 'Show' behavior. */
  closeDialog : function(event) {
    if (event) {
      Event.stop(event);
    }
    // Call optional callback
    this.options.onClose.call(this);
    // Hide the dialog, without removing it from the DOM.
    this.dialog.hide();
    if (this.options.removeOnClose) {
      this.dialog.remove();
    }
    // Stop the UI shortcuts (except the initial Show Dialog one).
    this.detachKeyListeners();
    // Re-enable the 'show' behavior.
    if (this.options.globalDialog) {
      widgets.ModalPopup.active = false;
    } else {
      this.active = false;
    }
  },
  /** Enables all the keyboard shortcuts, except the one that opens the dialog, which is already enabled. */
  attachKeyListeners : function() {
    for (var action in this.shortcuts) {
      if (action != "show") {
        this.registerShortcuts(action);
      }
    }
  },
  /** Disables all the keyboard shortcuts, except the one that opens the dialog. */
  detachKeyListeners : function() {
    for (var action in this.shortcuts) {
      if (action != "show") {
        this.unregisterShortcuts(action);
      }
    }
  },
  /**
   * Enables the keyboard shortcuts for a specific action.
   *
   * @param {String} action The action to register
   * {@see #shortcuts}
   */
  registerShortcuts : function(action) {
    var shortcuts = this.shortcuts[action].keys;
    var method = this.shortcuts[action].method.bindAsEventListener(this, action);
    var options = this.shortcuts[action].options;
    for (var i = 0; i < shortcuts.length; ++i) {
      shortcut.add(shortcuts[i], method, options);
    }
  },
  /**
   * Disables the keyboard shortcuts for a specific action.
   *
   * @param {String} action The action to unregister {@see #shortcuts}
   */
  unregisterShortcuts : function(action) {
    for (var i = 0; i < this.shortcuts[action].keys.length; ++i) {
      shortcut.remove(this.shortcuts[action].keys[i]);
    }
  },
  createButton : function(type, text, title, id, extraClass) {
    var wrapper = new Element("span", {"class" : "buttonwrapper"});
    var button = new Element("input", {
      "type" : type,
      "class" : "button",
      "value" : text,
      "title" : title,
      "id" : id
    });
    let textAlternative = new Element("div", {
      "class" : "sr-only"
    }).update(title);
    button.appendChild(textAlternative);
    if (extraClass) {
      button.addClassName(extraClass);
    }
    wrapper.update(button);
    return wrapper;
  }
});
/** Whether or not the dialog is already active (or activating). */
widgets.ModalPopup.active = false;
// End XWiki augmentation.
return XWiki;
}(XWiki || {}));

// When the document is loaded, enable the keyboard listener that triggers the dialog.
// document.observe("xwiki:dom:loaded", function() {
//   new XWiki.widgets.ModalPopup("An example dialog",
//     { "show" : { method : "this.createDialog", keys : ['Ctrl+Y', 'Meta+Y']} },
//     { title: "Example", titleColor: "#369", borderColor: "#369", screenColor: "#FFF" }
//   );
// });
