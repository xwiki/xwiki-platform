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
/*!
#set ($l10nKeys = [
  'core.viewers.attachments.upload.addFileInput',
  'core.viewers.attachments.upload.removeFileInput',
  'core.viewers.attachments.upload.removeFileInput.title',
  'core.viewers.attachments.delete.inProgress',
  'core.viewers.attachments.delete.done',
  'core.viewers.attachments.delete.failed',
  ['docextra.extranb', '__number__'],
  'docextra.attachments'
])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($params = $key.subList(1, $key.size()))
  #if ($params)
    #set ($discard = $l10n.put($key[0], $services.localization.render($key[0], $params)))
  #else
    #set ($discard = $l10n.put($key, $services.localization.render($key)))
  #end
#end
#[[*/
// Start JavaScript-only code.
(function(l10n) {
  "use strict";

window.XWiki = window.XWiki || {};
var viewers = XWiki.viewers = XWiki.viewers || {};
/**
 * Enhancements for the Attachment upload area: adding and removing file fields, resetting with the Cancel button,
 * preventing submit if no files are selected.
 */
viewers.Attachments = Class.create({
  /** Counter for creating distinct upload field names. */
  counter : 1,
  /** Constructor. Adds all the JS improvements of the Attachment area. */
  initialize : function() {
    if ($("attachform")) {
      // If the upload form is already visible, enhance it.
      this.prepareForm();
    } else {
      // Otherwise, we wait for a notification for the AJAX loading of the Attachments metadata tab.
      this.addTabLoadListener();
    }
  },
  /** Enhance the upload form with JS behaviors. */
  prepareForm : function() {
    if (!$("attachform")) {
      return;
    }
    this.form = $("attachform").up("form");
    this.defaultFileDiv = this.form.down("input[type='file']").up("div");
    this.inputSize = this.form.down("input[type='file']").size;

    var html5Uploader = this.attachHTML5Uploader(this.form.down("input[type='file']"));
    if (html5Uploader) {
      html5Uploader.hideFormButtons();
    } else {
      this.addInitialRemoveButton();
      this.addAddButton();
      this.resetOnCancel();
    }
    this.blockEmptySubmit();
  },
  /** If available in the current browser, enable HTML5 upload for a given file input */
  attachHTML5Uploader : function(input) {
    if (typeof(XWiki.FileUploader) != 'undefined') {
      input.multiple = true;
      return new XWiki.FileUploader(input, {
        'responseContainer' : $('_attachments'),
        'responseURL' : XWiki.currentDocument.getURL('get', 'xpage=attachmentslist&forceTestRights=1'),
        'maxFilesize' : parseInt(input.readAttribute('data-max-file-size'))
      });
    }
    return false;
  },
  /** By default the form contains one upload field. Add a "remove" button for this one, too. */
  addInitialRemoveButton : function() {
    this.defaultFileDiv.appendChild(this.createRemoveButton());
  },
  /** Add an "Add another file" button below the file fields. */
  addAddButton : function() {
    var addButton = new Element("input", {
      type: "button",
      value: l10n['core.viewers.attachments.upload.addFileInput'],
      className: "attachmentActionButton add-file-input"
    });
    this.addDiv = new Element("div");
    this.addDiv.appendChild(addButton);
    Event.observe(addButton, 'click', this.addField.bindAsEventListener(this));
    this.defaultFileDiv.up().insertBefore(this.addDiv, this.defaultFileDiv.next());
  },
  /** Add a submit listener that prevents submitting the form if no file was specified. */
  blockEmptySubmit : function() {
    Event.observe(this.form, 'submit', this.onSubmit.bindAsEventListener(this));
  },
  /** Add a reset listener that resets the number of file fields to 1. */
  resetOnCancel : function() {
    Event.observe(this.form, 'reset', this.onReset.bindAsEventListener(this));
    Event.observe(this.form.down('.cancel'), 'click', this.onReset.bindAsEventListener(this));
  },
  /** Creates and inserts a new file input field. */
  addField : function(event) {
    var fileInput = new Element("input", {
      type: "file",
      name: "filepath_" + this.counter,
      size: this.inputSize,
      className: "uploadFileInput"
    });
    // For the moment, specifying a different name is not used anymore.
    var filenameInput = new Element("input", {type: "hidden", name : "filename_" + this.counter});
    var removeButton = this.createRemoveButton();
    var containerDiv = new Element("div", {'class' : 'fileupload-field'});
    containerDiv.insert(filenameInput).insert(fileInput).insert(removeButton);
    this.addDiv.parentNode.insertBefore(containerDiv, this.addDiv);
    // Remove the focus border from the button
    event.element().blur();
    this.counter++;
  },
  /** Remove a file field when pressing the corresponding "Remove" button. */
  removeField : function(event) {
    event.element().up("div").remove();
  },
  /** Create a remove button that triggers {@link #removeField} when clicked. */
  createRemoveButton : function() {
    var removeButton = new Element("input", {
      type: "button",
      value: l10n['core.viewers.attachments.upload.removeFileInput'],
      title: l10n['core.viewers.attachments.upload.removeFileInput.title'],
      className: "attachmentActionButton remove-file-input"
    });
    Event.observe(removeButton, "click", this.removeField.bindAsEventListener(this));
    return removeButton;
  },
  /** Form submit listener. It checks that at least one file item contains a filename. If not, cancel the submission. */
  onSubmit : function(event) {
    var hasFiles = false;
    this.form.getInputs("file").each(function(item) {
      if(item.value != '') {
        hasFiles = true;
      }
    });
    if(!hasFiles) {
      event.stop();
    }
  },
  /** Form reset listener. It resets the number of file fields to just one. */
  onReset : function(event) {
    if (event) {
      event.stop();
    }
    this.form.getInputs("file").each(function(item) {
      item.up().remove();
    });
    this.counter = 1;
    this.addField(event);
  },
  /**
   * Registers a listener that watches for the insertion of the Attachments tab and triggers the form enhancement.
   */
  addTabLoadListener : function(event) {
    var listener = function(event) {
      if (event.memo.id == 'Attachments') {
        this.prepareForm();
      }
    }.bindAsEventListener(this);
    document.observe("xwiki:docextra:loaded", listener);
  }
});

// When the document is loaded, trigger the attachment form enhancements.
(XWiki.domIsLoaded && new viewers.Attachments())
|| document.observe("xwiki:dom:loaded", function() { new viewers.Attachments(); });

/**
 * Delete attachments from AttachmentsTab.
 */
require(['jquery', 'xwiki-events-bridge'], function($) {
  /**
   * Getting the button that triggers the modal.
   */
  $(document).on('show.bs.modal', '#deleteAttachment', function(event) {
    $(this).data('relatedTarget', $(event.relatedTarget));
  });
  /**
   * Event on deleteAttachment button.
   */
  $(document).on('click', '#deleteAttachment input.btn-danger', function() {
    var modal = $('#deleteAttachment');
    var button = modal.data('relatedTarget');
    var notification;
    /**
     * Ajax request made for deleting an attachment. Delete the HTML element on succes. Disable the delete button
     * before the request is send, so the user cannot resend it in case it takes longer.
     * Display error message on failure.
     */
    $.ajax({
      url : button.prop('href'),
      beforeSend : function() {
        button.prop('disabled', true);
        notification = new XWiki.widgets.Notification(l10n['core.viewers.attachments.delete.inProgress'], 'inprogress');
      },
      success : function() {
        var attachment = button.closest('.attachment');
        // Remove the corresponding HTML element from the UI and update the attachment count.
        attachment.remove();
        updateCount();
        notification.replace(new XWiki.widgets.Notification(l10n['core.viewers.attachments.delete.done'], 'done'));
      },
      error: function() {
        // The button is enabled in case of error.
        button.prop('disabled', false);
        notification.replace(new XWiki.widgets.Notification(l10n['core.viewers.attachments.delete.failed'], 'error'));
      }
    })
  });
  /**
   * Updating the number of files in AttachmentsTab and in More actions menu.
   */
  var updateCount = function() {
    var itemCount = $('#Attachmentstab').find('.itemCount');
    var attachmentsNumber = $("#Attachmentspane .attachment").size();
    if(itemCount) {
      itemCount.text(l10n['docextra.extranb'].replace("__number__", attachmentsNumber));
    };
    if($('#tmAttachments').length) {
      // Calling normalize() because a text node needs to be modified and so all consecutive text nodes are merged.
      $('#tmAttachments')[0].normalize();
      var attachmentsLabel = ' ' + l10n['docextra.attachments'] + ' ';
      var label = attachmentsLabel + l10n['docextra.extranb'];
      label = label.replace("__number__", attachmentsNumber);
      $('#tmAttachments').contents().last()[0].nodeValue=label;
    }
  };
  /**
   * Firing updateCount event when an attachment is successfully uploaded.
   */
  $(document).on('xwiki:html5upload:done', function() {
    updateCount();
  });
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$l10n]));
