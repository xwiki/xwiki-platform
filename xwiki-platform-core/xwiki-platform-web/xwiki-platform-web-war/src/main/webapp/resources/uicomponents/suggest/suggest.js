/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
    noresults : "$services.localization.render('core.widgets.suggest.noResults')",
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
    // Indicates if the auxiliary information contains HTML code or node.
    resultInfoHTML : false,
    // The name of the JSON parameter or XML attribute holding the result icon.
    resultIcon: "icon",
    // The name of the JSON parameter or XML attribute holding a potential result hint (displayed next to the value).
    resultHint: "hint",
    // The name of the JSON field or XML attribute holding the result type. The value of the specified field/attribute is
    // used as a CSS class name. This is useful if you need to style suggestions differently based on some property.
    resultType: "type",
    // The name of the JSON field or XML attribute holding the result URL.
    resultURL: 'url',
    // The id of the element that will hold the suggest element
    parentContainer : "body",
    // Should results fragments be highlighted when matching typed input
    highlight: true,
    // Fade the suggestion container on clear
    fadeOnClear: true,
    // Show a 'hide suggestions' button
    hideButton: {
      positions: [ "top" ],
      text: "$escapetool.javascript($services.localization.render('core.widgets.suggest.hide'))"
    },
    insertBeforeSuggestions: null,
    // Should value be displayed as a hint
    displayValue: false,
    // Display value prefix text
    displayValueText: "$services.localization.render('core.widgets.suggest.valuePrefix')",
    // How to align the suggestion list when its width is different from the input field width
    align: "left",
    // When there are several suggest sources, should the widget displays only one, unified, "loading" indicator for all requests undergoing,
    // Or should it displays one loading indicator per request next to the corresponding source.
    unifiedLoader: false,
    // The DOM node to use to display the loading indicator when in mode unified loader (it will receive a "loading" class name for the time of the loading)
    // Default is null, which falls back on the input itself. This option is used only when unifiedLoader is true.
    loaderNode: null,
    // A list of key codes for which to propagate the keyboard event.
    // Useful when another keyboard event listener exists on the input field, even if it may be registered at a diferent level.
    // By default, the handled key events do not propagate, the rest do. See #onKeyPress
    propagateEventKeyCodes : []
  },
  sInput : "",
  nInputChars : 0,
  aSuggestions : {},
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
    if (typeof this.options.sources == 'object') {
      // We are in multi-source mode. The display is different in this mode even if there is only one source. We need to
      // set a flag to know that we are in this mode because we flatten the list of sources below.
      this.isInMultiSourceMode = true;
      this.sources = this.options.sources;
    } else {
      // We are in mono-source mode
      this.sources = this.options;
    }

    // Flatten sources
    this.sources = [ this.sources ].flatten().compact();

    if (this.sources.length == 0) {
      // We still need an empty (fake) source so that we display at least the 'No results' message.
      this.sources.push({
        script: function(value, callback) {callback([])}
      });
    }

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
    if (Prototype.Browser.IE || Prototype.Browser.WebKit || browser.isIE11up) {
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
    var checkPropagation = true;

    switch(key) {
      case Event.KEY_RETURN:
        if (!this.iHighlighted && (Object.keys(this.aSuggestions).length == 1 && this.aSuggestions[Object.keys(this.aSuggestions)[0]].length == 1)) {
          this.highlightFirst();
        }
        this.setHighlightedValue(event);
        break;
      case Event.KEY_ESC:
        this.clearSuggestions();
        break;
      case Event.KEY_UP:
        this.changeHighlight(key);
        break;
      case Event.KEY_DOWN:
        this.changeHighlight(key);
        break;
      default:
        checkPropagation = false;
        break;
    }

    // Stop propagation for the keys we have handled, unless otherwise specified in the options.
    if (checkPropagation && this.options.propagateEventKeyCodes && this.options.propagateEventKeyCodes.indexOf(key) == -1) {
      Event.stop(event);
    }
  },

  /**
   * Get suggestions
   *
   * @param {Object} val the value to get suggestions for
   */
  getSuggestions: function (val)
  {
    // If input stays the same, do nothing.
    val = val.strip().toLowerCase();
    if (val == this.sInput) {
      return false;
    }

    // Input length is less than the min required to trigger a request.
    // Reset input string, hide the suggestions (if any), and return.
    if (val.length < this.options.minchars) {
      this.sInput = "";
      this.clearSuggestions();
      return false;
    }

    // if caching enabled, and user is typing (ie. length of input is increasing)
    // filter results out of aSuggestions from last request
    //
    if (val.length>this.nInputChars && Object.keys(this.aSuggestions).length && this.options.cache)
    {
      var filteredSuggestions = {};
      for (var i=0; i < Object.keys(this.aSuggestions).length; i++) {
        var sourceId = Object.keys(this.aSuggestions)[i];
        var filteredSourceSuggestions = [];
        for (var j=0; j<this.aSuggestions[sourceId].length; j++) {
          var existingSuggestion = this.aSuggestions[sourceId][j];
          // Note: This is assuming that all suggestions are prefixed with the value. Does not apply in all cases, so
          // the use of options.cache is limited to only those scenarios.
          if (existingSuggestion.value.substr(0, val.length).toLowerCase() == val) {
            filteredSourceSuggestions.push(existingSuggestion);
          }
        }

        // Only set this source if it has at least one suggestion.
        if (filteredSourceSuggestions.length) {
          filteredSuggestions[sourceId] = filteredSourceSuggestions;
        }
      }

      this.sInput = val;
      this.nInputChars = val.length;
      this.aSuggestions = filteredSuggestions;

      // Display the just filtered suggestions.
      for (var i=0; i < sources.length; i++) {
        var source = sources[i];
        var sourceSuggestions = this.aSuggestions[source.id];
        if (sourceSuggestions) {
          this.createList(sourceSuggestions, source);
        }
      }

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
      this.container.select('.hide-button-wrapper').invoke('hide');
      this.ajID = setTimeout( function() { pointer.doAjaxRequests(requestId) }, this.options.delay );

    }
    return false;
  },

  /**
   * Fire the AJAX Request(s) that will get suggestions.
   *
   * @param requestId request identifier, used to ensure that only the latest request is handled, for improved performance
   * @param ajaxRequestParameters optional AJAX request parameters, in case you need to overwrite the defaults
   */
  doAjaxRequests: function (requestId, ajaxRequestParameters)
  {
    if (this.fld.value.length < this.options.minchars) {
      return;
    }

    for (var i=0;i<this.sources.length;i++) {
      var source = this.sources[i];
      if (typeof source.script == 'function') {
        this.fld.addClassName('loading');
        source.script(this.fld.value.strip(), function(suggestions) {
          if (requestId == this.latestRequest) {
            this.aSuggestions[source.id] = suggestions || [];
            suggestions && this.createList(this.aSuggestions[source.id], source);
            this.fld.removeClassName('loading');
          }
        }.bind(this));
      } else {
        this.doAjaxRequest(source, requestId, ajaxRequestParameters);
      }
    }
  },

  /**
   * Fire the AJAX request that will get the suggestions from the specified source.
   *
   * @param source the source to get the suggestions from
   * @param requestId request identifier, used to ensure that only the latest request is handled, for improved performance
   * @param ajaxRequestParameters optional AJAX request parameters, in case you need to overwrite the defaults
   */
  doAjaxRequest: function (source, requestId, ajaxRequestParameters)
  {
    var url = source.script + (source.script.indexOf('?') < 0 ? '?' : '&') + source.varname + "=" + encodeURIComponent(this.fld.value.strip());
    var method = source.method || "get";
    var headers = {};
    if (source.json) {
      headers.Accept = "application/json";
    } else {
      headers.Accept = "application/xml";
    }

    // Allow the default request parameters to be overwritten.
    var defaultAjaxRequestParameters = {
      method: method,
      requestHeaders: headers,
      onCreate: this.fld.addClassName.bind(this.fld, 'loading'),
      onSuccess: this.setSuggestions.bindAsEventListener(this, source, requestId),
      onFailure: function (response) {
        new XWiki.widgets.Notification("$services.localization.render('core.widgets.suggest.transportError')" + response.statusText, "error", {timeout: 5});
      },
      onComplete: this.fld.removeClassName.bind(this.fld, 'loading')
    }
    // Inject a reference to the (cloned) default AJAX request parameters to be able
    // to access the defaults even when they are overwritten by the provided values.
    defaultAjaxRequestParameters.defaultValues = Object.clone(defaultAjaxRequestParameters);
    new Ajax.Request(url, Object.extend(defaultAjaxRequestParameters, ajaxRequestParameters || {}));
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

    var suggestions = this.parseResponse(req, source);
    this.aSuggestions[source.id] = suggestions || [];
    suggestions && this.createList(this.aSuggestions[source.id], source);
  },

  _getNestedProperty: function(obj, path) {
    var properties = path.split('.');
    while (properties.length && (obj = obj[properties.shift()])) {};
    return properties.length > 0 ? null : obj;
  },

  /**
   * Builds the list of suggestions by parsing the given response.
   */
  parseResponse: function(req, source) {
    var suggestions = [];
    if (source.json) {
      var jsondata = req.responseJSON;
      if (!jsondata) {
        return null;
      }
      if (Array.isArray(jsondata)) {
        var results = jsondata;
      } else {
        var results = this._getNestedProperty(jsondata, source.resultsParameter || this.options.resultsParameter);
      }
      for (var i = 0; i < results.length; i++) {
        var result = results[i];
        suggestions.push({
          'id': this._getNestedProperty(result, source.resultId || this.options.resultId),
          'value': this._getNestedProperty(result, source.resultValue || this.options.resultValue),
          'info': this._getNestedProperty(result, source.resultInfo || this.options.resultInfo),
          'icon': this._getNestedProperty(result, source.resultIcon || this.options.resultIcon),
          'hint': this._getNestedProperty(result, source.resultHint || this.options.resultHint),
          'type': this._getNestedProperty(result, source.resultType || this.options.resultType),
          'url': this._getNestedProperty(result, source.resultURL || this.options.resultURL)
        });
      }
    } else {
      var xml = req.responseXML;
      // traverse xml
      var results = xml.getElementsByTagName(source.resultsParameter || this.options.resultsParameter)[0].childNodes;
      // TODO: This is incompatible with the REST search
      for (var i = 0; i < results.length; i++) {
        if (results[i].hasChildNodes()) {
          suggestions.push({
            'id': results[i].getAttribute('id'),
            'value': results[i].childNodes[0].nodeValue,
            'info': results[i].getAttribute('info'),
            'icon': results[i].getAttribute('icon'),
            'hint': results[i].getAttribute('hint'),
            'type': results[i].getAttribute('type'),
            'url': results[i].getAttribute('url')
          });
        }
      }
    }
    return suggestions;
  },

  /**
   * Creates the container that will hold one or multiple source results.
   */
  prepareContainer: function(){

    if (!$(this.options.parentContainer).down('.suggestItems')) {
      // If the suggestion top container is not in the DOM already, we create it and inject it

      var div = new Element("div", { 'class': "suggestItems "+ this.options.className });

      // Get position of target textfield
      var pos = $(this.options.parentContainer).tagName.toLowerCase() == 'body' ? this.fld.cumulativeOffset() : this.fld.positionedOffset();

      // Container width is passed as an option, or field width if no width provided.
      // The 2px substracted correspond to one pixel of border on each side of the field,
      // this allows to have the suggestion box borders well aligned with the field borders.
      // FIXME this should be computed instead, since border might not always be 1px.
      var fieldWidth = this.fld.offsetWidth - 2;
      var containerWidth = this.options.width || fieldWidth;
      var inputPositionLeft = this.fld.viewportOffset().left;
      var browserWidth = $('body').getWidth();

      // if the option is 'auto', we make sure that we have enough place to display it on the left. If not, it will go on the right.
      if (this.options.align == 'left' || (this.options.align == 'auto' && inputPositionLeft + this.options.width < browserWidth)) {
        // Align the box on the left
        div.style.left = pos.left + "px";
      } else if (this.options.align == "center") {
        // Align the box to the center
        div.style.left = pos.left + (fieldWidth - containerWidth) / 2 + "px";
      } else {
        // Align the box on the right.
        // This has a visible effect only when the container width is not the same as the input width
        div.style.left = (pos.left + fieldWidth - containerWidth) + "px";
      }

      div.style.top = (pos.top + this.fld.offsetHeight + this.options.offsety) + "px";
      // Don't enforce the width if it wasn't specified to let the container adjust its width to fit the suggest items.
      div.style[this.options.width ? 'width' : 'minWidth'] = containerWidth + "px";

      // set focus functions for div
      // when focus leaves div, set a timeout to remove the list after an interval
      // when focus enters div, kill the timeout so the list won't be removed
      var pointer = this;
      div.addEventListener("focusin", () => pointer.killTimeout());
      div.addEventListener("focusout", () => pointer.resetTimeout());
      div.addEventListener("mouseout", () => pointer.clearHighlight())

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

    if (this.isInMultiSourceMode) {
      // If we are in multi-source mode, we need to prepare a sub-container for each of the suggestion source
      for (var i=0;i<this.sources.length;i++) {

        var source = this.sources[i];
        source.id = source.id || i;

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
            this.resultContainer.down('.results' + source.id).addClassName('hidden').addClassName('loading');
          }
        }
        else {
          // The sub-container for this source has not been created yet
          // Really create the subcontainer for this source and inject it in the global container
          var sourceContainer = new Element('div', {'class' : 'results results' + source.id}),
              sourceHeader = new Element('div', {'class':'sourceName'});

          if (this.options.unifiedLoader) {
            sourceContainer.addClassName('hidden').addClassName('loading');
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
          sourceHeader.insert(source.name);
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

    var withEnableButton = typeof this.options.hideButton !== "undefined"
                        && typeof this.options.hideButton.positions === "object"
                        && this.options.hideButton.positions.length > 0;
    if (withEnableButton && !this.container.down('.hide-button')) {
      var positions = this.options.hideButton.positions;
      for (var i=0; i< positions.length; i++) {
        var hideButton = new Element('button', {'class' : 'hide-button', 'type' : 'button'})
          .update(this.options.hideButton.text), toInsert = {};
        toInsert[positions[i]] = new Element('div', {'class' : 'hide-button-wrapper'}).update(hideButton);
        hideButton.observe('click', this.clearSuggestions.bindAsEventListener(this));
        this.container.insert(toInsert);
      }
    }

    this.container.fire("xwiki:suggest:containerPrepared", {
      'container' : this.container,
      'suggest' : this
    });

    return this.container;
  },

  /**
   * Create the HTML list of suggestions and then notify that the suggest has been updated if all sources are loaded.
   *
   * @param {Object} arr
   * @param {Object} source the source for data for which to create this list of results.
   */
  createList: function(arr, source)
  {
    this._createList(arr, source);
    if (!this.isInMultiSourceMode || !this.resultContainer.down('.results.loading')) {
      document.fire('xwiki:suggest:updated', {
        'container' : this.container,
        'suggest' : this
      });
    }
  },

  /**
   * Create the HTML list of suggestions.
   *
   * @param {Object} arr
   * @param {Object} source the source for data for which to create this list of results.
   */
  _createList: function(arr, source)
  {
    this.isActive = true;
    var pointer = this;

    this.killTimeout();

    // Determine the source container.
    if (this.isInMultiSourceMode) {
      var sourceContainer = this.resultContainer.down('.results' + source.id);
      sourceContainer.removeClassName('loading');
      sourceContainer.down('.sourceContent').removeClassName('loading');
      (arr.length > 0 || this.options.shownoresults) && sourceContainer.removeClassName('hidden');

      // If we are in mode "unified loader" (showing one loading indicator for all requests and not one per request)
      // and there aren't any source still loading, we remove the unified loading status.
      if (this.options.unifiedLoader && !this.resultContainer.down('.results.loading')) {
        (this.options.loaderNode || this.fld).removeClassName('loading');
      }
    } else {
      var sourceContainer = this.resultContainer;
    }

    // if no results, and shownoresults is false, go no further
    if (arr.length == 0 && !this.options.shownoresults) {
      return false;
    }

    // Ensure any previous list of results for this source gets removed
    sourceContainer.down('ul') && sourceContainer.down('ul').remove();

    // Show the "hide suggestions" buttons
    this.container.select('.hide-button-wrapper').invoke('show');

    // create and populate list
    var list = new XWiki.widgets.XList([], {
       icon: this.options.icon,
       classes: 'suggestList',
       eventListeners: {
          'click' : function (event) { pointer.setHighlightedValue(event); return false; },
          'mouseover' : function () { pointer.setHighlight( this.getElement() ); }
       }
    });

    // loop throught arr of suggestions
    // creating an XlistItem for each suggestion
    //
    for (var i=0,len=arr.length;i<len;i++)
    {
      var escapeHTML = function(value) {
        return ((value || '') + '').escapeHTML();
      };
      var valueNode = new Element('div')
        .insert(new Element('span', {'class':'suggestId'}).update(escapeHTML(arr[i].id)))
        .insert(new Element('span', {'class':'suggestValue'}).update(escapeHTML(arr[i].value)))
        .insert(new Element('span', {'class':'suggestInfo'}).update(escapeHTML(arr[i].info)))
        .insert(new Element('span', {'class':'suggestURL'}).update(escapeHTML(arr[i].url)));

      var item = new XWiki.widgets.XListItem( this.createItemDisplay(arr[i], source) , {
        containerClasses: 'suggestItem ' + (arr[i].type || ''),
        value: valueNode,
        noHighlight: true, // we do the highlighting ourselves
        containerTagName: 'a'
      });
      // When the url is empty, we need to put a correct default value to avoid unexpected page loads/reloads
      item.containerElement.setAttribute('href', arr[i].url || 'javascript:void(0)');
      item.listItemElement.addEventListener('focusin', (event) => pointer.setHighlight(event.currentTarget));
      list.addItem(item);
    }

    // no results
    if (arr.length == 0)
    {
      list.addItem( new XWiki.widgets.XListItem(this.options.noresults, {
                          'classes' : 'noSuggestion',
                          noHighlight :true }) );
    }
    sourceContainer.appendChild( list.getElement() );

    this.suggest = sourceContainer;

    // remove list after an interval
    var pointer = this;
    if (this.options.timeout > 0) {
      this.toID = setTimeout(function () { pointer.clearSuggestions() }, this.options.timeout);
    }
  },

  /**
   * Creates the HTML display for the given suggestion item.
   *
   * @param {Object} date the data associated with a suggestion item (id, value, info, icon, etc.)
   * @param {Object} source the source for the suggeestion item data
   */
  createItemDisplay : function(data, source) {
    var escapedInput = this.sInput ? this.sInput.escapeHTML() : this.sInput;
    var escapedValue = ((data.value || '') + '').escapeHTML();
    // Output is either emphasized or raw value depending on source option.
    var output = source.highlight ? this.emphasizeMatches(escapedInput, escapedValue) : escapedValue;
    if (data.hint) {
      var escapedHint = (data.hint + '').escapeHTML();
      output += "<span class='hint'>" + escapedHint + "</span>";
    }
    if (!this.options.displayValue) {
      var displayNode = new Element("span", {'class':'info'}).update(output);
    } else {
      var displayNode = new Element("div").insert(new Element('div', {'class':'value'}).update(output));
      if (data.info) {
        var escapedInfo = data.info + '';
        if (source.resultInfoHTML  === undefined ? !this.options.resultInfoHTML : !source.resultInfoHTML) {
          escapedInfo = escapedInfo.escapeHTML();
        }
        displayNode.insert(new Element('div', {'class':'info'}).update("<span class='legend'>"
          + this.options.displayValueText + "</span>" + escapedInfo));
      }
    }
    // If the search result contains an icon information, we insert this icon in the result entry.
    if (data.icon) {
      if (data.icon.indexOf('.') >= 0 || data.icon.indexOf('/') >= 0) {
        // The icon is specified as a file path.
        var iconElement = new Element('img', {'src': data.icon, 'class': 'icon'});
      } else {
        // The icon is specified as a CSS class name.
        var iconElement = new Element('i', {'class': 'icon ' + data.icon});
      }
      displayNode.insert({top: iconElement});
    }
    return displayNode;
  },

  /**
   * Emphesize the elements in passed value that matches one of the words typed as input by the user.
   *
   * @param String input the (typed) input
   * @param String value the value to emphasize
   */
  emphasizeMatches:function(input, value)
  {
    if (!input) {
      return value;
    }
    // If the source declares that results are matching, we highlight them in the value
    var output = value,
        // Separate words (called fragments hereafter) in user input
        fragments = input.split(/\s+/).uniq().compact(),
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

    var elem;

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
          elem = list.down('div.results').down('li');
        } else {
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

  setHighlightedValue: function (event)
  {
    if (this.iHighlighted && !this.iHighlighted.hasClassName('noSuggestion'))
    {
      var text = function(element) {
        return element.textContent || element.innerText;
      };
      var icon = this.iHighlighted.down('img.icon');
      var data = {
        'suggest' : this,
        'id': text(this.iHighlighted.down(".suggestId")),
        'value': text(this.iHighlighted.down(".suggestValue")),
        'info': text(this.iHighlighted.down(".suggestInfo")),
        'url': text(this.iHighlighted.down(".suggestURL")),
        'icon' : icon ? icon.src : '',
        'originalEvent' : event
      };

      var selection, newFieldValue;
      if (this.sInput == "" && this.fld.value == "") {
        selection = newFieldValue = data.value;
      } else {
        if (this.seps) {
           var lastIndx = -1;
           for (var i = 0; i < this.seps.length; i++) {
             if (this.fld.value.lastIndexOf(this.seps.charAt(i)) > lastIndx) {
               lastIndx = this.fld.value.lastIndexOf(this.seps.charAt(i));
             }
           }
           if (lastIndx == -1) {
              selection = newFieldValue = data.value;
           } else {
             newFieldValue = this.fld.value.substring(0, lastIndx+1) + data.value;
             selection = newFieldValue.substring(lastIndx+1);
           }
        } else {
          selection = newFieldValue = data.value;
        }
      }

      var event = Event.fire(this.fld, "xwiki:suggest:selected", Object.clone(data));

      if (!event.stopped) {
        this.sInput = selection;
        this.fld.value = newFieldValue;
        this.fld.focus();
        this.clearSuggestions();

        // pass selected object to callback function, if exists
        typeof this.options.callback == "function" && this.options.callback(Object.clone(data));

        // there is a hidden input
        if (this.fld.id.indexOf("_suggest") > 0) {
          var hidden_id = this.fld.id.substring(0, this.fld.id.indexOf("_suggest"));
          var hidden_inp = $(hidden_id);
          if (hidden_inp) {
            hidden_inp.value =  data.info;
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
  resetTimeout: function(timeout)
  {
    if(!timeout) {
      timeout = 1000;
    }
    clearTimeout(this.toID);
    var pointer = this;
    this.toID = setTimeout(function () { pointer.clearSuggestions() }, timeout);
  },

  /**
   * Clear suggestions
   */
  clearSuggestions: function() {
    this.clearHighlight();
    this.killTimeout();
    this.isActive = false;
    var ele = $(this.container);
    var pointer = this;
    if (ele && ele.parentNode) {
      if (this.options.fadeOnClear && window.Effect) {
        new Effect.Fade(ele, {duration: "0.25", afterFinish : function() {
          if($(pointer.container)) {
            $(pointer.container).remove();
          }
        }});
      } else {
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
