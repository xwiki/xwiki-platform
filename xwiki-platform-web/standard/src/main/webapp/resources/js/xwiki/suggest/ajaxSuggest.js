// Make sure the XWiki 'namespace' exists.
if(typeof XWiki == "undefined") {
  XWiki = new Object();
}
// Make sure the widgets 'namespace' exists.
if(typeof(XWiki.widgets) == 'undefined') {
  XWiki.widgets = new Object();
}

var useXWKns;

if (useXWKns) {
  if (typeof _xwk == "undefined") {
    // This is temporary, until we move all backward compatibility into a compability.js file
    // For this, all calls referencing this old _xwk namespace have first to be cleaned from XE default webapp/XAR.
    _xwk = new Object();
  }
} else {
  _xwk = this;
}

// Same, this is temporary until the clean is finished.
// see http://jira.xwiki.org/jira/browse/XWIKI-3655
_xwk.ajaxSuggest =

/**
 * Suggest class.
 * Provide value suggestions to users when starting to type in a text input.
 */
XWiki.widgets.Suggest = Class.create({
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
    offsety : -5,
    // Display a "no results" message, or simply hide the suggest box when no suggestions are available
    shownoresults : true,
    // The message to display as the "no results" message
    noresults : "No results!",
    maxheight : 250,
    cache : false,
    seps : "",
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

    // parameters object
    Object.extend(this.options, param || { });

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
    if (Prototype.Browser.IE) {
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
    if(!$(this.idAs)) {
      // Let the key events pass through if the UI is not displayed
      return;
    }
    var key = event.keyCode;

    switch(key) {
      case Event.KEY_RETURN:
        if(this.aSuggestions.length == 1) {
          this.setHighlight(1);
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
    var url = this.options.script + this.options.varname + "=" + escape(this.fld.value);
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
        alert("AJAX error: " + response.statusText);
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

    this.idAs = "as_"+this.fld.id;
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
    if ($(this.idAs)) {
      $(this.idAs).remove();
    }
    this.killTimeout();

    // if no results, and shownoresults is false, do nothing
    if (arr.length == 0 && !this.options.shownoresults)
      return false;

    // create holding div
    //
    var div = new Element("div", {
      id: this.idAs,
      className: this.options.className
    });

    var hcorner = new Element("div", {className: "as_corner"});
    var hbar = new Element("div", {className: "as_bar"});
    var header = new Element("div", {className: "as_header"});
    header.appendChild(hcorner);
    header.appendChild(hbar);
    div.appendChild(header);

    // create and populate ul
    var ul = new Element("ul", {id: "as_ul"});

    // loop throught arr of suggestions
    // creating an LI element for each suggestion
    //
    for (var i=0;i<arr.length;i++)
    {
      // format output with the input enclosed in a EM element
      // (as HTML, not DOM)
      //
      var val = arr[i].value;
      var st = val.toLowerCase().indexOf( this.sInput.toLowerCase() );
      var output = val.substring(0,st) + "<em>" + val.substring(st, st+this.sInput.length) + "</em>" + val.substring(st+this.sInput.length);

      var span = new Element("span").update(output);

      var a = new Element("a", {href: "#"});

      var tl = new Element("span", {className:"tl"}).update(" ");
      var tr = new Element("span", {className:"tr"}).update(" ");
      a.appendChild(tl);
      a.appendChild(tr);

      a.appendChild(span);

      a.name = i+1;
      a.onclick = function () { pointer.setHighlightedValue(); return false; }
      a.onmouseover = function () { pointer.setHighlight(this.name); }

      var li   = new Element("li").update(a);

      ul.appendChild( li );
    }

    // no results
    if (arr.length == 0)
    {
      var li = new Element("li", {className:"as_warning"}).update(this.options.noresults);

      ul.appendChild( li );
    }
    div.appendChild( ul );

    var fcorner = new Element("div", {className: "as_corner"});
    var fbar = new Element("div", {className: "as_bar"});
    var footer = new Element("div", {className: "as_footer"});
    footer.appendChild(fcorner);
    footer.appendChild(fbar);
    div.appendChild(footer);

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
    $(this.options.parentContainer).appendChild(div);

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
    var list = $("as_ul");
    if (!list)
      return false;

    var n;

    if (key == 40)
      n = this.iHighlighted + 1;
    else if (key == 38)
      n = this.iHighlighted - 1;

    if (n > list.childNodes.length)
      n = list.childNodes.length;
    if (n < 1)
      n = 1;

    this.setHighlight(n);
  },

  /**
   * Set highlight
   *
   * @param {Object} n
   */
  setHighlight: function(n)
  {
    var list = $("as_ul");
    if (!list)
      return false;

    if (this.iHighlighted > 0)
      this.clearHighlight();

    this.iHighlighted = Number(n);

    list.childNodes[this.iHighlighted-1].className = "as_highlight";

    this.killTimeout();
  },

  /**
   * Clear highlight
   */
  clearHighlight: function()
  {
    var list = $("as_ul");
    if (!list)
      return false;

    if (this.iHighlighted > 0)
    {
      list.childNodes[this.iHighlighted-1].className = "";
      this.iHighlighted = 0;
    }
  },

  setHighlightedValue: function ()
  {
    if (this.iHighlighted)
    {
      if(this.sInput == "" && this.fld.value == "")
        this.sInput = this.fld.value = this.aSuggestions[ this.iHighlighted-1 ].value;
      else {
        if(this.seps) {
           var lastIndx = -1;
           for(var i = 0; i < this.seps.length; i++)
             if(this.fld.value.lastIndexOf(this.seps.charAt(i)) > lastIndx)
               lastIndx = this.fld.value.lastIndexOf(this.seps.charAt(i));
            if(lastIndx == -1)
              this.sInput = this.fld.value = this.aSuggestions[ this.iHighlighted-1 ].value;
            else
            {
              this.fld.value = this.fld.value.substring(0, lastIndx+1) + this.aSuggestions[ this.iHighlighted-1 ].value;
               this.sInput = this.fld.value.substring(lastIndx+1);
           }
        }
        else
          this.sInput = this.fld.value = this.aSuggestions[ this.iHighlighted-1 ].value;
      }

      this.fld.focus();

      /*
      // move cursor to end of input (safari)
      //
      if (this.fld.selectionStart)
        this.fld.setSelectionRange(this.sInput.length, this.sInput.length);*/

      this.clearSuggestions();

      // pass selected object to callback function, if exists

      if (typeof(this.options.callback) == "function") {
        this.options.callback( this.aSuggestions[this.iHighlighted-1] );
      }

      //there is a hidden input
      if(this.fld.id.indexOf("_suggest") > 0) {
        var hidden_id = this.fld.id.substring(0, this.fld.id.indexOf("_suggest"));
        var hidden_inp = $(hidden_id);

        if(hidden_inp)
           hidden_inp.value = this.aSuggestions[ this.iHighlighted-1 ].info;
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
    var ele = $(this.idAs);
    var pointer = this;
    if (ele) {
      var fade = new Effect.Fade(ele, {duration: "0.25", afterFinish : function() {
        if($(pointer.idAs)) {
          $(pointer.idAs).remove();
        }
      }});
    }
  }

});
