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
var viewers = XWiki.viewers = XWiki.viewers || {};
/**
 * Javascript enhancements for the comments viewer.
 */
viewers.Comments = Class.create({
  commentNumberRegex : /.+_(\d+)/,
  xcommentSelector: ".xwikicomment",
  commentPlaceHolderSelector: 'commentFormPlaceholder',

  /**
   * Extract the object number of the comment.
   * @param comment the comment
   * @returns the comment object number
   */
  extractCommentNumber : function (comment) {
    return comment.id.match(this.commentNumberRegex)[1];
  },

  /**
   * Check if the comment block has an object number.
   * @param comment the comment block
   * @returns {boolean} true if the comment block has an object number
   */
  hasCommentNumber : function (comment) {
    return comment.id.match(this.commentNumberRegex).length > 1;
  },

  /** Constructor. Adds all the JS improvements of the Comments area. */
  initialize : function() {
    // Make sure this class is initialized only once. This can happen when this file is included several times in 
    // a page.
    if (XWiki.viewers.initialized === undefined) {
      XWiki.viewers.initialized = true;
      var commentsContent = $('commentscontent');
      if (commentsContent) {
        // If the comments area is already visible, enhance them.
        this.startup();
      }
      if ($("Commentstab")) {
        this.container = $("Commentspane");
      } else if (commentsContent) {
        // We need to wrap the comments because we replace all of them when a new comment is added.
        this.container = commentsContent.wrap('div', {'id': 'Commentspane'});
      }
      // We wait for a notification for the AJAX loading of the Comments metadata tab.
      this.addTabLoadListener();
    }
  },
  /** Enhance the Comments UI with JS behaviors. */
  startup : function () {
    this.formDisplayed = false;
    this.loadIDs();
    this.getForm();
    this.addReplyListener();
    this.addEditListener();
    // replaces the comment button with the form on the first click.

    var openCommentForm = $('openCommentForm');
    if(openCommentForm) {
      openCommentForm.observe('click', function (event) {
        event.stop();
        $(this.commentPlaceHolderSelector).show();
        this.displayHiddenForm()
        this.reloadEditor({
          callback: function () {
            this.getForm();
            this.addSubmitListener(this.form);
            this.addCancelListener();
            this.addPreview(this.form);
          }.bind(this)
        });
      }.bind(this));
    }
  },
  // update the form element reference
  getForm: function () {
    var commentform = $('commentform');
    if (commentform) {
      this.form = commentform.up('form');
    } else {
      this.form = undefined;
    }
  },
  // load the form only once, when it is first needed
  // the next calls to this method does nothing but calling the callback
  // function parameter.
  displayHiddenForm: function () {
    if (!this.formDisplayed) {
      var placeHolder = $(this.commentPlaceHolderSelector);
      if (placeHolder) {
        placeHolder.removeClassName('hidden');
      }
      const button = $('openCommentForm');
      if (button) {
        button.hide();
      }
      const secondaryButton = $('loginAndComment');
      if (secondaryButton) {
        secondaryButton.hide();
      }
      this.formDisplayed = true;
    }
  },
  /**
   * Parse the IDs of the comments to obtain the xobject number.
   */
  loadIDs : function() {
    $$(this.xcommentSelector).each(function(item) {
      var elementId = item.id;
      item._x_number = elementId.substring(elementId.lastIndexOf("_") + 1) - 0;
    });
  },
  /**
   * Ajax comment editing.
   * For all edit buttons, listen to "click", and make ajax request to retrieve the form and save the comment.
   */
  addEditListener : function() {
    $$(this.xcommentSelector).each(function(item) {
      // Prototype bug in Opera: $$(".comment a.delete") returns only the first result.
      // Quick fix until Prototype 1.6.1 is integrated.
      item = item.down('a.edit');
      if (!item) {
        return;
      }
      item.observe('click', function(event) {
        item.blur();
        event.stop();
        this.displayHiddenForm();
        this.resetForm();
        if (item.disabled) {
          // Do nothing if the button was already clicked and it's waiting for a response from the server.
          return;
        } else if (item._x_editForm) {
          // If the form was already fetched, but hidden after cancel, just show it again
          // without making a new request
          var comment = item.up(this.xcommentSelector);
          comment.hide();
          const commentNbr = this.extractCommentNumber(comment);
          this.reloadEditor({
            commentNbr: commentNbr,
            callback: function () {
              this.getForm();
            }.bind(this)
          });
          item._x_editForm.show();
        } else {
          new Ajax.Request(
            /* Ajax request URL */
            item.readAttribute('href').replace('viewer=comments', 'xpage=xpart&vm=commentsinline.vm'),
            /* Ajax request parameters */
            {
              onCreate : function() {
                // Disable the button, to avoid a cascade of clicks from impatient users
                item.disabled = true;
                item._x_notification = new XWiki.widgets.Notification(
                    "$services.localization.render('core.viewers.comments.editForm.fetch.inProgress')",
                    "inprogress");
              },
              onSuccess : function(response) {
                // Hide other comment editing forms (allow only one comment to be edited at a time)
                if (this.editing) {
                  this.cancelEdit(false, this.editing);
                }
                // Replace the comment text with a form for editing it
                var comment = item.up(this.xcommentSelector);
                comment.insert({before: response.responseText});
                item._x_editForm = comment.previous();
                this.addSubmitListener(item._x_editForm);

                // extract the comment number in the number parameter.
                const commentNbr = item.readAttribute('href').match(/number=(\d+)/)[1];

                $(this.commentPlaceHolderSelector).hide();

                this.reloadEditor({
                  commentNbr: commentNbr,
                  callback: function () {
                    this.addPreview(item._x_editForm);
                    item._x_editForm.down('a.cancel').observe('click',
                        this.cancelEdit.bindAsEventListener(this, item));
                    comment.hide();
                    item._x_notification.hide();
                    // Currently editing: this comment
                    this.editing = item;
                  }.bind(this)
                });
              }.bind(this),
              onFailure : function (response) {
                var failureReason = response.statusText || 'Server not responding';
                item._x_notification.replace(new XWiki.widgets.Notification(
                    "$services.localization.render('core.viewers.comments.editForm.fetch.failed')" + failureReason,
                    "error"));
              }.bind(this),
              on0 : function (response) {
                response.request.options.onFailure(response);
              },
              onComplete: function() {
                // In the end: re-enable the button
                item.disabled = false;
              }
            }
          );
        }
      }.bindAsEventListener(this));
    }.bind(this));
  },
  /**
   * Cancel edit
   */
  cancelEdit : function (event, editActivator) {
    if (event) {
      event.stop();
    }
    var comment = editActivator.up(this.xcommentSelector);
    editActivator._x_editForm.hide();
    const commentNbr = this.extractCommentNumber(comment);
    const name = "XWiki.XWikiComments_" + commentNbr + "_comment";
    this.destroyEditor("[name='" + name + "']");
    comment.show();
    this.resetForm();
    this.cancelPreview(editActivator._x_editForm);
    this.editing = false;
  },
  /**
   * Inline reply: Move the form under the replied comment and update the hidden "replyto" field.
   */
  addReplyListener : function() {
    if (this.form) {
      $$(this.xcommentSelector).each(function(item) {
        this.addReplyListenerToComment(item);
      }.bind(this));
    } else {
      // If, for some reason, the form is missing, hide the reply functionality from the user
      $$(this.xcommentSelector + ' a.commentreply').each(function(item) {
        item.hide();
      });
    }
  },
  addReplyListenerToComment : function(item) {
    // Prototype bug in Opera: $$(".comment a.commentreply") returns only the first result.
    // Quick fix until Prototype 1.6.1 is integrated.
    item = item.down('a.commentreply');
    if (!item) {
      return;
    }
    item.observe('click', function(event) {
      item.blur();
      event.stop();
      this.getForm();
      // If the form was already displayed as a reply, re-enable the Reply button for the old location
      if (this.form.up('.commentthread')) {
        this.form.up(".commentthread").previous(this.xcommentSelector).down('a.commentreply').show();
      }
      // Insert the form on top of that comment's discussion
      item.up(this.xcommentSelector).next('.commentthread').insert({'top': this.form});

      this.reloadEditor({
        callback: function () {
          this.getForm();
          this.addSubmitListener(this.form);
          this.addCancelListener();
          this.addPreview(this.form);
          // Set the replyto field to the replied comment's number
          this.form["XWiki.XWikiComments_replyto"].value = item.up(this.xcommentSelector)._x_number;
          // Focus on the textarea.
          this.form["XWiki.XWikiComments_comment"].focus();
          // Hide the reply button
          item.hide();
        }.bind(this)
      });
    }.bindAsEventListener(this));
  },
  /**
   * When pressing Submit, check that the comment is not empty. Submit the form with ajax and update the whole
   * comments zone on success.
   */
  addSubmitListener : function(form) {
    // we check that the form exist and that no event has already been attached
    if (form && !form._has_event) {
      form._has_event = true;
      // Add listener for submit
      form.down("input[type='submit']").observe('click', function (event) {
        event.stop();
        // triggers the copy of the content of the rich editor to hidden fields
        document.fire('xwiki:actions:beforeSave');
        if (form.down('textarea').value != "") {
          var formData = new Hash(form.serialize(true));
          formData.set('xredirect',
              window.docgeturl + '?xpage=xpart&vm=commentsinline.vm&skin=' + encodeURIComponent(XWiki.skin));
          // Allows CommentAddAction to parse a template which will return a message telling if the captcha was
          // wrong.
          formData.set('xpage', 'xpart');
          formData.set('vm', 'commentsinline.vm');
          formData.set('skin', XWiki.skin);
          // Strip form parameters from the form action query string to prevent them from being overwriten.
          var queryStringParams = $H(form.action.toQueryParams());
          formData.keys().each(queryStringParams.unset.bind(queryStringParams));
          var url = form.action.replace(/\?.*/, '?' + queryStringParams.toQueryString());
          formData.unset('action_cancel');
          // Create a notification message to display to the user when the submit is being sent
          form._x_notification =
              new XWiki.widgets.Notification(
                  "$services.localization.render('core.viewers.comments.add.inProgress')",
                  "inprogress");
          form.disable();
          this.requestSucceeded = false;
          new Ajax.Request(url, {
            method : 'post',
            parameters : formData,
            onSuccess : function () {
              this.requestSucceeded = true;
              this.editing = false;
            }.bind(this),
            onFailure : function (response) {
              var failureReason = response.statusText || 'Server not responding';
              form._x_notification.replace(new XWiki.widgets.Notification(
                  "$services.localization.render('core.viewers.comments.add.failed')" + failureReason, "error"));
            }.bind(this),
            on0 : function (response) {
              response.request.options.onFailure(response);
            },
            onComplete : function (response) {
              // We enable the form even when it is going to be replaced because the browser caches the state of the
              // form input fields and the user will get disabled fields when (soft) reloading the page.
              form.enable();

              var comment = form.down('.xwikicomment');
              var name = "XWiki.XWikiComments_comment";
              if (comment !== undefined) {
                if (this.hasCommentNumber(comment)) {
                  const commentNbr = this.extractCommentNumber(comment);
                  name = "XWiki.XWikiComments_" + commentNbr + "_comment";
                }
              }
              this.destroyEditor("[name='" + name + "']");

              if (this.requestSucceeded) {
                this.container.update(response.responseText);

                // If a content is found in submittedcomment that means the submission was not valid.
                // For instance, the captcha was not accepted.
                var submittedCommentContainer =  $("submittedcomment");
                var submittedComment = '';
                if(submittedCommentContainer) {
                  submittedComment = submittedCommentContainer.value;
                  // Removed since it is not useful anymore.
                  submittedCommentContainer.remove();
                  // Forces displayHiddenForm to display the comment form with the content of the comment
                  // before submission.
                  this.formDisplayed = false;
                  this.displayHiddenForm();
                  this.reloadEditor({
                    content: submittedComment,
                    callback: function () {
                      this.getForm();
                      this.addSubmitListener(this.form);
                      this.addCancelListener();
                      this.addPreview(this.form);
                    }.bind(this)
                  });
                } else {
                  this.resetForm(); 
                }

                document.fire("xwiki:docextra:loaded", {
                  "id": "Comments",
                  "element": this.container
                });

                // Notify any displayed CAPTCHA that it was reloaded and it might need to reinitialize its JS.
                this.container.fire('xwiki:captcha:reloaded');

                // We send success notification only when everything is done: our integration tests relies on it
                // for waiting a comment added.
                form._x_notification.replace(
                  new XWiki.widgets.Notification("$services.localization.render('core.viewers.comments.add.done')",
                    "done"));
              }
            }.bind(this)
          });
        }
      }.bindAsEventListener(this));
    }
  },
  /**
   * When pressing Cancel, reset the form.
   */
  addCancelListener : function() {
    if (this.form) {
      this.initialLocation = new Element("span", {className : "hidden"});
      $('_comments').insert(this.initialLocation);
      // If the form is inside a thread, as a reply form, move it back to the bottom.
      this.form.down('a.cancel').observe('click', this.resetForm.bindAsEventListener(this));
    }
  },
  /**
   * Add a preview button that generates the rendered comment,
   */
  addPreview : function(form) {
    // We only allow to preview comment if the user is using text editor.
    if (!form || !XWiki.hasEdit || form.down("input[name='defaultEditorId']").value !== "text") {
      return;
    }

    var previewURL = "$xwiki.getURL('__space__.__page__', 'preview')".replace("__space__",
        encodeURIComponent(XWiki.currentSpace)).replace("__page__", encodeURIComponent(XWiki.currentPage));
    form.commentElt = form.down('textarea');
    var buttons = form.down('input[type=submit]').up('div');
    form.previewButton = new Element('span', {'class': 'buttonwrapper'}).update(new Element('input', {
      'type': 'button',
      'class': 'button',
      'value': "$services.localization.render('core.viewers.comments.preview.button.preview')"
    }));
    form.previewButton._x_modePreview = false;

    const buttonName = form.previewButton.down("input").value;
    const existingButton = buttons.down("input[value='" + buttonName + "']");
    if (existingButton) {
      existingButton.remove();
    }
    buttons.insert({'top': form.previewButton});
    form.previewButton.observe('click', function () {
      if (!form.previewButton._x_modePreview && !form.previewButton.disabled) {
        form.previewButton.disabled = true;
        var notification = new XWiki.widgets.Notification(
            "$services.localization.render('core.viewers.comments.preview.inProgress')", "inprogress");
        new Ajax.Request(previewURL, {
          method: 'post',
          parameters: {
            'xpage': 'plain',
            'sheet': '',
            'content': form.down('textarea').value,
            'form_token': form.form_token.value,
            'restricted': 'true'
          },
          onSuccess: function (response) {
            var comment = form.commentElt.up(this.xcommentSelector);
            var commentNumber;
            if (comment !== undefined && this.hasCommentNumber(comment)) {
              commentNumber = this.extractCommentNumber(comment);
            }
            this.doPreview(response.responseText, form, commentNumber);
            notification.hide();
          }.bind(this),
          /* If the content is empty or does not generate anything, we have the "This template does not exist" response,
             with a 400 status code. */
          on400: function (response) {
            this.doPreview('&nbsp;', form);
            notification.hide();
          }.bind(this),
          onFailure: function (response) {
            var failureReason = response.statusText || 'Server not responding';
            notification.replace(new XWiki.widgets.Notification(
                "$services.localization.render('core.viewers.comments.preview.failed')" + failureReason, "error"));
          },
          on0: function (response) {
            response.request.options.onFailure(response);
          },
          onComplete: function (response) {
            form.previewButton.disabled = false;
          }.bind(this)
        });
      } else {
        this.cancelPreview(form);
      }
    }.bindAsEventListener(this));
  },
  /**
   * Display the comment preview instead of the comment textarea.
   *
   * @param content the rendered comment, as HTML text
   * @param form the form for which the preview is done
   */
  doPreview : function(content, form, commentNumber) {
    require(['jquery'], function ($) {
      form.previewButton._x_modePreview = true;
      var commentEditorSelector = ".commenteditor";
      if (commentNumber) {
        commentEditorSelector += "-" + commentNumber;
      }
      const commentElt = $(form).find(commentEditorSelector);
      if (!$(form).find(".commentPreview").length) {
        commentElt.before('<div class="commentcontent commentPreview" style="display: none;"></div>');
      }
      const commentPreview = $(form).find(".commentcontent.commentPreview");
      commentPreview.html(content);
      commentPreview.show();
      commentElt.hide();
      $(form.previewButton).find('input')
        .val("$services.localization.render('core.viewers.comments.preview.button.back')");
    }.bind(this))
  },
  /**
   * Display the comment textarea instead of the comment preview.
   *
   * @param form the form for which the preview is canceled
   */
  cancelPreview : function(form) {
    require(['jquery'], function ($) {
      if (form.previewButton) {
        form.previewButton._x_modePreview = false;
        $(form.previewButton).find('input')
          .val("$services.localization.render('core.viewers.comments.preview.button.preview')");
      }
      const pc = $(form).find(".commentPreview");
      if (pc) {
        pc.hide();
        pc.empty();
      }
      const commentElt = $(form).find(".commenteditor");
      if (commentElt) {
        commentElt.show();
      }
    }.bind(this));
  },
  resetForm: function (event) {
    if (event) {
      event.stop();
    }
    require(['jquery'], function ($) {
      const commentPlaceHolder = $('#' + this.commentPlaceHolderSelector);
      if (this.form.up('.commentthread')) {
        // Show the comment's reply button back
        this.form.up(".commentthread").previous(this.xcommentSelector).down('a.commentreply').show();
        // Move back to form to its default location.
        commentPlaceHolder.empty().append(this.form)
      }
      // Hides the form and display the "add comment" button
      commentPlaceHolder.addClass("hidden");
      this.formDisplayed = false;
      $('#openCommentForm').show();
      $('#loginAndComment').show();
      this.form["XWiki.XWikiComments_replyto"].value = "";
      this.cancelPreview(this.form);
      // Cancel the edit mode so that leaving the page does not require confirmation.
      $(document).trigger('xwiki:actions:cancel');
    }.bind(this));
  },
  /**
   * Registers a listener that watches for the insertion of the Comments tab and triggers the enhancements.
   * After that, the listener removes itself, since it is no longer needed.
   */
  addTabLoadListener: function () {
    var listener = function (event) {
      if (event.memo.id == 'Comments') {
        this.startup();
      }
    }.bindAsEventListener(this);
    document.observe("xwiki:docextra:loaded", listener);
  },
  destroyEditor: function (selector) {
    require(['jquery', 'xwiki-events-bridge'], function ($) {
      $(document).trigger('xwiki:actions:cancel');
      if ($(selector).length) {
        const parent = $(selector).parent(".commenteditor");
        parent.empty();
      }
    });
  },
  reloadEditor: function (options) {
    var wfClass = '.commenteditor';
    options = options || {};
    const commentNbr = options.commentNbr;
    const callback = options.callback;
    var name = 'XWiki.XWikiComments_comment';
    if (commentNbr) {
      name = 'XWiki.XWikiComments_' + commentNbr + '_comment';
      wfClass = wfClass + '-' + commentNbr;
    }

    var content = options.content || {};

    this.destroyEditor("[name='" + name + "']");

    require(['jquery', 'xwiki-events-bridge'], function ($) {
      if ($(".commenteditor").length) {
        CKEDITOR.config.startupFocus = true;
        $.post(new XWiki.Document().getURL("get") + '?' + $.param({
          xpage: 'xpart',
          vm: 'commentfield.vm',
          number: commentNbr,
          name: name,
          content: content
        }), function (data) {
          const wf = $(wfClass);
          wf.empty();
          wf.append(data);
          wf.show();
          $(document).trigger('xwiki:dom:updated', {'elements': wf.toArray()});
          if ($("#commentCaptcha").length) {
            // FIXME: this solution is not good right now since it implies to always display the CAPTCHA
            // however since the idea is now to have a button "Add a comment", I won't got further: we should
            // display the captcha when the user decide to add a comment.
            $("#commentCaptcha").first().css('display', 'block');
          }
          if (callback) {
            callback();
          }
        });
      }
    });
  }
});

