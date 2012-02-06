var XWiki = (function(XWiki){

 var widgets = XWiki.widgets = XWiki.widgets || {};

 if (typeof widgets.XList == 'undefined') {
  if (typeof console != "undefined" && typeof console.warn == "function") {
    console.warn("[Suggest widget] Required class missing: XWiki.widgets.XList");
  }
 } else {

/**
 * Suggest class.
 * Provide value suggestions to users when starting to type in a text input.
 */
 widgets.Suggest = Class.create({
  options : {
    // The minimum number of characters after which to trigger the suggest
    minchars : 1,
    // The HTTP method for the AJAX request
    method : "get",
    // The name of the request parameter holding the input stub
    varname : "input",
    // The CSS classname of the suggest list
    className : "ajaxsuggest",
    timeout : 2500,
    delay : 500,
    offsety : 0,
    // Display a "no results" message, or simply hide the suggest box when no suggestions are available
    shownoresults : true,
    // The message to display as the "no results" message
    noresults : "No results!",
    maxheight : 250,
    cache : false,
    seps : "",
    icon : null,
    // The name of the JSON variable or XML element holding the results.
    // "results" for the old suggest, "searchResults" for the REST search.
    resultsParameter : "results",
    // The name of the JSON parameter or XML attribute holding the result identifier.
    // "id" for both the old suggest and the REST search.
    resultId : "id",
    // The name of the JSON parameter or XML attribute holding the result value.
    // "value" for the old suggest, "pageFullName" for the REST page search.
    resultValue : "value",
    // The name of the JSON parameter or XML attribute holding the result auxiliary information.
    // "info" for the old suggest, "pageFullName" for the REST search.
    resultInfo : "info",
    // The name of the JSON parameter or XML attribute holding the result icon.
    resultIcon: "icon",
    // The name of the JSON parameter or XML attribute holding a potential result hint (displayed next to the value).
    resultHint: "hint",
    // The id of the element that will hold the suggest element
    parentContainer : "body",
    // Should results fragments be highlighted when matching typed input
    highlight: true,
    // Fade the suggestion container on clear
    fadeOnClear: true,
    insertBeforeSuggestions: null,
    // Should value be displayed as a hint
    displayValue: false,
    // Display value prefix text
    displayValueText: "Value :",
    // How to align the suggestion list when its with is different than the input field width
    align: "left",
    // When there are several suggest sources, should the widget displays only one, unified, "loading" indicator for all requests undergoing,
    // Or should it displays one loading indicator per request next to the corresponding source.
    unifiedLoader: false,
    // The DOM node to use to display the loading indicator when in mode unified loader (it will receive a "loading" class name for the time of the loading)
    // Default is null, which falls back on the input itself. This option is used only when unifiedLoader is true.
    loaderNode: null
  },
  sInput : "",
  nInputChars : 0,
  aSuggestions : [],
  iHighlighted : null,
  isActive : false,

  /**
   * Initialize the suggest
   *
   * @param {Object} fld the suggest field
   * @param {Object} param the options
   */
  initialize: function (fld, param){

    if (!fld) {
      return false;
    }
    this.setInputField(fld);

    // Clone default options from the prototype so that they are not shared and extend options with passed parameters
    this.options = Object.extend(Object.clone(this.options), param || { });
    if (typeof this.options.sources == 'object' && this.options.sources.length > 1) {
      // We are in multi-sources mode
      this.sources = this.options.sources;
    } else {
      // We are in mono-source mode
      this.sources = this.options;
    }

    // Flatten sources
    this.sources = [ this.sources ].flatten().compact();

    // Reset the container if the configured parameter is not valid
    if (!$(this.options.parentContainer)) {
      this.options.parentContainer = $(document.body);
    }

    if (this.options.seps) {
      this.seps = this.options.seps;
    } else {
      this.seps = "";
    }

    // Initialize a request number that will keep track of the latest request being fired.
    // This will help to discard potential non-last requests callbacks ; this in order to have better performance
    // (less unneccessary DOM manipulation, and less unneccessary highlighting computation).
    this.latestRequest = 0;

  },

  /**
   * Sets or replace the input field associated with this suggest.
   */
  setInputField: function(input){
    this.detach();
    this.fld = $(input);
    if (this.fld.__x_suggest) {
      this.fld.__x_suggest.detach();
    }
    this.fld.__x_suggest = this;
    // Bind the key listeners on the input field.
    this.onKeyUp = this.onKeyUp.bindAsEventListener(this);
    this.fld.observe("keyup", this.onKeyUp);
    this.onKeyPress = this.onKeyPress.bindAsEventListener(this);
    if (Prototype.Browser.IE || Prototype.Browser.WebKit) {
      this.fld.observe("keydown", this.onKeyPress);
    } else {
      this.fld.observe("keypress", this.onKeyPress);
    }

    // Prevent normal browser autocomplete
    this.fld.setAttribute("autocomplete", "off");

    this.fld.observe("blur", function(event){
      // Make sure any running request will be dropped after the input field has been left
      this.latestRequest++;

    }.bind(this));
  },

  /**
   * Treats normal characters and triggers the autocompletion behavior. This is needed since the field value is not
   * updated when keydown/keypress are called, so the suggest would work with the previous value. The disadvantage is
   * that keyUp is not fired for each stroke in a long keypress, but only once at the end. This is not a real problem,
   * though.
   */
  onKeyUp: function(event)
  {
    var key = event.keyCode;
    switch(key) {
      // Ignore special keys, which are treated in onKeyPress
      case Event.KEY_RETURN:
      case Event.KEY_ESC:
      case Event.KEY_UP:
      case Event.KEY_DOWN:
        break;
      default: {
        // If there are separators in the input string, get suggestions only for the text after the last separator
        // TODO The user might be typing in the middle of the field, not in the last item. Do a better detection by
        // comparing the new value with the old one.
        if(this.seps) {
          var lastIndx = -1;
          for(var i = 0; i < this.seps.length; i++) {
            if(this.fld.value.lastIndexOf(this.seps.charAt(i)) > lastIndx) {
              lastIndx = this.fld.value.lastIndexOf(this.seps.charAt(i));
            }
          }
          if(lastIndx == -1) {
            this.getSuggestions(this.fld.value);
          } else {
            this.getSuggestions(this.fld.value.substring(lastIndx+1));
          }
        } else {
          this.getSuggestions(this.fld.value);
        }
      }
    }
  },
  /**
   * Treats Up and Down arrows, Enter and Escape, affecting the UI meta-behavior. Enter puts the currently selected
   * value inside the target field, Escape closes the suggest dropdown, Up and Down move the current selection.
   */
  onKeyPress: function(event) {
    if(!$(this.isActive)) {
      // Let the key events pass through if the UI is not displayed
      return;
    }
    var key = event.keyCode;

    switch(key) {
      case Event.KEY_RETURN:
        if (this.aSuggestions.length == 1) {
          this.highlightFirst();
        }
        this.setHighlightedValue();
        Event.stop(event);
        break;
      case Event.KEY_ESC:
        this.clearSuggestions();
        Event.stop(event);
        break;
      case Event.KEY_UP:
        this.changeHighlight(key);
        Event.stop(event);
        break;
      case Event.KEY_DOWN:
        this.changeHighlight(key);
        Event.stop(event);
        break;
      default:
        break;
    }
  },

  /**
   * Get suggestions
   *
   * @param {Object} val the value to get suggestions for
   */
  getSuggestions: function (val)
  {
    // if input stays the same, do nothing
    //
    val = val.strip();
    if (val == this.sInput) {
      return false;
    }

    // input length is less than the min required to trigger a request
    // reset input string
    // do nothing
    //
    if (val.length < this.options.minchars) {
      this.sInput = "";
      return false;
    }

    // if caching enabled, and user is typing (ie. length of input is increasing)
    // filter results out of aSuggestions from last request
    //
    if (val.length>this.nInputChars && this.aSuggestions.length && this.options.cache)
    {
      var arr = [];
      for (var i=0;i<this.aSuggestions.length;i++) {
        if (this.aSuggestions[i].value.substr(0,val.length).toLowerCase() == val.toLowerCase()) {
          arr.push( this.aSuggestions[i] );
        }
      }

      this.sInput = val;
      this.nInputChars = val.length;
      this.aSuggestions = arr;

      this.createList(this.aSuggestions);

      return false;
    } else  {
      // do new request
      this.sInput = val;
      this.nInputChars = val.length;

      this.prepareContainer();

      this.latestRequest++;
      var pointer = this;
      var requestId = this.latestRequest;
      clearTimeout(this.ajID);
      this.ajID = setTimeout( function() { pointer.doAjaxRequests(requestId) }, this.options.delay );

    }
    return false;
  },

  /**
   * Fire the AJAX Request(s) that will get suggestions
   */
  doAjaxRequests: function (requestId)
  {
    if (this.fld.value.length < this.options.minchars) {
      return;
    }

    for (var i=0;i<this.sources.length;i++) {
      var source = this.sources[i];

      // create ajax request
      var url = source.script + source.varname + "=" + encodeURIComponent(this.fld.value.strip());
      var method = source.method || "get";
      var headers = {};
      if (source.json) {
        headers.Accept = "application/json";
      } else {
        headers.Accept = "application/xml";
      }

      var ajx = new Ajax.Request(url, {
        method: method,
        requestHeaders: headers,
        onSuccess: this.setSuggestions.bindAsEventListener(this, source, requestId),
        onFailure: function (response) {
          new XWiki.widgets.Notification("Failed to retrieve suggestions : ')" + response.statusText, "error", {timeout: 5});
        }
      });
    }
  },

  /**
   * Set suggestions
   *
   * @param {Object} req
   * @param {Object} source
   * @param {Number} requestId the identifier of the request for which this callback is triggered.
   */
  setSuggestions: function (req, source, requestId)
  {

    // If there has been one or several requests fired in the mean time (between the time the request for which this callback
    // has been triggered and the time of the callback itself) ; we don't do anything and leave it to following callbacks to
    // set potential suggestions
    if (requestId < this.latestRequest) {
      return;
    }

    this.aSuggestions = [];

    if (source.json) {
      var jsondata = req.responseJSON;
      if (!jsondata) {
        return false;
      }
      var results = jsondata[source.resultsParameter || this.options.resultsParameter];

      for (var i = 0; i < results.length; i++) {
        this.aSuggestions.push({
           'id': results[i][source.resultId || this.options.resultId],
           'value': results[i][source.resultValue || this.options.resultValue],
           'info': results[i][source.resultInfo || this.options.resultInfo],
           'icon' : results[i][source.resultIcon || this.options.resultIcon],
           'hint' : results[i][source.resultHint || this.options.resultHint]
        });
      }
    } else {
      var xml = req.responseXML;

      // traverse xml
      //
      var results = xml.getElementsByTagName(source.resultsParameter || this.options.resultsParameter)[0].childNodes;

      // TODO: This is incompatible with the REST search
      for (var i = 0; i < results.length; i++) {
        if (results[i].hasChildNodes()) {
          this.aSuggestions.push({
            'id': results[i].getAttribute('id'),
            'value':results[i].childNodes[0].nodeValue,
            'info':results[i].getAttribute('info'),
            'icon':results[i].getAttribute('icon'),
            'hint':results[i].getAttribute('hint')
          });
        }
      }

    }
    this.createList(this.aSuggestions, source);
  },

  /**
   * Creates the container that will hold one or multiple source results.
   */
  prepareContainer: function(){

    if (!$(this.options.parentContainer).down('.suggestItems')) {
      // If the suggestion top container is not in the DOM already, we create it and inject it

      var div = new Element("div", { 'class': "suggestItems "+ this.options.className });

      // Get position of target textfield
      var pos = this.fld.cumulativeOffset();

      // Container width is passed as an option, or field width if no width provided.
      // The 2px substracted correspond to one pixel of border on each side of the field,
      // this allows to have the suggestion box borders well aligned with the field borders.
      // FIXME this should be computed instead, since border might not always be 1px.
      var containerWidth = this.options.width ? this.options.width : (this.fld.offsetWidth - 2)

      if (this.options.align == 'left') {
        // Align the box on the left
        div.style.left = pos.left + "px";
      } else if (this.options.align == "center") {
        // Align the box to the center
        div.style.left = pos.left + (this.fld.getWidth() - containerWidth - 2) / 2 + "px";
      } else {
        // Align the box on the right.
        // This has a visible effect only when the container width is not the same as the input width
        div.style.left = (pos.left - containerWidth + this.fld.offsetWidth - 2) + "px";
      }

      div.style.top = (pos.top + this.fld.offsetHeight + this.options.offsety) + "px";
      div.style.width = containerWidth + "px";

      // set mouseover functions for div
      // when mouse pointer leaves div, set a timeout to remove the list after an interval
      // when mouse enters div, kill the timeout so the list won't be removed
      var pointer = this;
      div.onmouseover = function(){ pointer.killTimeout() }
      div.onmouseout = function(){ pointer.resetTimeout() }

      this.resultContainer = new Element("div", {'class':'resultContainer'});
      div.appendChild(this.resultContainer);

      // add DIV to document
      $(this.options.parentContainer).insert(div);

      this.container = div;

      if (this.options.insertBeforeSuggestions) {
        this.resultContainer.insert(this.options.insertBeforeSuggestions);
      }

      document.fire("xwiki:suggest:containerCreated", {
        'container' : this.container,
        'suggest' : this
      });
    }

    if (this.sources.length > 1) {
      // If we are in multi-source mode, we need to prepare a sub-container for each of the suggestion source
      for (var i=0;i<this.sources.length;i++) {

        var source = this.sources[i];
        source.id = i

        if(this.resultContainer.down('.results' + source.id)) {
          // If the sub-container for this source is already present, we just re-initialize it :
          // - remove its content
          // - set it as loading
          if (this.resultContainer.down('.results' + source.id).down('ul')) {
            this.resultContainer.down('.results' + source.id).down('ul').remove();
          }
          if (!this.options.unifiedLoader) {
            this.resultContainer.down('.results' + source.id).down('.sourceContent').addClassName('loading');
          }
          else {
            (this.options.loaderNode || this.fld).addClassName("loading");
            this.resultContainer.down('.results' + source.id).addClassName('hidden loading');
          }
        }
        else {
          // The sub-container for this source has not been created yet
          // Really create the subcontainer for this source and inject it in the global container
          var sourceContainer = new Element('div', {'class' : 'results results' + source.id}),
              sourceHeader = new Element('div', {'class':'sourceName'});

          if (this.options.unifiedLoader) {
            sourceContainer.addClassName('hidden loading');
          }

          if (typeof source.icon != 'undefined') {
            // If there is an icon for this source group, set it as background image
            var iconImage = new Image();
            iconImage.onload = function(){
              this.sourceHeader.setStyle({
                backgroundImage: "url(" + this.iconImage.src + ")"
              });
              this.sourceHeader.setStyle({
                textIndent:(this.iconImage.width + 6) + 'px'
              });
            }.bind({
              sourceHeader:sourceHeader,
              iconImage:iconImage
            });
            iconImage.src = source.icon;
          }
          sourceHeader.insert(source.name)
          sourceContainer.insert( sourceHeader );
          var classes = "sourceContent " + (this.options.unifiedLoader ? "" : "loading");
          sourceContainer.insert( new Element('div', {'class':classes}));

          if (typeof source.before !== 'undefined') {
            this.resultContainer.insert(source.before);
          }
          this.resultContainer.insert(sourceContainer);
          if (typeof source.after !== 'undefined') {
            this.resultContainer.insert(source.after);
          }
        }
      }
    } else {
      // In mono-source mode, reset the list if present
      if (this.resultContainer.down("ul")) {
        this.resultContainer.down("ul").remove();
      }
    }

    var ev = this.container.fire("xwiki:suggest:containerPrepared", {
      'container' : this.container,
      'suggest' : this
    });

    return this.container;
  },

  /**
   * Create the HTML list of suggestions.
   *
   * @param {Object} arr
   * @param {Object} source the source for data for which to create this list of results.
   */
  createList: function(arr, source)
  {
    this.isActive = true;
    var pointer = this;

    this.killTimeout();

    // create holding div
    //
    if (this.sources.length > 1) {
      var div = this.resultContainer.down(".results" + source.id);
      if (arr.length > 0 || this.options.shownoresults) {
        div.down('.sourceContent').removeClassName('loading');
        this.resultContainer.down(".results" + source.id).removeClassName("hidden loading");
      }

      // If we are in mode "unified loader" (showing one loading indicator for all requests and not one per request)
      // and there aren't any source still loading, we remove the unified loading status.
      if (this.options.unifiedLoader && !this.resultContainer.down("loading")) {
        (this.options.loaderNode || this.fld).removeClassName("loading");
      }
    }
    else {
      var div = this.resultContainer;
    }

    // if no results, and shownoresults is false, go no further
    if (arr.length == 0 && !this.options.shownoresults) {
      return false;
    }

    // Ensure any previous list of results for this source gets removed
    if (div.down('ul')) {
      div.down('ul').remove();
    }

    // create and populate list
    var list = new XWiki.widgets.XList([], {
       icon: this.options.icon,
       classes: 'suggestList',
       eventListeners: {
          'click' : function () { pointer.setHighlightedValue(); return false; },
          'mouseover' : function () { pointer.setHighlight( this.getElement() ); }
       }
    });

    // loop throught arr of suggestions
    // creating an XlistItem for each suggestion
    //
    for (var i=0,len=arr.length;i<len;i++)
    {
	  // Output is either emphasized or row value depending on source option
      var output = source.highlight ? this.emphasizeMatches(this.sInput, arr[i].value) : arr[i].value;
      if (arr[i].hint) {
        output += "<span class='hint'>" + arr[i].hint + "</span>";
      }

      if (!this.options.displayValue) {
        var displayNode = new Element("span", {'class':'info'}).update(output);
      }
      else {
        var displayNode = new Element("div").insert(new Element('div', {'class':'value'}).update(output))
                                            .insert(new Element('div', {'class':'info'}).update(
                                              "<span class='legend'>" + this.options.displayValueText + "</span>" + arr[i].info)
                                            );
      }

      // If the search result contains an icon information, we insert this icon in the result entry.
      if (arr[i].icon) {
        var iconImage = new Element("img", {'src' : arr[i].icon, 'class' : 'icon' });
        displayNode.insert({top: iconImage});
      }

      var valueNode = new Element('div')
            .insert(new Element('span', {'class':'suggestId'}).update(arr[i].id))
            .insert(new Element('span', {'class':'suggestValue'}).update(arr[i].value))
            .insert(new Element('span', {'class':'suggestInfo'}).update(arr[i].info));

      var item = new XWiki.widgets.XListItem( displayNode , {
        containerClasses: 'suggestItem',
        value: valueNode,
        noHighlight: true // we do the highlighting ourselves
      });

      list.addItem(item);
    }

    // no results
    if (arr.length == 0)
    {
      list.addItem( new XWiki.widgets.XListItem(this.options.noresults, {
                          'classes' : 'noSuggestion',
                          noHighlight :true }) );
    }
    div.appendChild( list.getElement() );

    this.suggest = div;

    // remove list after an interval
    var pointer = this;
    if (this.options.timeout > 0) {
      this.toID = setTimeout(function () { pointer.clearSuggestions() }, this.options.timeout);
    }
  },

  /**
   * Emphesize the elements in passed value that matches one of the words typed as input by the user.
   *
   * @param String input the (typed) input
   * @param String value the value to emphasize
   */
  emphasizeMatches:function(input, value)
  {
    // If the source declares that results are matching, we highlight them in the value
    var output = value,
        // Separate words (called fragments hereafter) in user input
        fragments = input.split(' ').uniq().compact(),
        offset = 0,
        matches = {};

    for (var j=0,flen=fragments.length;j<flen;j++) {
      // We iterate over each fragments, and try to find one or several matches in this suggestion
      // item display value.
      var index = output.toLowerCase().indexOf(fragments[j].toLowerCase());
      while (index >= 0) {
        // As long as we have matches, we store their index and replace them in the output string with the space char
        // so that they don't get matched for ever.
        // Note that the space char is the only one safe to use, as it cannot be part of a fragment.
        var match = output.substring(index, index + fragments[j].length),
            placeholder = "";
        fragments[j].length.times(function(){
          placeholder += " ";
        });
        matches[index] = match;
        output = output.substring(0, index) + placeholder + output.substring(index + fragments[j].length);
        index = output.toLowerCase().indexOf(fragments[j].toLowerCase());
      }
    }
    // Now that we have found all matches for all possible fragments, we iterate over them
    // to construct the final "output String" that will be injected as a suggestion item,
    // with all matches emphasized
    Object.keys(matches).sortBy(function(s){return parseInt(s)}).each(function(key){
      var before = output.substring(0, parseInt(key) + offset);
      var after = output.substring(parseInt(key) + matches[key].length + offset);
      // Emphasize the match in the output string that will be displayed
      output = before + "<em>" + matches[key] + "</em>" + after;
      // Increase the offset by 9, which correspond to the number of chars in the opening and closing "em" tags
      // we have introduced for this match in the output String
      offset += 9;
    });

    return output;
  },

  /**
   * Change highlight
   *
   * @param {Object} key
   */
  changeHighlight: function(key)
  {
    var list = this.resultContainer;
    if (!list)
      return false;

    var n, elem;

    if (this.iHighlighted) {
      // If there is already a highlighted element, we look for the next or previous highlightable item in the list
      // of results, according to which key has been pressed.
      if (key == Event.KEY_DOWN) {
        elem = this.iHighlighted.next();
        if (!elem && this.iHighlighted.up('div.results')) {
          // if the next item could not be found and multi-source mode, find the next not empty source
          var source = this.iHighlighted.up('div.results').next();
          while (source && !elem) {
            elem = source.down('li');
            source = source.next();
          }
        }
        if(!elem) {
          elem = list.down('li');
        }
      }
      else if (key == Event.KEY_UP) {
        elem = this.iHighlighted.previous();
        if (!elem && this.iHighlighted.up('div.results')) {
          // if the previous item could not be found and multi-source mode, find the previous not empty source
          var source = this.iHighlighted.up('div.results').previous();
          while(source && !elem) {
            elem = source.down('li:last-child');
            source = source.previous();
          }
        }
        if (!elem) {
          elem =  list.select('ul')[list.select('ul').length - 1].down('li:last-child');
        }
      }
    }
    else {
      // No item is highlighted yet, so we just look for the first or last highlightable item,
      // according to which key, up or down, has been pressed.
      if (key == Event.KEY_DOWN) {
        if (list.down('div.results')) {
          elem = list.down('div.results').down('li')
        }
        else {
          elem = list.down('li');
        }
      }
      else if (key == Event.KEY_UP)
        if (list.select('li') > 0) {
          elem = list.select('li')[list.select('li').length - 1];
        }
    }

    if (elem) {
      this.setHighlight(elem);
    }
  },

  /**
   * Set highlight
   *
   * @param {Object} n
   */
  setHighlight: function(highlightedItem)
  {
    if (this.iHighlighted)
      this.clearHighlight();

    highlightedItem.addClassName("xhighlight");

    this.iHighlighted = highlightedItem;

    this.killTimeout();
  },

  /**
   * Clear highlight
   */
  clearHighlight: function()
  {
    if (this.iHighlighted) {
      this.iHighlighted.removeClassName("xhighlight");
      delete this.iHighlighted;
    }
  },

  highlightFirst: function()
  {
    if (this.suggest && this.suggest.down('ul')) {
      var first = this.suggest.down('ul').down('li');
      if (first) {
        this.setHighlight(first);
      }
    }
  },

  /**
   * return true if a suggestion is highlighted, false otherwise
   */
  hasActiveSelection: function(){
    return this.iHighlighted;
  },

  setHighlightedValue: function ()
  {
    if (this.iHighlighted && !this.iHighlighted.hasClassName('noSuggestion'))
    {
      var selection, newFieldValue
      if(this.sInput == "" && this.fld.value == "")
        selection = newFieldValue = this.iHighlighted.down(".suggestValue").innerHTML;
      else {
        if(this.seps) {
           var lastIndx = -1;
           for(var i = 0; i < this.seps.length; i++)
             if(this.fld.value.lastIndexOf(this.seps.charAt(i)) > lastIndx)
               lastIndx = this.fld.value.lastIndexOf(this.seps.charAt(i));
            if(lastIndx == -1)
              selection = newFieldValue = this.iHighlighted.down(".suggestValue").innerHTML;
            else
            {
               newFieldValue = this.fld.value.substring(0, lastIndx+1) + this.iHighlighted.down(".suggestValue").innerHTML;
               selection = newFieldValue.substring(lastIndx+1);
           }
        }
        else
          selection = newFieldValue = this.iHighlighted.down(".suggestValue").innerHTML;
      }

      var event = Event.fire(this.fld, "xwiki:suggest:selected", {
        'suggest' : this,
        'id': this.iHighlighted.down(".suggestId").innerHTML,
        'value': this.iHighlighted.down(".suggestValue").innerHTML,
        'info': this.iHighlighted.down(".suggestInfo").innerHTML,
        'icon' : this.iHighlighted.down('img.icon') ? this.iHighlighted.down('img.icon').src : ''
      });

      if (!event.stopped) {
        this.sInput = selection;
        this.fld.value = newFieldValue;
        this.fld.focus();
        this.clearSuggestions();

        // pass selected object to callback function, if exists
        if (typeof(this.options.callback) == "function") {
          this.options.callback({
            'id': this.iHighlighted.down(".suggestId").innerHTML,
            'value': this.iHighlighted.down(".suggestValue").innerHTML,
            'info': this.iHighlighted.down(".suggestInfo").innerHTML
          });
        }

        //there is a hidden input
        if(this.fld.id.indexOf("_suggest") > 0) {
          var hidden_id = this.fld.id.substring(0, this.fld.id.indexOf("_suggest"));
          var hidden_inp = $(hidden_id);
          if (hidden_inp) {
            hidden_inp.value =  this.iHighlighted.down(".suggestInfo").innerHTML;
          }
        }
      }
    }
  },

  /**
   * Kill timeout
   */
  killTimeout: function()
  {
    clearTimeout(this.toID);
  },

  /**
   * Reset timeout
   */
  resetTimeout: function()
  {
    clearTimeout(this.toID);
    var pointer = this;
    this.toID = setTimeout(function () { pointer.clearSuggestions() }, 1000);
  },

  /**
   * Clear suggestions
   */
  clearSuggestions: function() {
    this.killTimeout();
    this.isActive = false;
    var ele = $(this.container);
    var pointer = this;
    if (ele && ele.parentNode) {
      if (this.options.fadeOnClear) {
        var fade = new Effect.Fade(ele, {duration: "0.25", afterFinish : function() {
          if($(pointer.container)) {
            $(pointer.container).remove();
          }
        }});
      }
      else {
        $(this.container).remove();
      }
      document.fire("xwiki:suggest:clearSuggestions", { 'suggest' : this});
    }
  },

  /**
   * Remove suggest behavior from the target field (detach all listeners and hide the suggest if active)
   */
  detach : function() {
    if (this.fld) {
      Event.stopObserving(this.fld, "keyup", this.onKeyUp);
      if (Prototype.Browser.IE || Prototype.Browser.WebKit) {
        Event.stopObserving(this.fld, "keydown", this.onKeyPress);
      } else {
        Event.stopObserving(this.fld, "keypress", this.onKeyPress);
      }
      this.clearSuggestions();
      this.fld.__x_suggest = null;
      this.fld.setAttribute("autocomplete", "on");
    }
  }

 });

 }

 return XWiki;

})(XWiki || {});
