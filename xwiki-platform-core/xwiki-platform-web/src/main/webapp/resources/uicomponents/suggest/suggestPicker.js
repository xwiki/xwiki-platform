/**
 * Add-on for the Suggest widget which allows multiple values to be selected and displayed in a list. It requires an existing input and a Suggest instance already bound to the input.
 */
var XWiki = (function (XWiki) {
  var widgets = XWiki.widgets = XWiki.widgets || {};
  widgets.SuggestPicker = Class.create({

  options : {
    // Display the internal value?
    'showKey' : false,
    // If there's extra information for each item (the 'info' field), display it as a tooltip?
    'showTooltip' : false,
    // Show a delete icon next to each selected entry?
    'showDeleteTool' : true,
    // Should the list of selected values be sortable? Requires scriptaculous/dragdrop.js to be available.
    'enableSort' : true,
    // Show a "delete all" button after the list of suggestions?
    'showClearTool' : true,
    // Which type of input should be used for the selected values? Another good option is "checkbox"
    'inputType': 'hidden',
    // The element used as a reference insertion point. By default the enhanced input is used. Either a CSS selector or an actual element can be used.
    'listInsertionElement' : null,
    // Where should the list of selected items be placed? Valid options are 'before', 'top', 'bottom', 'after'.
    'listInsertionPosition' : 'after',
    // Accept free text, i.e. text that was entered by the user but didn't match anything on the server (no suggestions)?
    'acceptFreeText' : false,
    // Optional callback method called whenever a new item has been added
    'onItemAdded' : Prototype.emptyFunction,
    // The character used to separate the multiple values in the initial content of the target text input.
    'separator' : ','
  },
  /**
   * Constructor method.
   *
   * @param element the DOM <input> Element to bind to
   * @param suggest the instance of XWiki.widgets.Suggest already bound to the input
   * @param options additional configuration of the picker; see the {@link #options} field for possible options to specify
   */
  initialize: function(element, suggest, options) {
    // Pre-bind callback functions
    this.removeItem = this.removeItem.bindAsEventListener(this);
    this.checkboxChanged = this.checkboxChanged.bindAsEventListener(this);

    // Process options
    this.options = Object.extend(Object.clone(this.options), options || { });
    this.input = $(element);
    this.suggest = suggest;
    this.inputName = this.input.name;
    if (!this.options.acceptFreeText) {
      this.input.name = this.input.name + "__suggested";
    } else {
      this.input.addClassName("accept-value");
    }
    this.suggest.options.callback = this.acceptSuggestion.bind(this);

    // Create the list element which will hold the accepted elements
    this.list = new Element('ul', {'class' : 'accepted-suggestions'});
    var listInsertionElement;
    if (this.options.listInsertionElement) {
      if (typeof(this.options.listInsertionElement) === "string") {
        listInsertionElement = this.input.up().down(this.options.listInsertionElement);
      } else {
        listInsertionElement = this.options.listInsertionElement;
      }
    }
    if (!listInsertionElement) {
      listInsertionElement = this.input;
    }
    var insertion = {};
    insertion[this.options.listInsertionPosition] = this.list;
    listInsertionElement.insert(insertion);

    if (this.options.showClearTool) {
      this.clearTool = new Element('a', {
        href: '#clearSelection',
        'class' : 'clear-tool',
        title : "$services.localization.render('core.widgets.suggestPicker.deleteAll.tooltip')"
      }).update("$services.localization.render('core.widgets.suggestPicker.deleteAll')");
      this.clearTool.hide().observe('click', this.clearAcceptedList.bindAsEventListener(this));
      this.list.insert({'after': this.clearTool});
    }

    this.initializeSelection();
  },

  /**
   * Splits the initial content of the target text input using the configured separator and adds all the values to the
   * list of accepted suggestions.
   */
  initializeSelection: function() {
    this.input.readOnly = true;
    this.input.addClassName('loading');
    this.loadSelectedValue(this.input.value.split(this.options.separator), 0);
  },

  /**
   * Loads suggestions for the selected value in order to display it properly.
   */
  loadSelectedValue: function(values, index) {
    if (index >= values.length) {
      this.input.readOnly = false;
      this.input.value = '';
      this.input.removeClassName('loading');
      return;
    } else if (values[index].strip() == '') {
      this.loadSelectedValue(values, index + 1);
      return;
    }
    // Look for the selected value in all the sources.
    var found = false;
    var sourceIndex = 0;
    this.input.value = values[index];
    this.suggest.doAjaxRequests(-1, {
      parameters: {'exactMatch': true},
      onSuccess: function(response) {
        if (found) return;
        var suggestions = this.suggest.parseResponse(response, this.suggest.sources[sourceIndex]) || [];
        for (var i = 0; i < suggestions.length; i++) {
          if (this.matchesSelectedValue(values[index], suggestions[i])) {
            found = true;
            // Make sure the selected value is kept as is (submitting without changes must preserve the previous value).
            suggestions[i].value = values[index];
            this.addItem(suggestions[i]);
            return;
          }
        }
      }.bind(this),
      onComplete: function(response) {
        response.request.options.defaultValues.onComplete(response);
        if (++sourceIndex >= this.suggest.sources.length) {
          if (!found) this.addItem(this.getDefaultSuggestion(values[index]));
          // Load the next selected value.
          this.loadSelectedValue(values, index + 1);
        }
      }.bind(this)
    });
  },

  /**
   * @return {@code true} if the given suggestion matches perfectly the specified selected value, {@code false} otherwise
   */
  matchesSelectedValue: function(value, suggestion) {
    return value == suggestion.value;
  },

  /**
   * @return default suggestion data for when a selected value is not found in any of the configured sources
   */
  getDefaultSuggestion: function(value) {
    return {id: value, value: value, info: value};
  },

  /**
   * Callback called when the user accepted an item from the Suggest.
   *
   * @param suggestion the accepted suggestion, as an object with id, value and info values
   */
  acceptSuggestion : function(suggestion) {
    if (!this.acceptAlreadyAddedItem(suggestion.id || suggestion.value)) {
      this.addItem(suggestion);
    }
    this.input.value = "";
  },

  /**
   * Callback called when the user deleted an item from the list of accepted items.
   *
   * @param event the click event fired by the browser
   */
  removeItem : function(event) {
    var item = event.findElement('li');
    item.remove();
    this.notifySelectionChange(item);
    this.updateListTools();
    this.input.activate();
  },

  /**
   * Remove all the accepted items when the "remove all" button is clicked.
   */
  clearAcceptedList : function (event) {
    event && event.stop();
    this.list.update("");
    this.notifySelectionChange();
    this.updateListTools();
    this.input.activate();
  },

  /**
   * Callback called when the user checked or unchecked an item from the list of accepted items.
   *
   * @param event the click event fired by the browser
   */
  checkboxChanged : function(event) {
    var item = event.findElement('li');
    this.notifySelectionChange(item);
  },

  /**
   * Check if an item has been previously accepted or was displayed from the start as an unchecked checkbox, but might be unchecked, and re-enable it.
   *
   * @param key the key of the item to look for
   * @return true if the item existed and has been re-enabled, false otherwise
   */
  acceptAlreadyAddedItem : function (key) {
    var input = this.list ? this.list.down('input[id="' + this.getInputId(key).replace(/[^a-zA-Z0-9_-]/g, '\\$&') + '"]') : $(this.getInputId(key));
    if (input) {
      input.checked = true;
      this.notifySelectionChange(input.up('li') || input);
      return true;
    }
    return false;
  },

  /**
   * Add a new item in the list of accepted items.
   *
   * @param suggestion the suggestion object received from the Suggest widget
   */
  addItem : function(suggestion) {
    if (!suggestion) {
      return;
    }
    var listItem = this.displayItem(suggestion);
    this.list.insert(listItem);
    this.options.onItemAdded(listItem.down('input'));
    this.notifySelectionChange(listItem);
    this.updateListTools();
  },

  /**
   * Displays a selected item.
   */
  displayItem: function(suggestion) {
    var itemInput = this.createItemInput(suggestion);
    var listItem = new Element("li");
    var displayedValue = new Element('label', {'for' : itemInput.id}).insert({'bottom' : itemInput});
    // If the key should be displayed, insert it
    if (this.options.showKey) {
      displayedValue.insert({'bottom' : new Element("span", {"class": "key"}).update("[" + itemInput.value.escapeHTML() + "]")});
      displayedValue.insert({'bottom' : new Element("span", {"class": "sep"}).update(" ")});
    }
    // Insert the displayed value
    displayedValue.insert({'bottom' : new Element("span", {"class": "value"}).update(suggestion.value.escapeHTML())});
    listItem.insert(displayedValue);
    // Delete tool
    this.options.showDeleteTool && listItem.insert(this.createDeleteTool());
    // Tooltip, if information exists and the options state there should be a tooltip
    if (this.options.showTooltip && suggestion.info) {
      listItem.appendChild(new Element("div", {'class' : "tooltip"}).update(suggestion.info));
    }
    return listItem;
  },

  /**
   * Creates the input used to store the value of a selected item.
   */
  createItemInput: function(suggestion) {
    var inputOptions = {
      type : this.options.inputType,
      name : this.inputName,
      id : this.getInputId(suggestion.id || suggestion.value),
      value : suggestion.value || suggestion.id
    };
    if (this.options.inputType == 'checkbox') {
      inputOptions.checked = 'checked';
    }
    var input = new Element('input', inputOptions);
    input.observe('change', this.checkboxChanged);
    return input;
  },

  /**
   * Creates the tool used to delete a selected item.
   */
  createDeleteTool: function() {
    var deleteTool = new Element("span", {
      'class': "delete-tool",
      title : "$services.localization.render('core.widgets.suggestPicker.delete.tooltip')"
    }).update('&times;').observe('click', this.removeItem);
    deleteTool.insert({top: new Element('span', {'class': 'hidden'}).update('[')});
    deleteTool.insert({bottom: new Element('span', {'class': 'hidden'}).update(']')});
    return deleteTool;
  },

  /**
   * Meta-maintenance of the accepted items list: show/hide the "delete all" button, refresh the Sortable behavior.
   */
  updateListTools : function () {
    // Show/hide the "delete all" button
    if (this.clearTool) {
      if (this.list.childElements().length > 1) {
        this.clearTool.show();
      } else {
        this.clearTool.hide();
      }
    }
    // Refresh the Sortable behavior to take into account the new items
    if (this.options.enableSort && this.list.childElements().length > 1 && typeof(Sortable) != "undefined") {
      Sortable.create(this.list);
      this.list.addClassName('sortable');
    }
  },

  /**
   * Fire an event when the list of values changes.
   *
   * @param liElement an optional li element which changed (was removed or added or toggled)
   */
  notifySelectionChange : function(liElement) {
    Event.fire(document, "xwiki:multisuggestpicker:selectionchanged", {
       'trigger'    : this.input,
       'fieldName'  : this.inputName,
       'changedElement' : liElement
    });
  },

  /**
   * Compute a unique ID for a given item key.
   *
   * @param key the key of the item
   * @return an identifier obtained by concatenating the base input name and the key
   */
  getInputId : function(key) {
    return this.inputName + "_" + key;
  },

  /**
   * Remove suggest behavior from the target field (detach all listeners and remove the list of accepted suggestions)
   */
  detach : function() {
    this.clearTool && this.clearTool.stopObserving('click').remove();
    this.list && this.list.remove();
    this.input.name = this.inputName;
    this.input.removeClassName("accept-value");
  }
});
  return XWiki;
}(XWiki || {}));
