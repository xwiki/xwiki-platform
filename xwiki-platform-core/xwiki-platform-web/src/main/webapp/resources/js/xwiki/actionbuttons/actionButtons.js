// ======================================
// Form buttons: shortcuts, AJAX improvements and form validation
// To be completed.

var XWiki = (function(XWiki) {
// Start XWiki augmentation.
var actionButtons = XWiki.actionButtons = XWiki.actionButtons || {};

actionButtons.EditActions = Class.create({
  initialize : function() {
    this.addListeners();
    this.addShortcuts();
    this.addValidators();
  },
  addListeners : function() {
    $$('input[name=action_cancel]').each(function(item) {
      item.observe('click', this.onCancel.bindAsEventListener(this));
    }.bind(this));
    $$('input[name=action_preview]').each(function(item) {
      item.observe('click', this.onPreview.bindAsEventListener(this));
    }.bind(this));
    $$('input[name=action_save]').each(function(item) {
      item.observe('click', this.onSaveAndView.bindAsEventListener(this));
    }.bind(this));
    $$('input[name=action_saveandcontinue]').each(function(item) {
      item.observe('click', this.onSaveAndContinue.bindAsEventListener(this));
    }.bind(this));
  },
  addShortcuts : function() {
    var shortcuts = {
      'action_cancel' : "$services.localization.render('core.shortcuts.edit.cancel')",
      'action_preview' : "$services.localization.render('core.shortcuts.edit.preview')",
      // The following 2 are both "Back to edit" in the preview mode, depending on the used editor
      'action_edit' : "$services.localization.render('core.shortcuts.edit.backtoedit')",
      'action_inline' : "$services.localization.render('core.shortcuts.edit.backtoedit')",
      'action_save' : "$services.localization.render('core.shortcuts.edit.saveandview')",
      'action_propupdate' : "$services.localization.render('core.shortcuts.edit.saveandview')",
      'action_saveandcontinue' : "$services.localization.render('core.shortcuts.edit.saveandcontinue')"
    }
    for (var key in shortcuts) {
      var targetButtons = $$("input[name=" + key + "]");
      if (targetButtons.size() > 0) {
        shortcut.add(shortcuts[key], function() {
          this.click();
        }.bind(targetButtons.first()), {'propagate' : false} );
      }
    }
  },
  validators : new Array(),
  addValidators : function() {
    // Add live presence validation for inputs with classname 'required'.
    var inputs = $('body').select("input.required");
    for (var i = 0; i < inputs.length; i++) {
      var input = inputs[i];
      var validator = new LiveValidation(input, { validMessage: "" });
      validator.add(Validate.Presence, {
        failureMessage: "$services.localization.render('core.validation.required.message')"
      });
      validator.validate();
      this.validators.push(validator);
    }
  },
  validateForm : function(form) {
    for (var i = 0; i < this.validators.length; i++) {
      if (!this.validators[i].validate()) {
        return false;
      }
    }
    var commentField = form.comment;
    if (commentField && ($xwiki.isEditCommentSuggested() || $xwiki.isEditCommentMandatory())) {
      while (commentField.value == '') {
        var response = prompt("$services.localization.render('core.comment.prompt')", '');
        if (response === null) {
          return false;
        }
        commentField.value = response;
        if (!$xwiki.isEditCommentMandatory()) break;
      }
    }
    return true;
  },
  onCancel : function(event) {
    event.stop();

    // Notify others we are going to cancel
    this.notify(event, "cancel");

    // Optimisation: Do not send the entire form's data when all we want is to cancel editing.

    // Determine the form's action and clean it by removing any anchors.
    var location = event.element().form.action;
    if (typeof location != "string") {
       location = event.element().form.attributes.getNamedItem("action");
       if (location) {
         location = location.nodeValue;
       } else {
         location = window.self.location.href;
       }
    }
    var parts = location.split('#', 2);
    var fragmentId = (parts.length == 2) ? parts[1] : '';
    location = parts[0];
    if (location.indexOf('?') == -1) {
      location += '?';
    }

    // Make sure that we call the CancelAction using XWiki's ActionFilter, no matter what XWiki action was set in the form's action.
    var cancelActionParameter = '&action_cancel=true';

    // Include the xredirect element, if it exists.
    var xredirectElement = event.element().form.elements['xredirect'];
    var xredirectParameter = xredirectElement ? '&xredirect=' + escape(xredirectElement.value) : '';

    // Optimisation: Prevent a redundant request to remove the edit lock when the page unload event is triggered. Both the cancel action
    // and the page unload event would unlock the document, so no point in doing both.
    XWiki.EditLock && XWiki.EditLock.setLocked(false);

    // Call the cancel URL directly instead of submitting the form. (optimisation)
    window.location = location + cancelActionParameter + xredirectParameter + fragmentId;
  },
  onPreview : function(event) {
    if (!this.validateForm(event.element().form)) {
      event.stop();
    } else {
      // Notify others
      this.notify(event, "preview");
    }
  },
  onSaveAndView : function(event) {
    if (!this.validateForm(event.element().form)) {
      event.stop();
    } else {
      this.notify(event, "save", {"continue" : false});
    }
  },
  onSaveAndContinue : function(event) {
    if (!this.validateForm(event.element().form)) {
      event.stop();
    } else {
      this.notify(event, "save", {"continue" : true});
    }
  },
  notify : function(event, action, params) {
    document.fire("xwiki:actions:" + action, Object.extend({originalEvent : event, form: event.element().form}, params || { }));
    // In IE, events can't be stopped from another event's handler, so we must call stop() again here
    if (event.stopped) {
      event.stop();
    }
  }
});

// ======================================
// Save and continue button: Ajax improvements
actionButtons.AjaxSaveAndContinue = Class.create({
  initialize : function() {
    this.createMessages();
    this.addListeners();
  },
  createMessages : function() {
    this.savingBox = new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('core.editors.saveandcontinue.notification.inprogress'))", "inprogress", {inactive: true});
    this.savedBox = new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('core.editors.saveandcontinue.notification.done'))", "done", {inactive: true});
    this.failedBox = new XWiki.widgets.Notification('$escapetool.javascript($services.localization.render("core.editors.saveandcontinue.notification.error", ["<span id=""ajaxRequestFailureReason""/>"]))', "error", {inactive: true});
  },
  addListeners : function() {
    document.observe("xwiki:actions:save", this.onSave.bindAsEventListener(this));
  },
  onSave : function(event) {
    // Don't continue if the event has been stopped already
    if (event.stopped) {
      return;
    }
    if (event.memo["continue"]) {
      if (typeof (event.memo.originalEvent) != 'undefined') {
        event.memo.originalEvent.stop();
      }
      this.form = $(event.memo.form);
      this.savedBox.hide();
      this.failedBox.hide();
      this.savingBox.show();
      var formData = new Hash(this.form.serialize({hash: true, submit: 'action_saveandcontinue'}));
      formData.set('minorEdit', '1');
      if (!Prototype.Browser.Opera) {
        // Opera can't handle properly 204 responses.
        formData.set('ajax', 'true');
      }
      new Ajax.Request(this.form.action, {
        method : 'post',
        parameters : formData.toQueryString(),
        onSuccess : this.onSuccess.bindAsEventListener(this),
        on1223 : this.on1223.bindAsEventListener(this),
        on0 : this.on0.bindAsEventListener(this),
        onFailure : this.onFailure.bind(this)
      });
    }
  },
  // IE converts 204 status code into 1223...
  on1223 : function(response) {
    response.request.options.onSuccess(response);
  },
  // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
  on0 : function(response) {
    response.request.options.onFailure(response);
  },
  onSuccess : function(response) {
    // If there was a 'template' field in the form, disable it to avoid 'This document already exists' errors.
    if (this.form && this.form.template) {
      this.form.template.disabled = true;
      this.form.template.value = "";
    }
    this.savingBox.replace(this.savedBox);
    // Announce that the document has been saved
    // TODO: We should send the new version as a memo field
    document.fire("xwiki:document:saved");
  },
  onFailure : function(response) {
    this.savingBox.replace(this.failedBox);
    if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
      $('ajaxRequestFailureReason').update('Server not responding');
    } else if (response.getHeader('Content-Type').match(/^\s*text\/plain/)) {
      // Regard the body of plain text responses as custom status messages.
      $('ajaxRequestFailureReason').update(response.responseText);
    } else {
      $('ajaxRequestFailureReason').update(response.statusText);
    }
    // Announce that a document save attempt has failed
    document.fire("xwiki:document:saveFailed", {'response' : response});
  }
});

function init() {
  new actionButtons.EditActions();
  // In preview mode, the &continue part of the save&continue should lead back to the edit action.
  if (!$('body').hasClassName("previewbody")) {
    new actionButtons.AjaxSaveAndContinue();
  }
  return true;
}

// When the document is loaded, install action buttons
(XWiki.domIsLoaded && init())
|| document.observe('xwiki:dom:loaded', init );

function updateForShortcut() {
  if (typeof(Wysiwyg) == 'undefined') {
    return;
  }
  var editors = Wysiwyg.getInstances();
  for(var hookId in editors) {
    var editor = editors[hookId];
    var plainTextArea = editor.getPlainTextArea();
    if(plainTextArea && !plainTextArea.disabled) {
      $(hookId).value = plainTextArea.value;
    } else {
      editor.getCommandManager().execute('submit');
    }
  }
}
document.observe("xwiki:actions:save", updateForShortcut);
document.observe("xwiki:actions:preview", updateForShortcut);
// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
