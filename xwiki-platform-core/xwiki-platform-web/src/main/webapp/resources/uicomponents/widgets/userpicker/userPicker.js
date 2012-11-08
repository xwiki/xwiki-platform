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
  initialize: function($super, input, options) {
    $super(input, Object.extend({
      varname: "input",
      enableHideButton: false,
      timeout: 30000
    }, options || {}));

    var listInsertionElement = input;
    if (input.hasClassName('withScope')) {
      this._sourceURL = options.script;
      this._createScope();
      listInsertionElement = input.up();
    }
    this.suggestPicker = new SelectionManager(input, this, {
      listInsertionElement: listInsertionElement,
      listInsertionPosition : 'before',
      acceptFreeText : true
    });
    this.multipleSelection = input.hasClassName('multipleSelection');
    // Keep a reference to this user picker instance if the target input has the ID attribute specified.
    if (input.id != '') {
      widgets.UserPicker.instances[input.id] = this;
    }
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
  },

  /**
   * Clears the content of the target input and the current selection.
   */
  clear : function() {
    this.fld.clear();
    this.suggestPicker.clearAcceptedList();
  },

  _createScope: function() {
    this._scope = new Element('input', {
      type: 'image',
      'class': 'scope',
      alt: "$msg.get('core.widgets.userPicker.scopeHint')",
      title: "$msg.get('core.widgets.userPicker.scopeHint')"
    });
    // Group the scope and the input.
    var wrapper = new Element('span').insert(this._scope);
    this.fld.insert({before: wrapper});
    wrapper.insert(this.fld);
    // Add click handler.
    this._scope.observe('click', this._toggleScope.bindAsEventListener(this));
    // Initialize the scope value.
    this._toggleScope();
  },

  /**
   * Toggles between local and global scope.
   */
  _toggleScope: function(event) {
    event && event.stop();
    if (this._scope.value == 'local') {
      this._scope.value = 'global';
      this._scope.src = "$xwiki.getSkinFile('icons/silk/world.png')";
    } else {
      this._scope.value = 'local';
      this._scope.src = "$xwiki.getSkinFile('icons/silk/world_delete.png')";
    }
    this.sources[0].script = this._sourceURL + 'wiki=' + this._scope.value + '&';
  }
});
// Used to access the created user pickers.
widgets.UserPicker.instances = {};

var init = function(event) {
  var suggestionsMapping = {
    users: {
      script: XWiki.currentDocument.getURL('get', 'xpage=uorgsuggest&uorg=user&'),
      noresults: "$msg.get('core.widgets.userPicker.noResults')"
    },
    groups: {
      script: XWiki.currentDocument.getURL('get', 'xpage=uorgsuggest&uorg=group&'),
      noresults: "$msg.get('core.widgets.groupPicker.noResults')"
    }
  }
  var containers = (event && event.memo.elements) || [$('body')];
  Object.keys(suggestionsMapping).each(function(key) {
    containers.each(function(container) {
      container.select('input.suggest' + key.capitalize()).each(function(input) {
        if (!input.hasClassName('initialized')) {
          new widgets.UserPicker(input, suggestionsMapping[key]);
          input.addClassName('initialized');
        }
      });
    });
  });
  return true;
};
(XWiki.domIsLoaded && init()) || document.observe('xwiki:dom:loaded', init);
document.observe('xwiki:dom:updated', init);

// End XWiki augmentation.
return XWiki;
})(XWiki || {});
