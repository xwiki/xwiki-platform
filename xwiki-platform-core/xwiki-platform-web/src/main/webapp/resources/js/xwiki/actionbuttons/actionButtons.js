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
// ======================================
// Form buttons: shortcuts, AJAX improvements and form validation
// To be completed.

var XWiki = (function(XWiki) {
// Start XWiki augmentation.
  var actionButtons = XWiki.actionButtons = XWiki.actionButtons || {};

  // we need to handle the creation of document
  var currentDocument, currentVersion, isNew;
  var editingVersionDate = new Date();

  require(['xwiki-meta'], function (xm) {
    currentDocument = xm.documentReference;
    currentVersion = xm.version;
    isNew = xm.isNew;
  });

  var getCurrentVersion = function (event) {
    if (currentDocument.equals(event.memo.documentReference)) {
      currentVersion = event.memo.version;
      editingVersionDate = new Date();
      isNew = "false";
    }
  };

  // in object editor the version is updated when an object is added.
  document.observe('xwiki:document:changeVersion', getCurrentVersion);

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
        item.observe('click', this.onSubmit.bindAsEventListener(this, 'preview'));
      }.bind(this));
      $$('input[name=action_save]').each(function(item) {
        item.observe('click', this.onSubmit.bindAsEventListener(this, 'save'));
      }.bind(this));
      $$('input[name=action_saveandcontinue]').each(function(item) {
        item.observe('click', this.onSubmit.bindAsEventListener(this, 'save', true));
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
          }.bind(targetButtons.first()));
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
      var xredirectElement = event.element().form.select('input[name="xredirect"]')[0];
      var xredirectParameter = xredirectElement ? '&xredirect=' + escape(xredirectElement.value) : '';

      // Optimisation: Prevent a redundant request to remove the edit lock when the page unload event is triggered. Both the cancel action
      // and the page unload event would unlock the document, so no point in doing both.
      XWiki.EditLock && XWiki.EditLock.setLocked(false);

      // Call the cancel URL directly instead of submitting the form. (optimisation)
      window.location = location + cancelActionParameter + xredirectParameter + fragmentId;
    },
    onSubmit: function(event, action, continueEditing) {
      var beforeAction = 'before' + action.substr(0, 1).toUpperCase() + action.substr(1);
      if (this.notify(event, beforeAction, {'continue': continueEditing})) {
        if (this.validateForm(event.element().form)) {
          this.notify(event, action, {'continue' : continueEditing});
        } else {
          event.stop();
        }
      }
    },
    notify : function(originalEvent, action, params) {
      var event = document.fire('xwiki:actions:' + action, Object.extend({
        originalEvent: originalEvent,
        form: originalEvent.element().form
      }, params || {}));
      // We check both the current event and the original event in order to preserve backwards compatibility with older
      // code that may stop only the original event. We recommend stopping only the current event becase most of the
      // listeners shouldn't be aware of the original event.
      var stopped = event.stopped || originalEvent.stopped;
      // Stop the original event if the current event has been stopped. Also, in Internet Explorer the original event
      // can't be stopped from the current event's handlers, so in case some old code has tried to stop the original event
      // we must call stop() again here.
      if (stopped) {
        originalEvent.stop();
      }
      return !stopped;
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
      this.progressMessageTemplate = "$escapetool.javascript($services.localization.render('core.editors.savewithprogress.notification'))";
      this.progressBox = new XWiki.widgets.Notification(this.progressMessageTemplate.replace('__PROGRESS__', '0'), "inprogress", {inactive: true});
    },
    addListeners : function() {
      document.observe("xwiki:actions:save", this.onSave.bindAsEventListener(this));
    },
    // Allow to disable the editors (form, WikiEditor or CKEditor) while a save&view is performed.
    disableEditors : function () {
      if (this.form) {
        this.form.disable();
      }

      if ($$('.CodeMirror')[0]) {
        try {
          $$('.CodeMirror')[0].CodeMirror.setOption('readOnly', true);
        } catch (e) {}
      } else if (window.CKEDITOR) {
        try {

          // FIXME: This still allows to switch CKEditor from source mode to WYSIWYG and back
          // and doing that would disable the readOnly and activate back the warning about leaving page
          CKEDITOR.instances.content.resetDirty();
          CKEDITOR.instances.content.setReadOnly();
        } catch (e) {}
      }
    },
    // Allow to enable back the editors (form, WikiEditor or CKEditor) in case of 401 for example.
    enableEditors : function () {
      if (this.form) {
        this.form.enable();
      }
      if ($$('.CodeMirror')[0]) {
        try {
          $$('.CodeMirror')[0].CodeMirror.setOption('readOnly', false);
        } catch (e) {}
      } else if (window.CKEDITOR) {
        try {
          CKEDITOR.instances.content.setReadOnly(false);
        } catch (e) {}
      }
    },
    onSave : function(event) {
      // Don't continue if the event has been stopped already
      if (event.stopped) {
        return;
      }

      var isContinue = event.memo["continue"];

      this.form = $(event.memo.form);
      this.savedBox.hide();
      this.failedBox.hide();

      var isCreateFromTemplate = (this.form.template && this.form.template.value);

      // This could be a custom form, in which case we need to keep it simple to avoid breaking applications.
      var isCustomForm = (this.form.action.indexOf("/preview/") == -1 && this.form.action.indexOf("/save/") == -1);
      if (isCustomForm && !isContinue) {
        return;
      }

      // We only send the version info if we are on a standard edit.
      var isFromStandardEditor = window.location.href.indexOf("/edit/") != -1;

      if (isFromStandardEditor) {
        if ($('previousVersion')) {
          $('previousVersion').setValue(currentVersion);
          $('editingVersionDate').setValue(editingVersionDate.toISOString());
          $('isNew').setValue(isNew);
        } else {
          this.form.insert(new Element("input", {
            type: "hidden",
            name: "previousVersion",
            id: "previousVersion",
            value: currentVersion
          }));
          this.form.insert(new Element("input", {
            type: "hidden",
            name: "editingVersionDate",
            id: "editingVersionDate",
            value: editingVersionDate.toISOString()
          }));
          this.form.insert(new Element("input", {
            type: "hidden",
            name: "isNew",
            id: "isNew",
            value: isNew.toString()
          }));
        }
      }

      // Handle explicitly requested synchronous operations (mainly for backwards compatibility).
      var isAsync = (this.form.async && this.form.async.value === 'true');

      // Avoid template asynchronous handling of templates for synchronous or custom forms.
      if (!isAsync) {
        // A synchronous create from template operation should behave as a regular save operation,
        // waiting for the Save(AndContinue)Action to finish its work.
        isCreateFromTemplate = false;
      }

      // Below we handle the following cases:
      // - S&C no template / S&C from template sync (handled the same way),
      // - S&C from template async in edit/preview mode,
      // - S&V from template async.
      // - S&V no template sync

      // Stop the submit event.
      event.stop();

      // Show the right notification message.
      if (isCreateFromTemplate) {
        this.progressBox.show();
      } else {
        this.savingBox.show();
      }

      // Compute the data and submit the form in an AJAX request instead.
      var submitValue = 'action_save';
      if (isContinue) {
        submitValue = 'action_saveandcontinue';
      }
      var formData = new Hash(this.form.serialize({hash: true, submit: submitValue}));
      if (isContinue) {
        formData.set('minorEdit', '1');
      }
      if (!Prototype.Browser.Opera) {
        // Opera can't handle properly 204 responses.
        formData.set('ajax', 'true');
      }
      if (!isContinue) {
        this.disableEditors();
      }
      new Ajax.Request(this.form.action, {
        method : 'post',
        parameters : formData.toQueryString(),
        onSuccess : this.onSuccess.bindAsEventListener(this, isContinue, isCreateFromTemplate),
        on1223 : this.on1223.bindAsEventListener(this),
        on0 : this.on0.bindAsEventListener(this),
        on409 : this.on409.bindAsEventListener(this, isContinue),
        on401 : this.on401.bindAsEventListener(this),
        on403 : this.on403.bindAsEventListener(this, isContinue, isCreateFromTemplate),
        onFailure : this.onFailure.bind(this)
      });
    },
    // IE converts 204 status code into 1223...
    on1223 : function(response) {
      response.request.options.onSuccess(response);
    },
    // 0 is returned for network failures, except on IE where a strange large number (12031) is returned.
    on0 : function(response) {
      response.request.options.onFailure(response);
    },
    onSuccess : function(response, isContinue, isCreateFromTemplate) {
      // If there was a 'template' field in the form, disable it to avoid 'This document already exists' errors.
      if (this.form && this.form.template) {
        this.form.template.disabled = true;
        this.form.template.value = "";
      }

      // don't force save twice
      if ($('forceSave')) {
        $('forceSave').remove();
      }

      if (isCreateFromTemplate) {
        if (response.responseJSON) {
          // Start the progress display.
          this.getStatus(response.responseJSON.links[0].href, isContinue);
        } else {
          this.progressBox.hide();
          this.savingBox.replace(this.savedBox);
        }
      } else if (!isCreateFromTemplate && (!isContinue  || $('body').hasClassName('previewbody'))) {
        this.maybeRedirect(isContinue);
      } else {
        this.progressBox.hide();
        this.savingBox.replace(this.savedBox);
      }

      if (response.responseJSON && response.responseJSON.newVersion) {
        // update the version
        require(['xwiki-meta'], function (xm) {
          xm.setVersion(response.responseJSON.newVersion);
        });
      }

      // Announce that the document has been saved
      // TODO: We should send the new version as a memo field
      document.fire("xwiki:document:saved");
    },
    displayErrorModal : function (createContent) {
      XWiki.widgets.ErrorModalPopup = Class.create(XWiki.widgets.ModalPopup, {
        /** Default parameters can be added to the custom class. */
        defaultInteractionParameters : {},
        /** Constructor. Registers the key listener that pops up the dialog. */
        initialize : function($super, interactionParameters) {
          this.interactionParameters = Object.extend(Object.clone(this.defaultInteractionParameters), interactionParameters || {});
          // call constructor from ModalPopup with params content, shortcuts, options
          $super(
            createContent(this.interactionParameters, this),
            {
              "show"  : { method : this.showDialog,  keys : [] },
              //"close" : { method : this.closeDialog, keys : ['Esc'] }
            },
            {
              displayCloseButton : false,
              verticalPosition : "center",
              backgroundColor : "#FFF",
              removeOnClose : true
            }
          );
          this.showDialog();
        }
      });
      return new XWiki.widgets.ErrorModalPopup();
    },
    // 401 happens when the user is not authorized to do that: can be a logout or a change in perm
    on401 : function (response) {
      this.progressBox.hide();
      this.savingBox.hide();
      this.savedBox.hide();
      this.failedBox.hide();

      var createContent = function (data, modal) {
        var content =  new Element('div', {'class': 'modal-popup'});
        var buttonsDiv =  new Element('div');

        content.insert("$services.localization.render('core.editors.save.authorizationError.message')");
        content.insert(new Element('br'));
        content.insert(new Element('br'));
        var loginUrl = XWiki.currentDocument.getURL("login");

        var link = new Element('a', {'title': 'login', 'href': loginUrl, 'target': '_new'});
        link.insert("$services.localization.render('core.editors.save.authorizationError.followLink')");
        content.insert(link);
        content.insert(buttonsDiv);

        var br = new Element('br');
        var buttonCreate = new Element('button', {'class': 'btn btn-primary'});
        buttonCreate.insert("OK");
        buttonsDiv.insert(br);
        buttonsDiv.insert(buttonCreate);
        buttonCreate.on("click", function () {
          modal.closeDialog();
        });
        return content;
      };

      this.displayErrorModal(createContent);
      this.enableEditors();
      // Announce that a document save attempt has failed
      document.fire("xwiki:document:saveFailed", {'response' : response});
    },
    // 403 happens in case of CSRF issue
    on403 : function (response, isContinue, isCreateFromTemplate) {
      this.progressBox.hide();
      this.savingBox.hide();
      this.savedBox.hide();
      this.failedBox.hide();

      if (response.responseJSON && response.responseJSON.errorType === "CSRF") {
        var answerJson = response.responseJSON;
        var self = this;
        var createContent = function (data, modal) {
          var resubmit = function () {
            $$('input[name=form_token]').each(function (item) {
              item.value = answerJson.newToken;
            });
            new Ajax.Request(answerJson.resubmissionURI, {
              method : 'post',
              parameters : "form_token=" + answerJson.newToken,
              onSuccess : self.onSuccess.bindAsEventListener(self, isContinue, isCreateFromTemplate),
              on1223 : self.on1223.bindAsEventListener(self),
              on0 : self.on0.bindAsEventListener(self),
              on409 : self.on409.bindAsEventListener(self, isContinue),
              on401 : self.on401.bindAsEventListener(self),
              on403 : self.on403.bindAsEventListener(self, isContinue, isCreateFromTemplate),
              onFailure : self.onFailure.bind(self)
            });
            modal.closeDialog();
          };

          var content =  new Element('div', {'class': 'modal-popup', 'id': 'csrf-warning-modal'});
          var buttonsDiv =  new Element('div');

          // the confirmation message contains some double quotes that should be escaped.
          content.insert("$escapetool.json($services.localization.render('csrf.confirmation'))");
          content.insert(new Element('br'));
          var buttonCreate = new Element('button', {'class': 'btn btn-default', 'id': 'force-save-csrf'});
          buttonCreate.insert("$services.localization.render('yes')");
          buttonsDiv.insert(buttonCreate);
          buttonCreate.on("click", resubmit);

          buttonCreate = new Element('button', {'class': 'btn btn-primary', 'id': 'cancel-save-csrf'});
          buttonCreate.insert("$services.localization.render('no')");
          buttonsDiv.insert(buttonCreate);
          buttonCreate.on("click", function () {
            modal.closeDialog();
          });
          content.insert(buttonsDiv);
          return content;
        };
        this.enableEditors();
        this.displayErrorModal(createContent);
      } else {
        this.on401(response);
      }

      // Announce that a document save attempt has failed
      document.fire("xwiki:document:saveFailed", {'response' : response});
    },
    // 409 happens when the document is in conflict: i.e. someone's else edited it at the same time
    on409 : function(response, isContinue) {
      var self = this;
      this.progressBox.hide();
      this.savingBox.hide();
      this.savedBox.hide();
      this.failedBox.hide();

      // Prepare all the callback we need for the preview diff modal.

      // Reload the editors in case the user want to loose his change.
      var reloadEditors = function () {
        self.disableEditors();
        window.location.reload();
      };

      // Force save the change: submit the form with a forceSave input.
      var forceSave = function () {
        require(['jquery'], function ($) {
          $('#previewDiffModal').modal('hide');
          self.form.insert(new Element("input", {
            type: "hidden",
            name: "forceSave",
            id: "forceSave",
            value: "1"
          }));
          if (isContinue) {
            $('input[name=action_saveandcontinue]').click();
          } else {
            $('input[name=action_save]').click();
          }
        });
      };

      // We chose what to do depending on the radio button selection.
      var submitDiffButton = function () {
        require(['jquery'], function ($) {
          if ($('#actionReloadRadio').is(':checked')) {
            reloadEditors();
          }

          if ($('#actionForceSaveRadio').is(':checked')) {
            forceSave();
          }
        });
      };

      // Toggle the class of the div depending on the selected radio button.
      var radioSelect = function () {
        require(['jquery'], function ($) {
          var reloadDiv = $('#headingReloadEditor').parent();
          var forceSaveDiv = $('#headingForceSave').parent();

          var reloadRadio = $('#actionReloadRadio');
          var forceSaveRadio = $('#actionForceSaveRadio');

          reloadDiv.toggleClass('panel-primary', reloadRadio.is(':checked'));
          reloadDiv.toggleClass('panel-default', forceSaveRadio.is(':checked'));

          forceSaveDiv.toggleClass('panel-primary', forceSaveRadio.is(':checked'));
          forceSaveDiv.toggleClass('panel-default', reloadRadio.is(':checked'));
        });
      };

      // Prepare the modal
      var displayModal = function (response) {
        require(['jquery'], function ($) {
          $(response.responseText).appendTo('body');
          $('#previewDiffModal').modal('show');

          // We want to remove the html of the modal and the backdrop when the modal is closed.
          $('#previewDiffModal').on('hidden.bs.modal', function (e) {
            $('#previewDiffModal').remove();
            $('div.modal-backdrop').remove();
          });

          // Callback on the buttons.
          $('input[name=warningConflictAction]').on('click', radioSelect);
          $('#submitDiffButton').on('click', submitDiffButton);
        });
      };

      this.enableEditors();
      var formData = new Hash(this.form.serialize({hash: true, submit: 'preview'}));

      var jsonAnswer = response.responseJSON;
      var queryString = "diff=1&version=" + jsonAnswer.latestVersion;
      var previewUrl = new XWiki.Document().getURL("preview", queryString);

      new Ajax.Request(previewUrl, {
        method : 'post',
        parameters : formData.toQueryString(),
        onSuccess : displayModal,
        onFailure : this.onFailure.bind(this)
      });

      // Announce that a document save attempt has failed
      document.fire("xwiki:document:saveFailed", {'response' : response});
    },
    onFailure : function(response) {
      this.savingBox.replace(this.failedBox);
      this.progressBox.replace(this.failedBox);
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
    },
    startStatusReport : function(statusUrl) {
      updateStatus(0);
    },
    getStatus : function(statusUrl, isContinue, redirectUrl) {
      new Ajax.Request(statusUrl, {
        method : 'get',
        parameters : { 'media' : 'json' },
        onSuccess : function(response) {
          var progressOffset = response.responseJSON.progress.offset;
          this.updateStatus(progressOffset);
          if (progressOffset < 1) {
            // Start polling for status updates, every second.
            setTimeout(this.getStatus.bind(this, statusUrl, isContinue), 1000);
          } else {
            // Job complete.
            this.progressBox.replace(this.savedBox);
            this.maybeRedirect(isContinue);
          }
        }.bindAsEventListener(this),
        on1223 : this.on1223.bindAsEventListener(this),
        on0 : this.on0.bindAsEventListener(this),
        onFailure : this.onFailure.bindAsEventListener(this)
      });
    },
    updateStatus : function(progressOffset) {
      var stringProgress = "" + (progressOffset * 100);
      var dotIndex = stringProgress.indexOf('.');
      if (dotIndex > -1) {
        var stringProgress = stringProgress.substring(0, dotIndex + 2);
      }
      var newProgressBox = new XWiki.widgets.Notification(this.progressMessageTemplate.replace('__PROGRESS__', stringProgress), "inprogress");
      this.progressBox.replace(newProgressBox);
      this.progressBox = newProgressBox;
    },
    maybeRedirect : function(isContinue) {
      var url = "";
      if (!isContinue) {
        // Redirect to view mode or to whatever URL was requested.
        url = XWiki.currentDocument.getURL();
        var xredirectElement = this.form.select('input[name="xredirect"]')[0];
        if (xredirectElement && xredirectElement.value) {
          url = xredirectElement.value;
        }
      } else if ($('body').hasClassName('previewbody')) {
        // In preview mode, the &continue part of the save&continue should lead back to the edit action.

        // Redirect to edit mode or to the previous edit mode, if not overridden.
        url = XWiki.currentDocument.getURL('edit');
        if (this.form.xcontinue && this.form.xcontinue.value) {
          url = this.form.xcontinue.value;
        }
      } else {
        // No redirect needed.
        return;
      }

      // Do the redirect.
      if (url) {
        window.location = url;
      }
    }
  });

  function init() {
    new actionButtons.EditActions();
    new actionButtons.AjaxSaveAndContinue();
    return true;
  }

