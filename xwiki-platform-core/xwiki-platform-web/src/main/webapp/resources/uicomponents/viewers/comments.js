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
  xcommentSelector : ".xwikicomment",
  /** Constructor. Adds all the JS improvements of the Comments area. */
  initialize : function() {
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
  },
  /** Enhance the Comments UI with JS behaviors. */
  startup : function() {
    if ($("commentform")) {
      this.form = $("commentform").up("form");
    } else {
      this.form = undefined;
    }
    this.loadIDs();
    this.addDeleteListener();
    this.addReplyListener();
    this.addSubmitListener(this.form);
    this.addCancelListener();
    this.addEditListener();
    this.addPreview(this.form);
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
   * Ajax comment deletion.
   * For all delete buttons, listen to "click", and make ajax request to remove the comment. Remove the corresponding
   * HTML element on succes (replace it with a small notification message). Display error message (alert) on failure.
   */
  addDeleteListener : function() {
    $$(this.xcommentSelector).each(function(item) {
      // Prototype bug in Opera: $$(".comment a.delete") returns only the first result.
      // Quick fix until Prototype 1.6.1 is integrated.
      item = item.down('a.delete');
      if (!item) {
        return;
      }
      item.observe('click', function(event) {
        item.blur();
        event.stop();
        if (item.disabled) {
          // Do nothing if the button was already clicked and it's waiting for a response from the server.
          return;
        } else {
          new XWiki.widgets.ConfirmedAjaxRequest(
            /* Ajax request URL */
            item.readAttribute('href') + (Prototype.Browser.Opera ? "" : "&ajax=1"),
            /* Ajax request parameters */
            {
              onCreate : function() {
                // Disable the button, to avoid a cascade of clicks from impatient users
                item.disabled = true;
              },
              onSuccess : function() {
                // Remove the corresponding HTML element from the UI and update the comment count
                var comment = item.up(this.xcommentSelector);
                // If the form is inside this comment's reply thread, move it back to the bottom.
                if (this.form && this.form.descendantOf(comment.next('.commentthread'))) {
                  this.resetForm();
                }
                // Replace the comment with a "deleted comment" placeholder
                comment.replace(this.createNotification("$services.localization.render('core.viewers.comments.commentDeleted')"));
                this.updateCount();
                // fire an event for the annotations to know when a comment / annotation is deleted
                // FIXME: This is not the best way to go because the Annotations system should be in charge of properly deleting annotations,
                // not the Comments system. Try to find an alternative for the future.
                if (comment.hasClassName('annotation')) {
                  document.fire("xwiki:annotation:tab:deleted");
                }
              }.bind(this),
              onComplete : function() {
                // In the end: re-enable the button
                item.disabled = false;
              }
            },
            /* Interaction parameters */
            {
               confirmationText: "$services.localization.render('core.viewers.comments.delete.confirm')",
               progressMessageText : "$services.localization.render('core.viewers.comments.delete.inProgress')",
               successMessageText : "$services.localization.render('core.viewers.comments.delete.done')",
               failureMessageText : "$services.localization.render('core.viewers.comments.delete.failed')"
            }
          );
        }
      }.bindAsEventListener(this));
    }.bind(this));
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
        if (item.disabled) {
          // Do nothing if the button was already clicked and it's waiting for a response from the server.
          return;
        } else if (item._x_editForm){
          // If the form was already fetched, but hidden after cancel, just show it again
          // without making a new request
          var comment = item.up(this.xcommentSelector);
          comment.hide();
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
                item._x_notification = new XWiki.widgets.Notification("$services.localization.render('core.viewers.comments.editForm.fetch.inProgress')", "inprogress");
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
                this.addPreview(item._x_editForm);
                item._x_editForm.down('a.cancel').observe('click', this.cancelEdit.bindAsEventListener(this, item));
                comment.hide();
                item._x_notification.hide();
                // Currently editing: this comment
                this.editing = item;
              }.bind(this),
              onFailure : function (response) {
                var failureReason = response.statusText;
                if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                  failureReason = 'Server not responding';
                }
                item._x_notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.viewers.comments.editForm.fetch.failed')" + failureReason, "error"));
              }.bind(this),
              on0 : function (response) {
                response.request.options.onFailure(response);
              },
              onComplete : function() {
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
    comment.show();
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
      // If the form was already displayed as a reply, re-enable the Reply button for the old location
      if (this.form.up('.commentthread')) {
        this.form.up(".commentthread").previous(this.xcommentSelector).down('a.commentreply').show();
      }
      // Insert the form on top of that comment's discussion
      item.up(this.xcommentSelector).next('.commentthread').insert({'top' : this.form});
      // Set the replyto field to the replied comment's number
      this.form["XWiki.XWikiComments_replyto"].value = item.up(this.xcommentSelector)._x_number;
      // Clear the contents and focus the textarea
      this.form["XWiki.XWikiComments_comment"].value = "";
      this.form["XWiki.XWikiComments_comment"].focus();
      // Hide the reply button
      item.hide();
    }.bindAsEventListener(this));
  },
  /**
   * When pressing Submit, check that the comment is not empty. Submit the form with ajax and update the whole comments
   * zone on success.
   */
  addSubmitListener : function(form) {
    if (form) {
      // Add listener for submit
      form.down("input[type='submit']").observe('click', function(event) {
        event.stop();
        if (form.down('textarea').value != "") {
          var formData = new Hash(form.serialize(true));
          formData.set('xredirect', window.docgeturl + '?xpage=xpart&vm=commentsinline.vm&skin=' + encodeURIComponent(XWiki.skin));
          // Allows CommentAddAction to parse a template which will return a message telling if the captcha was wrong.
          formData.set('xpage', 'xpart');
          formData.set('vm', 'commentsinline.vm');
          formData.set('skin', XWiki.skin);
          // Strip form parameters from the form action query string to prevent them from being overwriten.
          var queryStringParams = $H(form.action.toQueryParams());
          formData.keys().each(queryStringParams.unset.bind(queryStringParams));
          var url = form.action.replace(/\?.*/, '?' + queryStringParams.toQueryString());
          formData.unset('action_cancel');
          // Create a notification message to display to the user when the submit is being sent
          form._x_notification = new XWiki.widgets.Notification("$services.localization.render('core.viewers.comments.add.inProgress')", "inprogress");
          form.disable();
          this.restartNeeded = false;
          new Ajax.Request(url, {
            method : 'post',
            parameters : formData,
            onSuccess : function () {
              this.restartNeeded = true;
              this.editing = false;
              form._x_notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.viewers.comments.add.done')", "done"));
            }.bind(this),
            onFailure : function (response) {
              var failureReason = response.statusText;
              if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                failureReason = 'Server not responding';
              }
              form._x_notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.viewers.comments.add.failed')" + failureReason, "error"));
            }.bind(this),
            on0 : function (response) {
              response.request.options.onFailure(response);
            },
            onComplete : function (response) {
              // We enable the form even when it is going to be replaced because the browser caches the state of the form
              // input fields and the user will get disabled fields when (soft) reloading the page.
              form.enable();
              if (this.restartNeeded) {
                this.container.update(response.responseText);
                document.fire("xwiki:docextra:loaded", {
                  "id" : "Comments",
                  "element": this.container
                });
                this.updateCount();
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
    if (!form || !XWiki.hasEdit) {
      return;
    }
    var previewURL = "$xwiki.getURL('__space__.__page__', 'preview')".replace("__space__", encodeURIComponent(XWiki.currentSpace)).replace("__page__", encodeURIComponent(XWiki.currentPage));
    form.commentElt = form.down('textarea');
    var buttons = form.down('input[type=submit]').up('div');
    form.previewButton = new Element('span', {'class' : 'buttonwrapper'}).update(new Element('input', {'type' : 'button', 'class' : 'button', 'value' : "$services.localization.render('core.viewers.comments.preview.button.preview')"}));
    form.previewButton._x_modePreview = false;
    form.previewContent = new Element('div', {'class' : 'commentcontent commentPreview'});
    form.commentElt.insert({'before' : form.previewContent});
    form.previewContent.hide();
    buttons.insert({'top' : form.previewButton});
    form.previewButton.observe('click', function() {
      if (!form.previewButton._x_modePreview && !form.previewButton.disabled) {
         form.previewButton.disabled = true;
         var notification = new XWiki.widgets.Notification("$services.localization.render('core.viewers.comments.preview.inProgress')", "inprogress");
         new Ajax.Request(previewURL, {
            method : 'post',
            parameters : {'xpage' : 'plain', 'sheet' : '', 'content' : form.commentElt.value},
            onSuccess : function (response) {
              this.doPreview(response.responseText, form);
              notification.hide();
            }.bind(this),
            /* If the content is empty or does not generate anything, we have the "This template does not exist" response,
               with a 400 status code. */
            on400 : function(response) {
              this.doPreview('&nbsp;', form);
              notification.hide();
            }.bind(this),
            onFailure : function (response) {
              var failureReason = response.statusText;
              if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
                failureReason = 'Server not responding';
              }
              notification.replace(new XWiki.widgets.Notification("$services.localization.render('core.viewers.comments.preview.failed')" + failureReason, "error"));
            },
            on0 : function (response) {
              response.request.options.onFailure(response);
            },
            onComplete : function (response) {
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
  doPreview : function(content, form) {
    form.previewButton._x_modePreview = true;
    form.previewContent.update(content);
    form.previewContent.show();
    form.commentElt.hide();
    form.previewButton.down('input').value = "$services.localization.render('core.viewers.comments.preview.button.back')";
  },
  /**
   * Display the comment textarea instead of the comment preview.
   *
   * @param form the form for which the preview is canceled
   */
  cancelPreview : function(form) {
    form.previewButton._x_modePreview = false;
    form.previewContent.hide();
    form.previewContent.update('');
    form.commentElt.show();
    form.previewButton.down('input').value = "$services.localization.render('core.viewers.comments.preview.button.preview')";
  },
  resetForm : function (event) {
    if (event) {
      event.stop();
    }
    if (this.form.up('.commentthread')) {
      // Show the comment's reply button
      this.form.up(".commentthread").previous(this.xcommentSelector).down('a.commentreply').show();
      // Put the form back to its initial location and clear the contents
      this.initialLocation.insert({after: this.form});
    }
    this.form["XWiki.XWikiComments_replyto"].value = "";
    this.form["XWiki.XWikiComments_comment"].value = "";
    this.cancelPreview(this.form);
  },
  updateCount : function() {
    if ($("Commentstab") && $("Commentstab").down(".itemCount")) {
      $("Commentstab").down(".itemCount").update("$services.localization.render('docextra.extranb', ['__number__'])".replace("__number__", $$(this.xcommentSelector).size()));
    }
    if ($("commentsshortcut") && $("commentsshortcut").down(".itemCount")) {
      $("commentsshortcut").down(".itemCount").update("$services.localization.render('docextra.extranb', ['__number__'])".replace("__number__", $$(this.xcommentSelector).size()));
    }
  },
  /**
   * Registers a listener that watches for the insertion of the Comments tab and triggers the enhancements.
   * After that, the listener removes itself, since it is no longer needed.
   */
  addTabLoadListener : function(event) {
    var listener = function(event) {
      if (event.memo.id == 'Comments') {
        this.startup();
      }
    }.bindAsEventListener(this);
    document.observe("xwiki:docextra:loaded", listener);
  },
  /**
   * Just a simple message box that is displayed at various events: comment deleted, sending comment...
   */
  createNotification : function(message) {
    var msg = new Element('div', {"class" : "notification" });
    msg.update(message);
    return msg;
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

require(['jquery'], function ($) {
  /**
 * Permalink: Events triggered when the permalink modal is displayed
 */
  $(document).on('show.bs.modal','#permalinkModal', function (event) {
    // Updating the permalink inside modal
    var modal = $(this);
    var button = $(event.relatedTarget);
    var permalinkValue = button.attr('href');
    modal.find('.form-control').val(permalinkValue);
    // Going to permalink locations
    $('#permalinkModal .btn-group .btn-primary').on('click', function() {
      window.location = permalinkValue;
    });
  })
});