function init() {
  return new viewers.Comments();
}

// When the document is loaded, trigger the Comments form enhancements.
(XWiki.domIsLoaded && init())
|| document.observe("xwiki:dom:loaded", init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));

/**
 * Permalink.
 */
require(['jquery'], function($) {

  /**
   * Permalink: Events triggered while the permalink modal is displayed.
   */
  $(document).on('show.bs.modal', '#permalinkModal', function(event) {
    var button = $(event.relatedTarget);
    var permalinkValue = button.prop('href');
    // Updating the permalink inside modal.
    $(this).find('.form-control').val(permalinkValue);
  });
  /**
   * Permalink: Events triggered after the permalink modal is displayed.
   */
  $(document).on('shown.bs.modal', '#permalinkModal', function() {
    // Autofocus on permalink field.
    $(this).find('.form-control').focus();
  })
  /**
   * Event on permalinkModal button for going to the permalink location.
   */
  $(document).on('click', '#permalinkModal input.btn-primary', function() {
    window.location = $('#permalinkModal').find('.form-control').val();
  });
});

/**
 * Delete a comment.
 */
require(['jquery', 'xwiki-events-bridge'], function($) {
  /**
   * Getting the button that triggers the modal.
   */
  $(document).on('show.bs.modal', '#deleteModal', function(event) {
    $(this).data('relatedTarget', $(event.relatedTarget));
  });
  /**
   * Event on deleteModal button.
   */
  $(document).on('click', '#deleteModal input.btn-danger', function() {
    var modal = $('#deleteModal');
    var button = modal.data('relatedTarget');
    var notification;
    /**
     * Ajax request made for deleting the comment.
     * Delete the HTML element on succes (replace it with a small notification message).
     * Display error message on failure.
     * Disable the delete button before the request is send, so the user cannot resend it in case it takes longer.
     */
    $.ajax({
      url : button.prop('href'),
      beforeSend : function() {
        button.prop('disabled', true);
        notification = new XWiki.widgets.Notification(
          "$services.localization.render('core.viewers.comments.delete.inProgress')", 'inprogress');
      },
      success : function() {
        var comment = button.closest('.xwikicomment');
        var commentForm = comment.nextAll('.commentthread').find('form');
        // If the form is inside this comment's reply thread, move it back to the bottom.
        if(commentForm.length){
          commentForm.find('.cancel')[0].click();
        }
        // Replace the comment with a "deleted comment" placeholder.
        comment.replaceWith(createNotification("$services.localization.render('core.viewers.comments.commentDeleted')"));
        updateCount();
        notification.replace(new XWiki.widgets.Notification(
          "$services.localization.render('core.viewers.comments.delete.done')", 'done'));
        // fire an event for the annotations to know when a comment / annotation is deleted
        // FIXME: This is not the best way to go because the Annotations system should be in charge of
        // properly deleting annotations, not the Comments system. Try to find an alternative for the future.
        if (comment.hasClass('annotation')) {
          $(document).trigger("xwiki:annotation:tab:deleted");
        }
      },
      error: function() {
        // The button is enabled in case of error.
        button.prop('disabled', false);
        notification.replace(new XWiki.widgets.Notification(
          "$services.localization.render('core.viewers.comments.delete.failed')", 'error'));
      }
    })
  });
  /**
   * Just a simple message box that is used as a placeholder for a deleted comment.
   */
  var createNotification = function(message) {
    var msg = new Element('div', {'class' : 'notification' });
    msg.update(message);
    return msg;
  };
  /**
   * Updating the number of comments in commentsTab and in More actions menu.
   */
  var updateCount = function() {
    var commentsTab = $('#Commentstab').find('.itemCount');
    var commentsNumber = $('.xwikicomment').length;
    if(commentsTab) {
      commentsTab.text("$services.localization.render('docextra.extranb', ['__number__'])".replace("__number__", commentsNumber));
    }
    if($('#tmComment').length) {
      // All the sub-nodes of tmComment are added in a normalized form.
      $('#tmComment')[0].normalize();
      var label = " $services.localization.render('docextra.comments') $services.localization.render('docextra.extranb', ['__number__'])";
      label = label.replace("__number__", commentsNumber);
      $('#tmComment').contents().last()[0].nodeValue=label;
    }
  };
  // Update the comments count when the comments tab is reloaded (e.g. because a comment is added). This works right now
  // only because we don't have pagination for comments.
  $(document).on('xwiki:docextra:loaded', function(event, data) {
    if (data.id === 'Comments') {
      updateCount();
    }
  });
});