// When the document is loaded, install action buttons.
  (XWiki.domIsLoaded && init()) || document.observe('xwiki:dom:loaded', init);

// End XWiki augmentation.
  return XWiki;
}(XWiki || {}));

// Make sure the action buttons are visible at the bottom of the window.
require(['jquery'], function ($) {
  var $container = $('.sticky-buttons');
  if ($container.length == 0) return;
  // Use the placeholder to save the initial container position and make a gap to place
  // the sticky save bar once it reaches its initial position.
  var $placeholder = $('<div></div>');
  // Hide the element while keeping its offset.
  $placeholder.height(0);
  $placeholder.insertBefore($container);

  $(window).on("scroll resize load click xwiki:dom:refresh", function() {
    var isFullScreen = $('.fullScreenWrapper').length > 0
    var isVisible = $container.is(':visible');
    // Show the element and make the gap where the save bar should fit.
    $placeholder.height($container.height());
    var position = $placeholder.offset().top + $placeholder.height();

    if (isVisible && !isFullScreen && $(window).height() + $(window).scrollTop() < position) {
      $container.addClass('sticky-buttons-fixed');
      // The width of the parent element is not inherited when the position is fixed
      $container.innerWidth($container.parent().width());
    } else {
      $container.removeClass('sticky-buttons-fixed');
      $container.innerWidth('');
      // Hide the element while keeping its offset.
      $placeholder.height(0);
    }
  });
});
