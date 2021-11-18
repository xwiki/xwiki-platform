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
 * A general purpose notification class, displaying a simple message for the user, at the bottom of the screen.
 * Features:
 * <ul>
 * <li>Several default aspects: <tt>plain</tt>, <tt>info</tt>, <tt>warning</tt>, <tt>error</tt>, <tt>inprogress</tt>,
 *  <tt>done</tt>.</li>
 * <li>Stacking of multiple notifications on the screen.</li>
 * <li>Possibility to replace a notification with another one, preserving the position.</li>
 * <li>Automatic hide after a configurable period of time.</li>
 * <li> After hiding, call the function specified in the options.</li>
 * <li>Configurable icon, background and text color.</li>
 * </ul>
 * To display a notification, it suffices to create a new XWiki.widgets.Notification object. Constructor parameters:
 * <dl>
 *   <dt>text</dt>
 *   <dd>The notification text</dd>
 *   <dt>type (optional)</dt>
 *   <dd>The notification type, one of <tt>plain</tt>, <tt>info</tt>, <tt>warning</tt>, <tt>error</tt>, <tt>inprogress</tt>,
 *    <tt>done</tt>. If an unknown or no type is passed, <tt>plain</tt> is used by default.</dd>
 *   <dt>options (optional)</dt>
 *   <dd>Additional configuration; supported options:
 *   <ul>
 *     <li><tt>timeout</tt>: number of seconds to keep the notification on the screen. Use 0 or false to keep it until manually removed.</li>
 *     <li><tt>inactive</tt>: don't show the notification when the object is created, manually call {@link #show} later</li>
 *     <li><tt>icon</tt>: a custom image to use</li>
 *     <li><tt>color</tt>: a custom color for the text</li>
 *     <li><tt>backgroundColor</tt>: a custom color for the background</li>
 *   </ul>
 *   </dd>
 * </dl>
 * Default parameters for the predefined notification types:
 * <dl>
 *   <dt>plain</dt>
 *   <dd>timeout: 5</dd>
 *   <dd>icon: none</dd>
 *   <dd>color: black</dd>
 *   <dd>background: #EEE</dd>
 *   <dt>info</dt>
 *   <dd>timeout: 5</dd>
 *   <dd>icon: (i)</dd>
 *   <dd>color: #28C</dd>
 *   <dd>background: #DDF</dd>
 *   <dt>warning</dt>
 *   <dd>timeout: 5</dd>
 *   <dd>icon: /!\</dd>
 *   <dd>color: 000</dd>
 *   <dd>background: #FFD</dd>
 *   <dt>error</dt>
 *   <dd>timeout: 10</dd>
 *   <dd>icon: (!)</dd>
 *   <dd>color: #900</dd>
 *   <dd>background: #EDD</dd>
 *   <dt>inprogress</dt>
 *   <dd>timeout: false</dd>
 *   <dd>icon: spinning</dd>
 *   <dd>color: #268</dd>
 *   <dd>background: #EEE</dd>
 *   <dt>done</dt>
 *   <dd>timeout: 2</dd>
 *   <dd>icon: (v)</dd>
 *   <dd>color: #090</dd>
 *   <dd>background: #EFD</dd>
 * </dl>
 */
widgets.Notification = Class.create({
  text : "Hello world!",
  defaultOptions : {
    /** supported types: plain, info, warning, error, inprogress, done */
    "plain"      : {timeout : 5},
    "info"       : {timeout : 5},
    "warning"    : {timeout : 5},
    "error"      : {timeout : 10},
    "inprogress" : {timeout : false},
    "done"       : {timeout : 2}
  },
  initialize : function(text, type, options) {
    this.text = text || this.text;
    this.type = (typeof this.defaultOptions[type] != "undefined") ? type : "plain";
    this.options = Object.extend(Object.clone(this.defaultOptions[this.type]), options || { });
    this.createElement();
    if (!this.options.inactive) {
      this.show();
    }
  },
  /** Creates the HTML structure for the notification. */
  createElement : function() {
    if (!this.element) {
      this.element = new Element("div", {"class" : "xnotification xnotification-" + this.type}).update(this.text);
      if (this.options.icon) {
        this.element.setStyle({backgroundImage : this.options.icon, paddingLeft : "22px"});
      }
      if (this.options.backgroundColor) {
        this.element.setStyle({backgroundColor : this.options.backgroundColor});
      }
      if (this.options.color) {
        this.element.setStyle({color : this.options.color});
      }
      this.element = this.element.wrap(new Element("div", {"class" : "xnotification-wrapper"}));
      Event.observe(this.element, "click", this.hide.bindAsEventListener(this));
    }
  },
  /** Display the notification and schedule an automatic hide after the configured period of time, if any. */
  show : function() {
    if (!this.element.descendantOf(widgets.Notification.getContainer())) {
      widgets.Notification.getContainer().insert({top: this.element});
    }
    this.element.show();
    if (this.options.timeout) {
      this.timer = window.setTimeout(this.hide.bind(this), this.options.timeout * 1000);
    }
  },
  /** Hide the notification. */
  hide : function() {
    this.element.hide();
    if (this.element.parentNode) {
      this.element.remove();
    }
    if (this.timer) {
      window.clearTimeout(this.timer);
      this.timer = null;
    }
    (typeof this.options.onHide == 'function') && this.options.onHide();
  },
  /** Silently replace this notification with another one, keeping the same place. */
  replace : function(notification) {
    if (this.element.parentNode) {
      this.element.replace(notification.element);
    }
    if (this.timer) {
      window.clearTimeout(this.timer);
      this.timer = null;
    }
    notification.show();
  }
});

/** The container for all the notifications. */
widgets.Notification.container = null;

/** Returns the container for all the notifications. The container is created the first time this function is called. */
widgets.Notification.getContainer = function() {
  if (!widgets.Notification.container) {
    widgets.Notification.container = new Element('div', {"class" : "xnotification-container"});
    // Make notifications alert / accessible for screen readers
    widgets.Notification.container.writeAttribute("role", "alert");
    // Insert the container in the document body.
    $('body').insert(widgets.Notification.container);
  }
  return widgets.Notification.container;
};

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
