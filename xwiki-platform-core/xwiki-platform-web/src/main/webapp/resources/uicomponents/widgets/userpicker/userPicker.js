var XWiki = (function defineUserPicker(XWiki) {

// Start XWiki augmentation.
var widgets = XWiki.widgets;
// TODO: Use require.js here when we move away from Prototype.js
if (!widgets || !widgets.SuggestPicker) {
  var tryCount = arguments[1] || 0;
  if (tryCount > 5) {
    console.error('Failed to define the UserPicker module: required dependency SuggestPicker is missing.');
  } else {
    setTimeout(defineUserPicker.bind(window, XWiki, tryCount + 1), 0);
  }
  return XWiki;
}

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
  acceptSuggestion: function(suggestion) {
    // We look for a suggestion that is already selected and has the same value. We don't rely on the suggestion id
    // because the id is the user alias and we can have two users with the same alias but from different wikis.
    // See XWIKI-11868: Cannot add an entity that has the same local and global name with the user/group picker
    if (!this.list.down('input[value="' + suggestion.value + '"]')) {
      this.addItem(suggestion);
    }
    this.input.value = '';
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
  },

  // @Override
  createItemInput: function(suggestion) {
    // We don't have to set the input id because the input is hidden. At the same time the suggestion id is the user
    // alias and we can have two users with the same alias but from different wikis (and thus we would get two hidden
    // inputs with the same id).
    return new Element('input', {type: 'hidden', name: this.inputName, value: suggestion.value});
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
    // The scope toggle doesn't make sense when global users are required.
    if (input.hasClassName('withScope') && !input.hasClassName('global')) {
      this._sourceURL = options.script;
      this._createScope();
      listInsertionElement = input.up();
    }
    this._selectionManager = this._createSelectionManager({
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
    var userName = source.highlight ? this.emphasizeMatches(this.sInput.escapeHTML(), data.info.escapeHTML()) : data.info.escapeHTML();
    container.insert(new Element('div', {'class': 'user-name'}).update(userName));
    var referenceWrapper = new Element('div');
    var userAlias = source.highlight ? this.emphasizeMatches(this.sInput.escapeHTML(), data.id.escapeHTML()) : data.id.escapeHTML();
    referenceWrapper.insert(new Element('span', {'class': 'user-alias'}).update(userAlias));
    var userReference = XWiki.Model.resolve(data.value, XWiki.EntityType.DOCUMENT);
    var wiki = userReference.extractReferenceValue(XWiki.EntityType.WIKI) || XWiki.currentWiki;
    // Display the wiki only for local users (in order to be consistent with the display in view mode).
    if (wiki !== '$xcontext.mainWikiName') {
      referenceWrapper.insert(new Element('span', {'class': 'user-wiki'}).update(wiki.escapeHTML()));
    }
    container.insert(referenceWrapper);
    return container;
  },

  /**
   * Clears the content of the target input and the current selection.
   */
  clear : function() {
    this.fld.clear();
    this._selectionManager.clearAcceptedList();
  },

  /**
   * Determines the selection manager that is going to be used. Derived classes can override this method if they want
   * to use a different selection manager.
   */
  _createSelectionManager: function(options) {
    return new SelectionManager(this.fld, this, options);
  },

  _createScope: function() {
    this._scope = new Element('input', {
      type: 'image',
      'class': 'scope',
      alt: "$services.localization.render('core.widgets.userPicker.scopeHint')",
      title: "$services.localization.render('core.widgets.userPicker.scopeHint')"
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
  },

  // @Override
  detach: function($super) {
    // Remove the list of accepted suggestions.
    this._selectionManager && this._selectionManager.detach();

    // Remove the scope toggle.
    this._scope && this._scope.stopObserving('click').up().insert({before: this.fld}).remove();

    $super();
  }
});
// Used to access the created user pickers.
widgets.UserPicker.instances = {};

var init = function(event) {
  var suggestionsMapping = {
    users: {
      script: XWiki.currentDocument.getURL('get', 'xpage=uorgsuggest&uorg=user&'),
      noresults: "$services.localization.render('core.widgets.userPicker.noResults')"
    },
    groups: {
      script: XWiki.currentDocument.getURL('get', 'xpage=uorgsuggest&uorg=group&'),
      noresults: "$services.localization.render('core.widgets.groupPicker.noResults')"
    }
  }
  var containers = (event && event.memo.elements) || [$('body')];
  Object.keys(suggestionsMapping).each(function(key) {
    containers.each(function(container) {
      container.select('input.suggest' + key.capitalize()).each(function(input) {
        if (!input.hasClassName('initialized')) {
          var options = Object.clone(suggestionsMapping[key]);
          // The picker suggests by default local users or groups.
          if (input.hasClassName('global')) {
            // Suggest global users or groups.
            options.script = options.script + 'wiki=global&';
          }
          new widgets.UserPicker(input, options);
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
