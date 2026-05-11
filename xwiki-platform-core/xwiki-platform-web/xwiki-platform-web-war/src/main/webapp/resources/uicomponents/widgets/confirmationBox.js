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
// Make sure the XWiki 'namespace' and the ModalPopup class exist.
if(typeof(XWiki) == "undefined" || typeof(XWiki.widgets) == "undefined" || typeof(XWiki.widgets.ModalPopup) == "undefined") {
  if (typeof console != "undefined" && typeof console.warn == "function") {
    console.warn("[ConfirmationBox widget] Required class missing: XWiki.widgets.ModalPopup");
  }
} else {
  /**
   * A general purpose modal confirmation dialog widget for requesting user confirmation on an action.
   * Features:
   * <ul>
   * <li>Displays a confirmation question with customizable text.</li>
   * <li>Multiple action buttons: Yes, No, and optional Cancel.</li>
   * <li>Customizable callbacks for Yes, No, and Cancel actions.</li>
   * </ul>
   * To display a confirmation dialog, create a new XWiki.widgets.ConfirmationBox object. Constructor parameters:
   * <dl>
   *   <dt>behavior (optional)</dt>
   *   <dd>An object containing callback functions:
   *   <ul>
   *     <li><tt>onYes</tt>: callback when the user picks the Yes option</li>
   *     <li><tt>onNo</tt>: callback when the user picks the No option</li>
   *     <li><tt>onCancel</tt>: callback when the user picks the Cancel option</li>
   *   </ul>
   *   </dd>
   *   <dt>interactionParameters (optional)</dt>
   *   <dd>Configuration options:
   *   <ul>
   *     <li><tt>confirmationText</tt>: the question or message to display (defaults to a localized message). Since
   *     18.4.0RC1 and 17.10.9, its values is used as plain text. Before, it was used as HTML content. You can check for
   *     the presence of the ConfirmationBox.textContentFormat static method to know which behavior applies at runtime.
   *     </li>
   *     <li><tt>yesButtonText</tt>: text for the Yes button (defaults to localized "Yes")</li>
   *     <li><tt>noButtonText</tt>: text for the No button (defaults to localized "No")</li>
   *     <li><tt>cancelButtonText</tt>: text for the Cancel button (defaults to localized "Cancel")</li>
   *     <li><tt>showCancelButton</tt>: boolean to show/hide the Cancel button (defaults to false)</li>
   *   </ul>
   *   </dd>
   * </dl>
   */
  XWiki.widgets.ConfirmationBox = Class.create(XWiki.widgets.ModalPopup, {
    /** Default displayed texts */
    defaultInteractionParameters : {
      confirmationText: "$services.localization.render('core.widgets.confirmationBox.defaultQuestion')",
      yesButtonText: "$services.localization.render('core.widgets.confirmationBox.button.yes')",
      noButtonText: "$services.localization.render('core.widgets.confirmationBox.button.no')",
      cancelButtonText: "$services.localization.render('core.widgets.confirmationBox.button.cancel')",
      showCancelButton: false
    },
    /** Constructor. Registers the key listener that pops up the dialog. */
    initialize : function($super, behavior, interactionParameters) {
      this.interactionParameters = Object.extend(Object.clone(this.defaultInteractionParameters), interactionParameters || {});
      var buttons = {
        "show"  : { method : this.showDialog,  keys : [] },
        "yes"   : { method : this.onYes,       keys : ['Enter', 'Space', 'y'] },
        "no"    : { method : this.onNo,        keys : ['n'] },
        "close" : { method : this.closeDialog, keys : ['c'] }
      };
      if (this.interactionParameters.showCancelButton) {
        buttons.close.keys.push('Esc');
      } else {
        buttons.no.keys.push('Esc');
      }
      $super(
        this.createContent(this.interactionParameters),
        buttons,
        {
          displayCloseButton : false,
          removeOnClose : true
        }
      );
      this.showDialog();
      this.setClass("confirmation");
      this.behavior = behavior || { };
    },
    /** Create the content of the confirmation dialog: icon + question text, buttons */
    createContent : function (data) {
      var question = new Element("div", {"class" : "question"});
      question.textContent = data.confirmationText;
      var buttons = new Element("div", {"class" : "buttons"});
      var yesButton = this.createButton("button", data.yesButtonText, "(Enter)", "");
      Event.observe(yesButton, "click", this.onYes.bindAsEventListener(this));
      buttons.insert(yesButton);
      var noButton = this.createButton("button", data.noButtonText, data.showCancelButton ? "(n)" : "(Esc)", "", "secondary");
      Event.observe(noButton, "click", this.onNo.bindAsEventListener(this));
      buttons.insert(noButton);
      if (data.showCancelButton) {
        var cancelButton = this.createButton("button", data.cancelButtonText, "(Esc)", "", "cancel secondary");
        Event.observe(cancelButton, "click", this.onCancel.bindAsEventListener(this));
        buttons.insert(cancelButton);
      }
      var content =  new Element("div");
      content.insert(question).insert(buttons);
      return content;
    },
    onYes : function(event) {
      this.closeDialog();
      if (typeof (this.behavior.onYes) == 'function') {
        this.behavior.onYes(event);
      }
    },
    onNo : function(event) {
      this.closeDialog();
      if (typeof (this.behavior.onNo) == 'function') {
        this.behavior.onNo(event);
      }
    },
    onCancel : function(event) {
      this.closeDialog();
      if (typeof (this.behavior.onCancel) == 'function') {
        this.behavior.onCancel(event);
      }
    }
  });

  /**
   * Indicates how the textContent parameter is interpreted by XWiki.widgets.ConfirmationBox.
   * Callers can check for the presence of this method to decide whether to escape their input:
   * When this method is available, the textContent parameter is treated as plain text and must not be escaped.
   * When this method is missing (before 18.4.0RC1/17.10.9), the textContent parameter is interpreted as HTML and may
   * need to be escaped.
   * The returned value is always the "plain" string.
   * @return {string} the expected format of the text property
   * @since 18.4.0RC1
   * @since 17.10.9
   */
  XWiki.widgets.ConfirmationBox.textContentFormat = () => "plain";
} // if the parent widget is defined
