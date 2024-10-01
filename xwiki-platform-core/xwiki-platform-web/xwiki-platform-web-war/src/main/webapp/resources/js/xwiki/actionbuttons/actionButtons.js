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
  var currentDocument;
  var editingVersionDateField = $('editingVersionDate');
  var previousVersionField = $('previousVersion');
  var isNewField = $('isNew');

  var refreshVersion = function (event) {
    if (currentDocument.equals(event.memo.documentReference)) {
      if (previousVersionField) {
        previousVersionField.setValue(event.memo.version);
      }
      if (isNewField) {
        isNewField.setValue(false);
      }
    }
  };

  /**
   * Allow custom validation messages to be set on the validated field usin data attributes.
   *
   * @param field the validated field
   */
  const maybeUseCustomValidationMessage = field => {
    const failures = ['badInput', 'patternMismatch', 'rangeOverflow', 'rangeUnderflow', 'stepMismatch', 'tooLong',
      'tooShort', 'typeMismatch', 'valueMissing'];
    failures.find(failure => {
      // Convert 'badInput' into 'data-validation-bad-input'.
      const attributeName = 'data-validation-' + failure.replace(/([A-Z])/g, match => `-${match.toLowerCase()}`);
      if (field.validity[failure] && field.getAttribute(attributeName)) {
        field.setCustomValidity(field.getAttribute(attributeName));
        // We need to remove our custom validation message as soon as the user changes the field value, otherwise the
        // field remains marked (and styled) as invalid.
        const resetValidationMessage = () => {
          field.setCustomValidity('');
          field.removeEventListener('input', resetValidationMessage);
        };
        field.addEventListener('input', resetValidationMessage);
        // Stop here.
        return true;
      }
      // Continue with the next failures.
      return false;
    });
  };

  // Listen for versions change to update properly the version fields.
  document.observe('xwiki:document:changeVersion', refreshVersion);

  actionButtons.EditActions = Class.create({
    initialize: function() {
      this.addListeners();
      this.addShortcuts();
    },

    addListeners: function() {
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

    addShortcuts: function() {
      var shortcuts = {
        'action_cancel' : "#getShortcutValue('core.shortcuts.edit.cancel')",
        'action_preview' : "#getShortcutValue('core.shortcuts.edit.preview')",
        // The following 2 are both "Back to edit" in the preview mode, depending on the used editor
        'action_edit' : "#getShortcutValue('core.shortcuts.edit.backtoedit')",
        'action_inline' : "#getShortcutValue('core.shortcuts.edit.backtoedit')",
        'action_save' : "#getShortcutValue('core.shortcuts.edit.save')",
        'action_propupdate' : "#getShortcutValue('core.shortcuts.edit.saveandview')",
        'action_saveandcontinue' : "#getShortcutValue('core.shortcuts.edit.saveandcontinue')"
      }
      for (var key in shortcuts) {
        var targetButtons = $$("input[name=" + key + "]");
        if (targetButtons.length) {
          shortcut.add(shortcuts[key], function() {
            this.click();
          }.bind(targetButtons.first()));
        }
      }
    },

    validateForm: function(form) {
      // Validate the form using the standard HTML5 Constraint Validation API.
      if (form && !form.checkValidity()) {
        // If the invalid fields specify custom validation messages then use them, instead of those provided by the web
        // browser, in order to match the XWiki locale, which may be different than the web browser's locale.
        [...form.elements].filter(field => !field.validity.valid).forEach(maybeUseCustomValidationMessage);
        // Show the validation errors.
        form.reportValidity();
        return false;
      }

      var commentField = form?.comment || $('commentinput');
      const commentIsSuggested = commentField?.getAttribute('data-xwiki-edit-comment-suggested') === 'true';
      const commentIsMandatory = commentField?.getAttribute('data-xwiki-edit-comment-mandatory') === 'true';
      if (commentIsSuggested || commentIsMandatory) {
        while (commentField.value === '') {
          var response = prompt(commentField.getAttribute('data-xwiki-edit-comment-prompt'), '');
          if (response === null) {
            return false;
          }
          commentField.value = response;
          if (!commentIsMandatory) break;
        }
      }

      return true;
    },

    onCancel: function(event) {
      // Notify the others that we are going to cancel and let them prevent our default behavior.
      if (this.notify(event, 'cancel')) {
        event.preventDefault();

        // Optimisation: Do not submit the entire form's data when all we want is to cancel editing.
        var form = event.element().form;
        form && this.cancelForm(form);
      }
    },

    cancelForm: function(form) {
      // Determine the form's action and clean it by removing any anchors.
      var location = form.action || window.location.href;
      var parts = location.split('#', 2);
      var fragmentId = (parts.length == 2) ? parts[1] : '';
      location = parts[0];
      if (location.indexOf('?') == -1) {
        location += '?';
      }

      // Make sure that we call the CancelAction using XWiki's ActionFilter, no matter what XWiki action was set in the
      // form's action.
      var cancelActionParameter = '&action_cancel=true';

      // Include the xredirect element, if it exists.
      var xredirectElement = form.select('input[name="xredirect"]')[0];
      var xredirectParameter = xredirectElement ? '&xredirect=' + escape(xredirectElement.value) : '';

      // Include the language parameter if it exists
      var language = form.select('input[name="language"]')[0];
      var languageParameter = language ? '&language=' + escape(language.value) : '';

      // Optimisation: Prevent a redundant request to remove the edit lock when the page unload event is triggered. Both
      // the cancel action and the page unload event would unlock the document, so no point in doing both.
      XWiki.EditLock && XWiki.EditLock.setLocked(false);

      // Call the cancel URL directly instead of submitting the form. (optimisation)
      window.location = location + cancelActionParameter + xredirectParameter + languageParameter + fragmentId;
    },

    onSubmit: function(event, action, continueEditing) {
      var beforeAction = 'before' + action.substr(0, 1).toUpperCase() + action.substr(1);
      if (this.notify(event, beforeAction, {'continue': continueEditing})) {
        if (this.validateForm(event.element().form)) {
          this.notify(event, action, {'continue' : continueEditing});
        } else {
          event.preventDefault();
        }
      }
    },

    notify: function(originalEvent, action, params) {
      // We fire the action event on the button that triggered the action. This is useful when there are multiple forms
      // on the same page and you want to catch the events that were triggered by a specific form.
      var event = originalEvent.element().fire('xwiki:actions:' + action, Object.extend({
        originalEvent: originalEvent,
        form: originalEvent.element().form
      }, params || {}));
      // We check both the current event and the original event in order to preserve backwards compatibility with older
      // code that may prevent default behavior only for the original event. We recommend calling preventDefault() only
      // on the current event because most of the listeners shouldn't be aware of the original event.
      var defaultPrevented = event.defaultPrevented || originalEvent.defaultPrevented;
      // Stop the original event if the current event has been stopped. Also, in Internet Explorer the original event
      // can't be stopped from the current event's handlers, so in case some old code has tried to stop the original
      // event we must stop it again here.
      if (defaultPrevented) {
        originalEvent.preventDefault();
      }
      return !defaultPrevented;
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
      this.failedBox = new XWiki.widgets.Notification(
        '$escapetool.javascript($services.localization.render("core.editors.saveandcontinue.notification.error", ["<span id=""ajaxRequestFailureReason""></span>"]))',
        "error", {inactive: true});
      this.progressMessageTemplate = "$escapetool.javascript($services.localization.render('core.editors.savewithprogress.notification'))";
      this.progressBox = new XWiki.widgets.Notification(this.progressMessageTemplate.replace('__PROGRESS__', '0'), "inprogress", {inactive: true});
      this.savedWithMergeBox = new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('core.editors.saveandcontinue.notification.doneWithMerge'))", "done", {inactive: true});
    },
    addListeners : function() {
      document.observe("xwiki:actions:save", this.onSave.bindAsEventListener(this));
    },
    // Allow to disable the editors (form, WikiEditor or CKEditor) while a save&view is performed.
    disableEditors : function () {
      this._setFormDisabledState(true);
    },
    // Allow to enable back the editors (form, WikiEditor or CKEditor) in case of 401 for example.
    enableEditors : function () {
      this._setFormDisabledState(false);
    },
    _setFormDisabledState: function(disabled) {
      // If the form fields are wrapped in a field set then use that to disable / re-enable all the form fields at once.
      // This is faster and more robust because we don't need to remember the disabled state for each field (in order to
      // avoid enabling fields that are meant to be disabled in some cases).
      const fieldSet = this.form?.querySelector(':scope > fieldset');
      if (fieldSet) {
        fieldSet.disabled = disabled;
      } else if (disabled) {
        this.form?.disable();
      } else {
        this.form?.enable();
      }
    },
    getFormData: function(action) {
      const formData = new FormData(this.form);
      if (this.hasFormAction(action)) {
        formData.set(action, '');
      }
      return new URLSearchParams(formData);
    },
    hasFormAction: function(action) {
      return typeof action === 'string' && [...this.form.querySelectorAll('input[type=submit], button')]
        .some(button => {
          return button.getAttribute('name') === action;
        });
    },
    onSave : function(event) {
      // Don't continue if the event has been stopped already.
      if (event.defaultPrevented) {
        return;
      }

      this.savedBox.hide();
      this.failedBox.hide();
      var isContinue = event.memo["continue"];
      this.form = $(event.memo.form);

      // This could be a custom form, in which case we need to keep it simple to avoid breaking applications.
      var isCustomForm = this.form.action.indexOf("/preview/") == -1 && this.form.action.indexOf("/save/") == -1;
      if (isCustomForm && !isContinue) {
        return;
      }

      var isCreateFromTemplate = (this.form.template && this.form.template.value);

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

      // Prevent the default form submit behavior.
      event.preventDefault();

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
      var formData = this.getFormData(submitValue);
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
      var state = {
        isContinue: isContinue,
        isCreateFromTemplate: isCreateFromTemplate,
        saveButton: event.element()
      };
      new Ajax.Request(this.form.action, {
        method : 'post',
        parameters : formData.toString(),
        onSuccess : this.onSuccess.bind(this, state),
        on0 : this.on0.bind(this),
        on409 : this.on409.bind(this, state),
        on401 : this.on401.bind(this, state),
        on403 : this.on403.bind(this, state),
        onFailure : this.onFailure.bind(this, state)
      });
    },
    // 0 is returned for network failures.
    on0 : function(response) {
      response.request.options.onFailure(response);
    },
    onSuccess : function(state, response) {
      // If there was a 'template' field in the form, disable it to avoid 'This document already exists' errors.
      if (this.form && this.form.template) {
        this.form.template.disabled = true;
        this.form.template.value = "";
      }

      // Reset the comment after a successful save and continue action.
      if ($("commentinput")) {
        $("commentinput").value = '';
      }

      // don't force save twice
      if ($('forceSave')) {
        $('forceSave').remove();
      }

      $$('input[name=mergeChoices]').forEach(function (item) {item.remove();});
      $$('input[name=customChoices]').forEach(function (item) {item.remove();});

      var hasBeenSaved = false;
      if (state.isCreateFromTemplate) {
        // We might have a responseJSON containing other information than links, if the template cannot be accessed.
        if (response.responseJSON && response.responseJSON.links) {
          // Start the progress display.
          this.getStatus(response.responseJSON.links[0].href, state);
        } else {
          this.progressBox.hide();
          this.savingBox.replace(this.savedBox);
          // in such case the page is saved, so we'll need to maybe redirect
          hasBeenSaved = true;
        }
      } else {
        this.progressBox.hide();
        if (response.responseJSON && response.responseJSON.mergedDocument) {
          this.savingBox.replace(this.savedWithMergeBox);
        } else {
          this.savingBox.replace(this.savedBox);
        }
        hasBeenSaved = true;
      }

      if (hasBeenSaved && !state.isContinue || $('body').hasClassName('previewbody')) {
        state.saveButton.fire("xwiki:document:saved", response.responseJSON);
        if (this.maybeRedirect(state.isContinue)) {
          return;
        }
      }

      if (response.responseJSON && response.responseJSON.newVersion) {
        // update the version
        require(['xwiki-meta'], function (xm) {
          xm.setVersion(response.responseJSON.newVersion);
        });

        // We only update this field since the other ones are updated by the callback of setVersion.
        if (editingVersionDateField) {
          editingVersionDateField.setValue(new Date().getTime());
        }
      }

      // Announce that the document has been saved
      state.saveButton.fire("xwiki:document:saved", response.responseJSON);

      // If documents have been merged we need to reload to get latest saved version.
      if (response.responseJSON && response.responseJSON.mergedDocument) {
        this.reloadEditor();
      }
    },
    displayErrorModal : function (createContent) {
      XWiki.widgets.ErrorModalPopup = Class.create(XWiki.widgets.ModalPopup, {
        /** Default parameters can be added to the custom class. */
        defaultInteractionParameters : {},
        /** Constructor. Registers the key listener that pops up the dialog. */
        initialize : function($super, interactionParameters) {
          this.interactionParameters = Object.extend(Object.clone(this.defaultInteractionParameters),
            interactionParameters || {});
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
    // Reload the editors in case the user want to loose his change.
    reloadEditor : function () {
      // We don't rely on window.location.reload() since it might keep cached data from the form.
      // We don't rely on window.location.reload(true) either since it's unclear if it's properly supported by
      // all browsers. Instead we rely on a query parameter with the current date.
      var params = new URLSearchParams(window.location.search);
      params.set("timestamp", new Date().getTime());
      window.location.search = "?" + params.toString();
    },
    // 401 happens when the user is not authorized to do that: can be a logout or a change in perm
    on401 : function (state, response) {
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
      state.saveButton.fire("xwiki:document:saveFailed", {'response' : response});
    },
    // 403 happens in case of CSRF issue
    on403 : function (state, response) {
      if (!response.responseJSON || response.responseJSON.errorType !== "CSRF") {
        return this.on401(state, response);
      }

      this.progressBox.hide();
      this.savingBox.hide();
      this.savedBox.hide();
      this.failedBox.hide();

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
            onSuccess : self.onSuccess.bind(self, state),
            on0 : self.on0.bind(self),
            on409 : self.on409.bind(self, state),
            on401 : self.on401.bind(self, state),
            on403 : self.on403.bind(self, state),
            onFailure : self.onFailure.bind(self, state)
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

      // Announce that a document save attempt has failed
      state.saveButton.fire("xwiki:document:saveFailed", {'response' : response});
    },
    // 409 happens when the document is in conflict: i.e. someone's else edited it at the same time
    on409 : function(state, response) {
      var self = this;
      this.progressBox.hide();
      this.savingBox.hide();
      this.savedBox.hide();
      this.failedBox.hide();
      this.enableEditors();

      var jsonAnswer = response.responseJSON;
      var formData = this.getFormData('preview');

      var displayModal;

      /**
       * Display the previewdiff modal with the appropriate diff version.
       * If no argument is given, the version chosen in the select is used.
       */
      var previewDiff = function (previousVersion, nextVersion) {
        var queryString = "diff=1&version=" + jsonAnswer.latestVersion;
        if ($$('input[name=warningConflictAction]').length > 0) {
          queryString += "&warningConflictAction=" + $$('input[name=warningConflictAction]:checked')[0].value;
        }
        var original, revised;

        if (previousVersion && nextVersion) {
          original = previousVersion;
          revised = nextVersion;
        } else if ($$('select[name=original]').length > 0 && $$('select[name=revised]').length > 0) {
          original = $$('select[name=original]')[0].value;
          revised = $$('select[name=revised]')[0].value;
        }

        if (original && revised) {
          queryString +=
            "&original=" + original + "&revised=" + revised;
        }

        var previewUrl = new XWiki.Document().getURL("preview", queryString);
        new Ajax.Request(previewUrl, {
          method : 'post',
          parameters : formData.toString(),
          onSuccess : displayModal,
          onFailure : self.onFailure.bind(self, state)
        });
      };

      // Submit the choice made: if it's reload, reload the editors,
      // else submit the save with the right forceSave input.
      var submitAction = function () {
        // Ensure the previous values are really removed.
        // It might not be the case if another conflict arise for example.
        if ($('forceSave')) {
          $('forceSave').remove();
        }

        $$('input[name=mergeChoices]').forEach(function (item) {item.remove();});
        $$('input[name=customChoices]').forEach(function (item) {item.remove();});
        require(['jquery'], function ($) {
          var action = $('input[name=warningConflictAction]:checked').val();
          getConflictIds().map(retrieveDecisions);
          $('#previewDiffModal').modal('hide');
          if (action === "reload") {
            self.reloadEditor();
          } else {
            self.form.insert(new Element("input", {
              type: "hidden",
              name: "forceSave",
              id: "forceSave",
              value: action
            }));
            if (state.isContinue) {
              $('input[name=action_saveandcontinue]').click();
            } else {
              $('input[name=action_save]').click();
            }
          }
        });
      };

      var radioToogleClass = function () {
        require(['jquery'], function ($) {
          $('input[name=warningConflictAction]').parents(".panel").removeClass('panel-primary');
          $('input[name=warningConflictAction]').parents(".panel").addClass('panel-default');
          $('input[name=warningConflictAction]:checked').parents(".panel").addClass('panel-primary');
        });
      };

      // Allow to select a button radio by clicking anywhere on the panel.
      var selectChoice = function (event) {
        require(['jquery'], function ($) {
          $(event.currentTarget).find('input[name=warningConflictAction]').click();
        });
      };

      // Change the diff based on the action chosen.
      var radioSelect = function () {
        var selectedValue = $$('input[name=warningConflictAction]:checked')[0].value;
        if (selectedValue == "merge") {
          previewDiff("NEXT", "MERGED");
        // It's the next and current value which will be used for the merge, so better use them in case of custom
        // conflicts resolution.
        } else if (selectedValue == "override" || selectedValue == "custom") {
          previewDiff("NEXT", "CURRENT");
        } else if (selectedValue == "reload") {
          previewDiff("CURRENT", "NEXT");
        }
      };

      var getConflictIds = function () {
        return $$('input[name=conflict_id]').map(function (item) {return item.value;})
      };

      var retrieveDecisions = function (conflictId) {
        var selectValue = $('conflict_decision_select_' + conflictId).value;
        var customValue = $('conflict_decision_value_custom_' + conflictId).value;
        self.form.insert(new Element("input", {
          type: "hidden",
          name: "mergeChoices",
          id: "mergeChoices_" + conflictId,
          value: conflictId + "=" + selectValue
        }));
        if (selectValue === "custom") {
          self.form.insert(new Element("input", {
            type: "hidden",
            name: "customChoices",
            id: "custom_" + conflictId,
            value: conflictId + "=" + encodeURI(customValue)
          }));
        }
      };

      // Prepare the modal
      displayModal = function (response) {
        require(['jquery'], function ($) {
          if ($('#previewDiffModal')) {
            $('#previewDiffChangeDiff').off('click');
            $('input[name=warningConflictAction]').off('click');
            $('#submitDiffButton').off('click');
            $('#previewDiffModal').remove();
            $('div.modal-backdrop').remove();
          }
          $(response.responseText).appendTo('body');
          radioToogleClass();
          $('#previewDiffModal').modal('show');

          // We want to remove the html of the modal and the backdrop when the modal is closed.
          $('#previewDiffModal').on('hidden.bs.modal', function (e) {
            $('#previewDiffModal').remove();
            $('div.modal-backdrop').remove();
          });

          $('#previewDiffChoices .panel').on('click', selectChoice);
          $('input[name=warningConflictAction]').on('change', radioSelect);
          $('#previewDiffChangeDiff').on('click', previewDiff);
          $('#submitDiffButton').on('click', submitAction);
          $(document).trigger("xwiki:dom:updated", {'elements': $('#previewDiffModal').toArray()});
        });
      };

      previewDiff();
      // Announce that a document save attempt has failed
      state.saveButton.fire("xwiki:document:saveFailed", {'response' : response});
    },
    onFailure : function(state, response) {
      this.enableEditors();
      this.savingBox.replace(this.failedBox);
      this.progressBox.replace(this.failedBox);
      if (!response.statusText) {
        $('ajaxRequestFailureReason').update('Server not responding');
      } else if (response.getHeader('Content-Type').match(/^\s*text\/plain/)) {
        // Regard the body of plain text responses as custom status messages.
        $('ajaxRequestFailureReason').update(response.responseText);
      } else {
        $('ajaxRequestFailureReason').update(response.statusText);
      }
      // Announce that a document save attempt has failed
      state.saveButton.fire("xwiki:document:saveFailed", {'response' : response});
    },
    startStatusReport : function(statusUrl) {
      updateStatus(0);
    },
    getStatus : function(statusUrl, state) {
      new Ajax.Request(statusUrl, {
        method : 'get',
        parameters : { 'media' : 'json' },
        onSuccess : function(response) {
          var progressOffset = response.responseJSON.progress.offset;
          this.updateStatus(progressOffset);
          if (progressOffset < 1) {
            // Start polling for status updates, every second.
            setTimeout(this.getStatus.bind(this, statusUrl, state), 1000);
          } else {
            // Job complete.
            this.progressBox.replace(this.savedBox);
            this.maybeRedirect(state.isContinue);
          }
        }.bind(this),
        on0 : this.on0.bind(this),
        onFailure : this.onFailure.bind(this, state)
      });
    },
    updateStatus : function(progressOffset) {
      var stringProgress = "" + (progressOffset * 100);
      var dotIndex = stringProgress.indexOf('.');
      if (dotIndex > -1) {
        var stringProgress = stringProgress.substring(0, dotIndex + 2);
      }
      var newProgressBox = new XWiki.widgets.Notification(this.progressMessageTemplate.replace('__PROGRESS__',
        stringProgress), "inprogress");
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
        return false;
      }

      // Do the redirect.
      if (url) {
        window.location = url;
      }
      return true;
    }
  });

  function init() {
    require(['xwiki-meta'], function (xm) {
      currentDocument = xm.documentReference;
    });

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
