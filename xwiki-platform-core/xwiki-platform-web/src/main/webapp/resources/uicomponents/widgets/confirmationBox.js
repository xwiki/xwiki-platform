// Make sure the XWiki 'namespace' and the ModalPopup class exist.
if(typeof(XWiki) == "undefined" || typeof(XWiki.widgets) == "undefined" || typeof(XWiki.widgets.ModalPopup) == "undefined") {
  if (typeof console != "undefined" && typeof console.warn == "function") {
    console.warn("[ConfirmationBox widget] Required class missing: XWiki.widgets.ModalPopup");
  }
} else {
  XWiki.widgets.ConfirmationBox = Class.create(XWiki.widgets.ModalPopup, {
    /** Default displayed texts */
    defaultInteractionParameters : {
      confirmationText: "$services.localization.render('core.widgets.confirmationBox.defaultQuestion')",
      yesButtonText: "$services.localization.render('core.widgets.confirmationBox.button.yes')",
      noButtonText: "$services.localization.render('core.widgets.confirmationBox.button.no')"
    },
    /** Constructor. Registers the key listener that pops up the dialog. */
    initialize : function($super, behavior, interactionParameters) {
      this.interactionParameters = Object.extend(Object.clone(this.defaultInteractionParameters), interactionParameters || {});
      $super(
        this.createContent(this.interactionParameters),
        {
          "show"  : { method : this.showDialog,  keys : [] },
          "yes"   : { method : this.onYes,       keys : ['Enter', 'Space'] },
          "no"    : { method : this.onNo,        keys : ['Esc'] },
          "close" : { method : this.closeDialog, keys : [] }
        },
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
      var question = new Element("div", {"class" : "question"}).update(data.confirmationText);
      var buttons = new Element("div", {"class" : "buttons"});
      var yesButton = this.createButton("button", data.yesButtonText, "(Enter)", "");
      var noButton = this.createButton("button", data.noButtonText, "(Esc)", "");
      buttons.insert(yesButton);
      buttons.insert(noButton);
      var content =  new Element("div");
      content.insert(question).insert(buttons);
      Event.observe(yesButton, "click", this.onYes.bindAsEventListener(this));
      Event.observe(noButton, "click", this.onNo.bindAsEventListener(this));
      return content;
    },
    onYes : function() {
      this.closeDialog();
      if (typeof (this.behavior.onYes) == 'function') {
        this.behavior.onYes();
      }
    },
    onNo : function() {
      this.closeDialog();
      if (typeof (this.behavior.onNo) == 'function') {
        this.behavior.onNo();
      }
    }
  });
} // if the parent widget is defined
