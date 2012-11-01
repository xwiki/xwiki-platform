var XWiki = (function(XWiki) {
// Start XWiki augmentation.
var widgets = XWiki.widgets = XWiki.widgets || {};

/**
 * Extends the SuggestPicker to customize the way the selected users are displayed.
 */
var SelectionManager = Class.create(widgets.SuggestPicker, {
  // @Override
  matchesSelectedValue: function(value, suggestion) {
    // The given value can be a relative user reference. We need to parse it and match only the user alias.
    return XWiki.Model.resolve(value, XWiki.EntityType.DOCUMENT).name == suggestion.id;
  },

  // @Override
  getDefaultSuggestion: function(value) {
    var userReference = XWiki.Model.resolve(value, XWiki.EntityType.DOCUMENT);
    return {
      id: userReference.name,
      value: value,
      info: userReference.name,
      icon: "$xwiki.getSkinFile('icons/xwiki/noavatar.png')"
    };
  },

  // @Override
  addItem: function($super, suggestion) {
    // Clear the previously selected user in single selection mode.
    this.input.hasClassName('multipleSelection') || this.list.update('');
    $super(suggestion);
  },

  // @Override
  displayItem: function(suggestion) {
    var itemDisplay = this.suggest.createItemDisplay(suggestion, {});
    itemDisplay.down('.user-name').insert(this.createDeleteTool());
    return new Element('li').insert(itemDisplay).insert(this.createItemInput(suggestion));
  }
});

/**
 * Enhances a plain HTML text input with the ability to suggest users.
 */
widgets.UserPicker = Class.create(widgets.Suggest, {
  // @Override
  initialize: function($super, input) {
    $super(input, {
      script: XWiki.currentDocument.getURL('get', 'xpage=uorgsuggest&wiki=local&uorg=user&'),
      varname: "input",
      noresults: "$msg.get('core.widgets.userPicker.noResults')",
      enableHideButton: false,
      timeout: 30000
    });
    this.suggestPicker = new SelectionManager(input, this, {
      listInsertionPosition : 'before',
      acceptFreeText : true
    });
    this.multipleSelection = input.hasClassName('multipleSelection');
  },

  // @Override
  createItemDisplay: function(data, source) {
    var container = new Element('div', {'class': 'user'});
    var avatarWrapper = new Element('div', {'class': 'user-avatar-wrapper'});
    avatarWrapper.insert(new Element('img', {src: data.icon, alt: data.info, 'class': 'icon'}));
    container.insert(avatarWrapper);
    var userName = source.highlight ? this.emphasizeMatches(this.sInput, data.info) : data.info;
    container.insert(new Element('div', {'class': 'user-name'}).update(userName));
    var userAlias = source.highlight ? this.emphasizeMatches(this.sInput, data.id) : data.id;
    container.insert(new Element('div', {'class': 'user-alias'}).update(userAlias));
    return container;
  }
});

var init = function() {
  $('body').select('input.suggestUsers').each(function(input) {
    if (!input.hasClassName('initialized')) {
      new widgets.UserPicker(input);
      input.addClassName('initialized');
    }
  });
  return true;
};
(XWiki.domIsLoaded && init()) || document.observe('xwiki:dom:loaded', init);
document.observe('xwiki:dom:updated', init);

// End XWiki augmentation.
return XWiki;
})(XWiki || {});
