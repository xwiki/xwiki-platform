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
    'onItemAdded' : Prototype.emptyFunction
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
    this.input = element;
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
      this.clearTool = new Element('span', {'class' : 'clear-tool delete-tool invisible', 'title' : "$msg.get('core.widgets.suggestPicker.deleteAll.tooltip')"}).update("$msg.get('core.widgets.suggestPicker.deleteAll')");
      this.clearTool.observe('click', this.clearAcceptedList.bindAsEventListener(this));
      this.list.insert({'after': this.clearTool});
    }
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
    return false;
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
  },

  /**
   * Remove all the accepted items when the "remove all" button is clicked.
   */
  clearAcceptedList : function () {
    this.list.update("");
    this.notifySelectionChange();
    this.updateListTools();
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
    var input = $(this.getInputId(key));
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
    var key = suggestion.id || suggestion.value;
    var id = this.getInputId(key);
    var listItem = new Element("li");
    var displayedValue = new Element("label", {"class": "accepted-suggestion", "for" : id});
    // Insert input
    var inputOptions = {"type" : this.options.inputType, "name" : this.inputName, "id" : id, "value" : key};
    if (this.options.inputType == 'checkbox') {
      inputOptions.checked = 'checked';
    }
    var newInput = new Element("input", inputOptions);
    displayedValue.insert({'bottom' : newInput});
    // If the key should be displayed, insert it
    if (this.options.showKey) {
      displayedValue.insert({'bottom' : new Element("span", {"class": "key"}).update("[" + key.escapeHTML() + "]")});
      displayedValue.insert({'bottom' : new Element("span", {"class": "sep"}).update(" ")});
    }
    // Insert the displayed value
    displayedValue.insert({'bottom' : new Element("span", {"class": "value"}).update(suggestion.value.escapeHTML())});
    listItem.insert(displayedValue);
    // Delete tool
    if (this.options.showDeleteTool) {
      var deleteTool = new Element("span", {'class': "delete-tool", "title" : "$msg.get('core.widgets.suggestPicker.delete.tooltip')"}).update("$msg.get('core.widgets.suggestPicker.delete')");
      deleteTool.observe('click', this.removeItem);
      listItem.appendChild(deleteTool);
    }
    // Tooltip, if information exists and the options state there should be a tooltip
    if (this.options.showTooltip && suggestion.info) {
      listItem.appendChild(new Element("div", {'class' : "tooltip"}).update(suggestion.info));
    }
    this.list.insert(listItem);
    newInput.observe('change', this.checkboxChanged);
    this.options.onItemAdded(newInput);
    this.notifySelectionChange(listItem);
    this.updateListTools();
    return newInput;
  },

  /**
   * Meta-maintenance of the accepted items list: show/hide the "delete all" button, refresh the Sortable behavior.
   */
  updateListTools : function () {
    // Show/hide the "delete all" button
    if (this.clearTool) {
      if (this.list.select('li .accepted-suggestion').length > 0) {
        this.clearTool.removeClassName('invisible');
      } else {
        this.clearTool.addClassName('invisible');
      }
    }
    // Refresh the Sortable behavior to take into account the new items
    if (this.options.enableSort && this.list.select('li .accepted-suggestion').length > 0 && typeof(Sortable) != "undefined") {
      Sortable.create(this.list);
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
  }
});
  return XWiki;
}(XWiki || {}));
