// ======================================
// Form buttons: shortcuts, AJAX improvements and form validation
// To be completed.
if (typeof(XWiki) == 'undefined') {
  XWiki = new Object();
}
if (typeof(XWiki.actionButtons) == 'undefined') {
  XWiki.actionButtons = new Object();
}

XWiki.actionButtons.EditActions = Class.create({
  initialize : function() {
    this.addListeners();
    this.addShortcuts();
    this.saveAndContinueListener = new XWiki.actionButtons.EditActions.AjaxSaveAndContinue(this);
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
  },
  addShortcuts : function() {
    var shortcuts = {
      'action_cancel' : "$msg.get('core.shortcuts.edit.cancel')",
      'action_preview' : "$msg.get('core.shortcuts.edit.preview')",
      // The following 2 are both "Back to edit" in the preview mode, depending on the used editor
      'action_edit' : "$msg.get('core.shortcuts.edit.backtoedit')",
      'action_inline' : "$msg.get('core.shortcuts.edit.backtoedit')",
      'action_save' : "$msg.get('core.shortcuts.edit.saveandview')",
      'action_saveandcontinue' : "$msg.get('core.shortcuts.edit.saveandcontinue')"
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
  validateForm : function(form) {
    #if($xwiki.isEditCommentMandatory())
      var commentField = form.comment
      while (commentField.value == "") {
        var response = prompt("${msg.get('core.comment.prompt')}");
        if (response === null) {
          return false;
        }
        commentField.value = response;
      }
    #elseif($xwiki.isEditCommentSuggested())
      var commentField = form.comment
      if (commentField.value == "") {
        var response = prompt("${msg.get('core.comment.prompt')}");
        if (response === null) {
          return false;
        }
        commentField.value = response;
      }
    #end
    return true;
  },
  onCancel : function(evt) {
    evt.stop();
    var location = evt.element().form.action;
    if (location.indexOf('?') == -1) {
      location += '?';
    }
    window.location = location + '&action_cancel=true'; 
  },
  onPreview : function(evt) {
    if (!this.validateForm(evt.element().form)) {
      evt.stop();
    }
  },
  onSaveAndView : function(evt) {
    if (!this.validateForm(evt.element().form)) {
      evt.stop();
    }
  }
});

// ======================================
// Save and continue button: Ajax improvements
XWiki.actionButtons.EditActions.AjaxSaveAndContinue = Class.create({
  saveAndContinue : false,
  effectDuration : 1.0,
  initialize : function(editActions) {
    this.editActions = editActions;
    this.createMessages();
    this.addListeners();
  },
  createMessages : function() {
    this.container = new Element('div').setStyle({
      'fontWeight' : 'bold',
      position : 'fixed',
      bottom: '8px',
      left: '0%',
      width: '100%',
      'textAlign': 'center',
      zIndex: 1200
    }).hide();
    var innerContainer = new Element('div');
    this.savingBox = new Element('span', { 'class' : 'plainmessage' }).setStyle({
      background: "#000 url($xwiki.getSkinFile('icons/xwiki/ajax-loader-white.gif')) 8px center no-repeat",
      margin: 'auto',
      padding: '8px 32px',
      color: '#FFF'
    }).update('Saving...').hide();
    this.savedBox = new Element('span').setStyle({
      background: "#000 url($xwiki.getSkinFile('icons/silk/tick.gif')) 8px center no-repeat",
      margin: 'auto',
      padding: '8px 32px',
      color: '#FFF'
    }).update('Saved').hide();
    this.failedBox = new Element('span').setStyle({
      background: "#DDD url($xwiki.getSkinFile('icons/msgerror.png')) 8px center no-repeat",
      margin: 'auto',
      padding: '6px 32px 4px',
      color: '#F00',
      border: '2px solid #F00'
    }).update('Failed to save the document. Reason: <span id="ajaxRequestFailureReason"></span>').hide();
    innerContainer.appendChild(this.savingBox);
    innerContainer.appendChild(this.savedBox);
    innerContainer.appendChild(this.failedBox);
    this.container.appendChild(innerContainer);
    document.body.appendChild(this.container);
  },
  addListeners : function() {
    $$('input[name=action_saveandcontinue]').each(function(item){
      $(item.form).observe('submit', this.onSubmitting.bindAsEventListener(this));
      item.observe('click', this.onSAC.bindAsEventListener(this));
    }.bind(this));
  },
  onSubmitting : function(event) {
    if (this.saveAndContinue) {
      event.stop();
      this.saveAndContinue = false;
      this.savedBox.hide();
      this.failedBox.hide();
      this.savingBox.show();
      this.showMessage();
      var formData = new Hash(event.element().serialize({hash: true, submit: 'action_save'}));
      if (!Prototype.Browser.Opera) {
        // Opera can't handle properly 204 responses.
        formData.set('ajax', 'true');
      }
      new Ajax.Request(event.element().action, {
        method : 'post',
        parameters : formData.toQueryString(),
        onSuccess : this.onSuccess.bindAsEventListener(this),
        on1223 : this.on1223.bindAsEventListener(this),
        on0 : this.on0.bindAsEventListener(this),
        onFailure : this.onFailure.bind(this)
      });
    }
  },
  onSAC : function(event) {
    if (!this.editActions.validateForm(event.element().form)) {
      event.stop();
      return false;
    }
    this.saveAndContinue = true;
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
    this.savingBox.hide();
    this.savedBox.show();
    this.hideMessage();
  },
  onFailure : function(response) {
    if (response.statusText == '' /* No response */ || response.status == 12031 /* In IE */) {
      $('ajaxRequestFailureReason').update('Server not responding');
    } else {
      $('ajaxRequestFailureReason').update(response.statusText);
    }
    this.savingBox.hide();
    this.failedBox.show();
    this.hideMessage();
  },
  showMessage : function() {
    if (this.hideEffect) {
      this.hideEffect.cancel();
      delete this.hideEffect;
    }
    if (Prototype.Browser.IE) {
      this.container.setStyle({position : 'absolute', 'bottom': '0px'});
    }
    if (!this.showEffect && !this.container.visible()) {
      this.showEffect = new Effect.Appear(this.container, {duration: 0.2, afterFinish: function() {
        delete this.showEffect;
      }.bind(this)});
    }
  },
  hideMessage : function() {
    this.hideEffect = new Effect.Fade(this.container, {duration: this.effectDuration, delay: 2, afterFinish: function() {
      this.savedBox.hide();
      this.failedBox.hide();
      delete this.hideEffect;
    }.bind(this)});
  }
});
document.observe('dom:loaded', function() {
  new XWiki.actionButtons.EditActions();
});