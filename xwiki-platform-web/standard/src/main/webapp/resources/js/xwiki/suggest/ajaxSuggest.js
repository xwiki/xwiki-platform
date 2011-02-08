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
    // The id of the element that will hold the suggest element
    parentContainer : "body"
  },
  sInput : "",
  nInputChars : 0,
  aSuggestions : [],
  iHighlighted : 0,

  /**
   * Initialize the suggest
   *
   * @param {Object} fld the suggest field
   * @param {Object} param the options
   */
  initialize: function (fld, param){
    this.fld = $(fld);

    if (!this.fld) {
      return false;
    }

    // Clone default options from the prototype so that they are not shared and extend options with passed parameters
    this.options = Object.extend(Object.clone(this.options), param || { });

    // Reset the container if the configured parameter is not valid
    if (!$(this.options.parentContainer)) {
      this.options.parentContainer = $(document.body);
    }

    if (this.options.seps) {
      this.seps = this.options.seps;
    } else {
      this.seps = "";
    }
    // Bind the key listeners on the input field.
    this.fld.observe("keyup", this.onKeyUp.bindAsEventListener(this));
    if (Prototype.Browser.IE || Prototype.Browser.WebKit) {
      this.fld.observe("keydown", this.onKeyPress.bindAsEventListener(this));
    } else {
      this.fld.observe("keypress", this.onKeyPress.bindAsEventListener(this));
    }

    // Prevent normal browser autocomplete
    this.fld.setAttribute("autocomplete", "off");
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
    if(!$(this.suggest)) {
      // Let the key events pass through if the UI is not displayed
      return;
    }
    var key = event.keyCode;

    switch(key) {
      case Event.KEY_RETURN:
        if(this.aSuggestions.length == 1) {
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

      var pointer = this;
      clearTimeout(this.ajID);
      this.ajID = setTimeout( function() { pointer.doAjaxRequest() }, this.options.delay );
    }
    return false;
  },

  /**
   * Fire the AJAX Request that will get suggestions
   */
  doAjaxRequest: function ()
  {
    var pointer = this;

    // create ajax request
    var url = this.options.script + this.options.varname + "=" + encodeURIComponent(this.fld.value.strip());
    var method = this.options.method;
    var headers = {};
    if (this.options.json) {
      headers.Accept = "application/json";
    } else {
      headers.Accept = "application/xml";
    }

    var ajx = new Ajax.Request(url, {
      method: method,
      requestHeaders: headers,
      onSuccess: this.setSuggestions.bindAsEventListener(this),
      onFailure: function (response) {
        new XWiki.widgets.Notification("Failed to retrieve suggestions : ')" + response.statusText, "error", {timeout: 5});
      }
    });
  },

  /**
   * Set suggestions
   *
   * @param {Object} req
   */
  setSuggestions: function (req)
  {
    this.aSuggestions = [];

    if (this.options.json) {
      var jsondata = req.responseJSON;
      if (!jsondata) {
        return false;
      }
      var results = jsondata[this.options.resultsParameter];

      for (var i = 0; i < results.length; i++) {
        this.aSuggestions.push({
          'id': results[i][this.options.resultId],
          'value': results[i][this.options.resultValue],
          'info': results[i][this.options.resultInfo]
        });
      }
    } else {
      var xml = req.responseXML;

      // traverse xml
      //
      var results = xml.getElementsByTagName(this.options.resultsParameter)[0].childNodes;

      // TODO: This is incompatible with the REST search
      for (var i = 0; i < results.length; i++) {
        if (results[i].hasChildNodes()) {
          this.aSuggestions.push({
            'id': results[i].getAttribute('id'),
            'value':results[i].childNodes[0].nodeValue,
            'info':results[i].getAttribute('info')
          });
        }
      }

    }
    this.createList(this.aSuggestions);
  },

  /**
   * Create the HTML list of suggestions.
   *
   * @param {Object} arr
   */
  createList: function(arr)
  {
    var pointer = this;

    // get rid of old list
    // and clear the list removal timeout
    //
    if (this.suggest && this.suggest.parentNode) {
      this.suggest.remove();
    }
    this.killTimeout();

    // if no results, and shownoresults is false, do nothing
    if (arr.length == 0 && !this.options.shownoresults)
      return false;

    // create holding div
    //
    var div = new Element("div", { 'class': "suggestItems "+ this.options.className });

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
    for (var i=0;i<arr.length;i++)
    {
      // format output with the input enclosed in a EM element
      // (as HTML, not DOM)
      //
      var val = arr[i].value, st = val.toLowerCase().indexOf( this.sInput.toLowerCase() );
      var output = val.substring(0,st) + "<span class='highlight'>" + val.substring(st, st+this.sInput.length) + "</span>" + val.substring(st+this.sInput.length);
      var span = new Element("span").update(output);
      var valueNode = new Element('div')
            .insert(new Element('span', {'class':'suggestId'}).update(arr[i].id))
            .insert(new Element('span', {'class':'suggestValue'}).update(arr[i].value))
            .insert(new Element('span', {'class':'suggestInfo'}).update(arr[i].info));

      var item = new XWiki.widgets.XListItem( span , {
        containerClasses: 'suggestItem',
        value: valueNode,
        noHighlight: true // we do the highlighting ourselves
      });

      list.addItem(item);
    }

    // no results
    if (arr.length == 0)
    {
      list.addItem( new XWiki.widgets.XListItem(this.options.noresults, {'classes' : 'noSuggestion'}))
    }
    div.appendChild( list.getElement() );

    // get position of target textfield
    // position holding div below it
    // set width of holding div to width of field
    var pos = this.fld.cumulativeOffset();

    div.style.left = pos.left + "px";
    div.style.top = (pos.top + this.fld.offsetHeight + this.options.offsety) + "px";
    div.style.width = this.fld.offsetWidth + "px";

    // set mouseover functions for div
    // when mouse pointer leaves div, set a timeout to remove the list after an interval
    // when mouse enters div, kill the timeout so the list won't be removed
    div.onmouseover = function(){ pointer.killTimeout() }
    div.onmouseout = function(){ pointer.resetTimeout() }

    // add DIV to document
    $(this.options.parentContainer).insert(div);
    this.suggest = div;

    // currently no item is highlighted
    this.iHighlighted = 0;

    // remove list after an interval
    var pointer = this;
    this.toID = setTimeout(function () { pointer.clearSuggestions() }, this.options.timeout);
  },

  /**
   * Change highlight
   *
   * @param {Object} key
   */
  changeHighlight: function(key)
  {
    var list = this.suggest.down('ul');
    if (!list)
      return false;

    var n, elem;

    if (this.iHighlighted) {
      if (key == 40)
        elem = this.iHighlighted.next() || list.down('li');
      else if (key == 38)
        elem = this.iHighlighted.previous() || list.down('li:last-child');
    }
    else {
      if (key == 40)
        elem = list.down('li');
      else if (key == 38)
        if (list.select('li') > 0) {
          elem = list.select('li')[list.select('li').length];
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

  setHighlightedValue: function ()
  {
    if (this.iHighlighted && !this.iHighlighted.hasClassName('noSuggestion'))
    {
      if(this.sInput == "" && this.fld.value == "")
        this.sInput = this.fld.value = this.iHighlighted.down(".suggestValue").innerHTML;
      else {
        if(this.seps) {
           var lastIndx = -1;
           for(var i = 0; i < this.seps.length; i++)
             if(this.fld.value.lastIndexOf(this.seps.charAt(i)) > lastIndx)
               lastIndx = this.fld.value.lastIndexOf(this.seps.charAt(i));
            if(lastIndx == -1)
              this.sInput = this.fld.value = this.iHighlighted.down(".suggestValue").innerHTML;
            else
            {
              this.fld.value = this.fld.value.substring(0, lastIndx+1) + this.iHighlighted.down(".suggestValue").innerHTML;
               this.sInput = this.fld.value.substring(lastIndx+1);
           }
        }
        else
          this.sInput = this.fld.value = this.iHighlighted.down(".suggestValue").innerHTML;
      }

      Event.fire(this.fld, "xwiki:suggest:selected", {
        'id': this.iHighlighted.down(".suggestId").innerHTML,
        'value': this.iHighlighted.down(".suggestValue").innerHTML,
        'info': this.iHighlighted.down(".suggestInfo").innerHTML
      });
      this.fld.focus();

      /*
      // move cursor to end of input (safari)
      //
      if (this.fld.selectionStart)
        this.fld.setSelectionRange(this.sInput.length, this.sInput.length);*/

      this.clearSuggestions();

      // pass selected object to callback function, if exists

      if (typeof(this.options.callback) == "function") {
        this.options.callback( {
          'id': this.iHighlighted.down(".suggestId").innerHTML,
          'value': this.iHighlighted.down(".suggestValue").innerHTML,
          'info': this.iHighlighted.down(".suggestInfo").innerHTML
        } );
      }

      //there is a hidden input
      if(this.fld.id.indexOf("_suggest") > 0) {
        var hidden_id = this.fld.id.substring(0, this.fld.id.indexOf("_suggest"));
        var hidden_inp = $(hidden_id);

        if(hidden_inp)
           hidden_inp.value =  this.iHighlighted.down(".suggestInfo").innerHTML;
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
    var ele = $(this.suggest);
    var pointer = this;
    if (ele) {
      var fade = new Effect.Fade(ele, {duration: "0.25", afterFinish : function() {
        if($(pointer.suggest)) {
          $(pointer.suggest).remove();
          delete pointer.suggest;
        }
      }});
    }
  }

 });

 }

 return XWiki;

})(XWiki || {});
