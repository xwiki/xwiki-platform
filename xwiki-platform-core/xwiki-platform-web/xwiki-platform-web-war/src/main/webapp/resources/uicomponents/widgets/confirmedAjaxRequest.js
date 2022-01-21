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
if(typeof(XWiki) == "undefined" || typeof(XWiki.widgets) == "undefined" || typeof(XWiki.widgets.ConfirmationBox) == "undefined") {
  if (typeof console != "undefined" && typeof console.warn == "function") {
    console.warn("[MessageBox widget] Required class missing: XWiki.widgets.ModalPopup");
  }
} else {
/**
 * An AJAX request performed only if the user confirms the action in a modal dialog. It also displays (if configured)
 * notification messages for progress, success and failure.
 * <p>Usage:</p>
 * <code>new XWiki.widgets.ConfirmedAjaxRequest(requestURL, ajaxRequestParameters, interactionParameters)</code>
 * where:
 * <dl>
 *  <dt>requestURL</dl>
 *  <dd>the target URL, which is automatically postfixed with "ajax=1"</dd>
 *  <dt>ajaxRequestParameters</dt>
 *  <dd>the options accepted by Prototype's Ajax.Request, see <a href="http://www.prototypejs.org/api/ajax/options">the documentation page</a></dd>
 *  <dt>interactionParameters</dt>
 *  <dd>text for the messages displayed to the user:
 *    <ul>
 *      <li><tt>confirmationText</tt>: the question in the modal dialog. Default: "Are you sure?"</li>
 *      <li><tt>yesButtonText</tt>: the text for the "Yes" button. Default: "Yes"</li>
 *      <li><tt>noButtonText</tt>: the text for the "No" button. Default: "No"</li>
 *      <li><tt>displayProgressMessage</tt>: whether or not to display a notification that the request is in progress. Default: true</li>
 *      <li><tt>progressMessageText</tt>: the text for the "in progress" notification. Default: "Sending request..."</li>
 *      <li><tt>displaySuccessMessage</tt>: whether or not to display a notification that the request was successful. Default: true</li>
 *      <li><tt>successMessageText</tt>: the text for the "success" notification. Default: "Done!"</li>
 *      <li><tt>displayFailureMessage</tt>: whether or not to display a notification that the request failed. Default: true</li>
 *      <li><tt>failureMessageText</tt>: the text for the "failed" notification. Default: "Failed: &lt;status text&gt;"</li>
 * </dl>
 */
XWiki.widgets.ConfirmedAjaxRequest = Class.create(XWiki.widgets.ConfirmationBox, {
  /** Some functions to fix several browser specific problems */
  defaultAjaxRequestParameters : {
    // 0 is returned for network failures.
    on0 : function(response) {
      response.request.options.onFailure(response);
    }
  },
  /** Constructor. Registers the key listener that pops up the dialog. */
  initialize : function($super, requestUrl, ajaxRequestParameters, interactionParameters) {
    this.interactionParameters = Object.extend({
      displayProgressMessage: true,
      progressMessageText : "$services.localization.render('core.widgets.confirmationBox.notification.inProgress')",
      displaySuccessMessage: true,
      successMessageText : "$services.localization.render('core.widgets.confirmationBox.notification.done')",
      displayFailureMessage: true,
      failureMessageText : "$services.localization.render('core.widgets.confirmationBox.notification.failed')"
    }, interactionParameters || {});
    this.requestUrl = requestUrl;
    this.ajaxRequestParameters = Object.extend(Object.clone(this.defaultAjaxRequestParameters), ajaxRequestParameters || {});
    Object.extend(this.ajaxRequestParameters, {
      onSuccess : function() {
        if (this.interactionParameters.displaySuccessMessage) {
          if (this.progressNotification) {
            this.progressNotification.replace(new XWiki.widgets.Notification(this.interactionParameters.successMessageText, "done"));
          } else {
            new XWiki.widgets.Notification(this.interactionParameters.successMessageText, "done");
          }
        } else if (this.progressNotification) {
          this.progressNotification.hide();
        }
        if (ajaxRequestParameters.onSuccess) {
          ajaxRequestParameters.onSuccess.apply(this, arguments);
        }
      }.bind(this),
      onFailure : function(response) {
        if (this.interactionParameters.displayFailureMessage) {
          var failureReason = response.statusText || 'Server not responding';
          if (this.progressNotification) {
            this.progressNotification.replace(new XWiki.widgets.Notification(this.interactionParameters.failureMessageText + failureReason, "error"));
          } else {
            new XWiki.widgets.Notification(this.interactionParameters.failureMessageText + failureReason, "error");
          }
        } else if (this.progressNotification) {
          this.progressNotification.hide();
        }
        if (ajaxRequestParameters.onFailure) {
          ajaxRequestParameters.onFailure.apply(this, arguments);
        }
      }.bind(this)
    });
    $super({
      onYes : function() {
        if (this.interactionParameters.displayProgressMessage) {
          this.progressNotification = new XWiki.widgets.Notification(this.interactionParameters.progressMessageText, "inprogress");
        }
        // perform the ajax request
        new Ajax.Request(this.requestUrl, this.ajaxRequestParameters);
      }.bind(this)
    }, this.interactionParameters
    );
  }
});

} // if the parent widget is defined