// Make sure the XWiki 'namespace' and the ModalPopup class exist.
if(typeof(XWiki) == "undefined" || typeof(XWiki.widgets) == "undefined" || typeof(XWiki.widgets.ModalPopup) == "undefined") {
  if (typeof console != "undefined" && typeof console.warn == "function") {
    console.warn("[MessageBox widget] Required class missing: XWiki.widgets.ModalPopup");
  }
} else {
/**
 * An AJAX request performed only if the user confirms the action in a modal dialog. It also forwards IE specific status
 * codes for 204 responses to the correct onSuccess function, and it displays (if configured) notification messages for
 * progress, success and failure.
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
XWiki.widgets.ConfirmedAjaxRequest = Class.create(XWiki.widgets.ModalPopup, {
  /** Default displayed texts */
  defaultInteractionParameters : {
    confirmationText: "$msg.get('core.widgets.confirmationBox.defaultQuestion')",
    yesButtonText: "$msg.get('core.widgets.confirmationBox.button.yes')",
    noButtonText: "$msg.get('core.widgets.confirmationBox.button.no')",
    displayProgressMessage: true,
    progressMessageText : "$msg.get('core.widgets.confirmationBox.notification.inProgress')",
    displaySuccessMessage: true,
    successMessageText : "$msg.get('core.widgets.confirmationBox.notification.done')",
    displayFailureMessage: true,
    failureMessageText : "$msg.get('core.widgets.confirmationBox.notification.failed')"
  },
  /** Some functions to fix several browser specific problems */
  defaultAjaxRequestParameters : {
    // IE converts 204 status code into 1223...
    on1223 : function(response) {
      response.request.options.onSuccess(response);
    },
    // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
    on0 : function(response) {
      response.request.options.onFailure(response);
    }
  },
  /** Constructor. Registers the key listener that pops up the dialog. */
  initialize : function($super, requestUrl, ajaxRequestParameters, interactionParameters) {
    this.interactionParameters = Object.extend(Object.clone(this.defaultInteractionParameters), interactionParameters || {});
    $super(
      this.createContent(this.interactionParameters),
      {
        "show"  : { method : this.showDialog,  keys : [] },
        "go"    : { method : this.go,   keys : ['Enter']},
        "close" : { method : this.closeDialog, keys : ['Esc'] }
      },
      {
         displayCloseButton : false
      }
    );
    this.showDialog();
    this.setClass("confirmation");
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
          ajaxRequestParameters.onSuccess(arguments);
        }
      }.bind(this),
      onFailure : function(response) {
        if (this.interactionParameters.displayFailureMessage) {
          var failureReason = response.statusText;
          if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
            failureReason = 'Server not responding';
          }
          if (this.progressNotification) {
            this.progressNotification.replace(new XWiki.widgets.Notification(this.interactionParameters.failureMessageText + failureReason, "error"));
          } else {
            new XWiki.widgets.Notification(this.interactionParameters.failureMessageText + failureReason, "error");
          }
        } else if (this.progressNotification) {
          this.progressNotification.hide();
        }
        if (ajaxRequestParameters.onFailure) {
          ajaxRequestParameters.onFailure(arguments);
        }
      }.bind(this)
    });
  },
  /** Create the content of the confirmation dialog: icon + question text, buttons */
  createContent : function (data) {
    var question = new Element("div", {"class" : "question"}).update(data.confirmationText);
    var buttons = new Element("div", {"class" : "buttons"});
    var goButton = this.createButton("button", data.yesButtonText, "(Enter)", "");
    var cancelButton = this.createButton("button", data.noButtonText, "(Esc)", "");
    buttons.insert(goButton);
    buttons.insert(cancelButton);
    var content =  new Element("div");
    content.insert(question).insert(buttons);
    Event.observe(goButton, "click", this.go.bindAsEventListener(this));
    Event.observe(cancelButton, "click", this.closeDialog.bindAsEventListener(this));
    return content;
  },
  /** Performs the Ajax request */
  go : function () {
    this.closeDialog();
    if (this.interactionParameters.displayProgressMessage) {
      this.progressNotification = new XWiki.widgets.Notification(this.interactionParameters.progressMessageText, "inprogress");
    }
    // perform the ajax request
    new Ajax.Request(this.requestUrl, this.ajaxRequestParameters);
  }
});

} // if the parent widget is defined