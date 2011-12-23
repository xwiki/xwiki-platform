Lightbox = Class.create({
  initialize: function(formUrl, saveUrl, redirectUrl) {
    this.formUrl = formUrl;
    this.saveUrl = saveUrl;
    this.redirectUrl = redirectUrl;
    this.formData = "";
    this.loadedForms = new Object();
    this.lbinit();
    this.lbShow();
    this.lbLoadForm(formUrl);
  },

  lbShow: function() {
    this.lbLoading();
    toggleClass($('lb-bg'), 'hidden');
    toggleClass($('lb-align'), 'hidden');
    this.resizeBackground();
    if(browser.isIE6x) {
      $$('select').each(function(item) {
        if (item.up('#lb')) {
          return;
        }
        item._x_originalVisibility = item.style['visibility'];
        item.setStyle({visibility: 'hidden'});
      });
    }
  },

  lbHide: function() {
    toggleClass($('lb-bg'), 'hidden');
    toggleClass($('lb-align'), 'hidden');
    if(browser.isIE6x) {
      $$('select').each(function(item) {
        item.setStyle({visibility: item._x_originalVisibility});
      });
    }
  },

  lbLoading: function() {
    if(this.currentUrl) {
      this.loadedForms[this.currentUrl] = $('lb-content').firstChild.cloneNode(true);
    }
    $('lb-content').innerHTML = this.getWaiting();
  },

  lbLoadForm: function(url) {
    this.currentUrl = url;
    if(this.loadedForms[url]) {
      $('lb-content').innerHTML = '';
      this.lbPlaceContentInDocument(this.loadedForms[url], $('lb-content'));
      this.form = c.getElementsByTagName('form')[0];
    } else {
      new Ajax.Request(url, {onSuccess: this.lbFormDataLoaded.bind(this)});
    }
  },

  lbFormDataLoaded: function(transport) {
    var responseContent = document.createElement('div');
    responseContent.innerHTML = transport.responseText;
    $('lb-content').innerHTML = '';
    this.lbPlaceContentInDocument(responseContent, $('lb-content'), 
      function() {
        this.resizeBackground();
      }.bind(this)
    );
    this.form = $('lb-content').getElementsByTagName('form')[0];
  },

  /**
   * Some elements such as script elements are treated specially by the browser and not loaded if they are deep in a
   * DOM tree which is added to the document with Javascript.
   * However if each of these elements are placed in the tree individually, the loading works but execution must stop
   * while scripts are loading otherwise an inline script may be executed before a reference script which it depends on.
   * This function ends and is restarted by a callback each time a script is done loading.
   *
   * @param content A DOM node containing th econtent to place in the document.
   * @param whereToPlace A DOM node in the document inwhich to place the content.
   * @param onComplete A function to run when this is completely finished, note that this function will return before it
   *                   is complete, it will be restarted later by a callback.
   */
  lbPlaceContentInDocument: function(content, whereToPlace, onComplete) {
    // First clear already existing listeners because we will be firing a dom:loaded event for the 
    // benefit of listeners we may be adding.
    document.stopObserving('dom:loaded');

    var scripts = Array.from(content.getElementsByTagName('script'));
    // Opera doesn't render stylesheets unless we expressly place them.
    var links = Array.from(content.getElementsByTagName('link'));
    var styles = Array.from(content.getElementsByTagName('style'));
    var treatSpecially = links.concat(scripts, styles).flatten();

    // Clone all elements in treatSpecially and remove them from the content DOM tree.
    var clones = [];
    for (var i = 0; i < treatSpecially.length; i++) {
      clones[i] = document.createElement(treatSpecially[i].tagName);
      var attributes = treatSpecially[i].attributes;
      for(var j = 0; j < attributes.length; j++) {
        // In IE7 the attributes are there but are empty even if they aren't there.
        // and apparently IE refuses to parse content in a script element if there is a src attribute.
        if (attributes[j].value != "") {
          clones[i].setAttribute(attributes[j].name, attributes[j].value);
        }
      }
      try {
        var cloneContent = treatSpecially[i].innerHTML;
        if (cloneContent.startsWith('//<![CDATA[') && cloneContent.endsWith('//]]>')) {
          // IE drops the entire CDATA section when we set the inner HTML of the clone. In order to preserve the code we
          // have to unwrap it.
          cloneContent = cloneContent.substring(11, cloneContent.length - 5);
        }
        clones[i].innerHTML = cloneContent;
        // Remove element from content.
        treatSpecially[i].parentNode.removeChild(treatSpecially[i]);
      } catch (ie) {
        // Now let's try it the IE7 way.
        // IE doesn't need to have link and style elements handled specially, only Opera.
        if (clones[i].tagName.toLowerCase() == 'script') {
          clones[i].text = treatSpecially[i].text;
          treatSpecially[i].parentNode.removeChild(treatSpecially[i]);
        }
      }
    }

    // Insert the content.
    whereToPlace.appendChild(content);

    /*
     * @param elements - Array - The elements to add to the document in the order given.
     * @param whereToPlace A DOM node in the document inwhich to place the elements.
     * @param onComplete - Function - Do this when the function is completely finished (it will return at the first script
     *                                which needs to be loaded.)
     * @param startAt - Skips over this number of elements at the beginning of the list.
     */ 
    var appendSpecialElements = function(elements, whereToPlace, onComplete, startAt) {
      var i = 0;
      if (startAt) {
        i = startAt;
      }
      while (i < elements.length) {
        whereToPlace.appendChild(elements[i]);
        if (elements[i].tagName.toLowerCase() == 'script' && elements[i].src != '') {
          // In order to make sure the element is loaded before loading the next one, This function ends and then is 
          // restarted by the callback.
          // IE7 does not allow Event.observe(script, 'load'
          // Testing for IE < 8
          if (browser.isIE == true && typeof XDomainRequest == "undefined") {
            Event.observe(elements[i], 'readystatechange', function(event) {
              if (event.element().readyState == 'complete') {
                appendSpecialElements(elements, whereToPlace, onComplete, i + 1);
              }
            });
          } else {
            Event.observe(elements[i], 'load', function() {
              appendSpecialElements(elements, whereToPlace, onComplete, i + 1);
            });
          }
          return;
        }
        i++;
      }
      // Do whatever was supposed to be done after this is finished.
      onComplete();
    }

    // Start running the appendSpecialElements function.
    appendSpecialElements(clones, whereToPlace, function() {
      // Do whatever was supposed to do at completion.
      if(Object.isFunction(onComplete)) {
        onComplete();
      }

      // Finally, we place a script which fires a dom:loaded event. We are not just manually firing the event 
      // because we want to make sure all other scripts have been loaded and parsed first.
      var fireScript = document.createElement('script');
      try {
        fireScript.innerHTML = 'document.fire("dom:loaded");';
      } catch (ie) {
        // IE7
        fireScript.text = 'document.fire("dom:loaded");';
      }
      whereToPlace.appendChild(fireScript);
    }.bind(this));
  },

  lbSaveForm: function() {
    this.lbSaveData();
    Form.disable(this.form);
    this.lbSaveSync(this.saveUrl);
    this.lbHide();
    window.location = this.redirectUrl;
  },

  lbNext: function(nextUrl) {
    this.lbSaveData();
    this.lbLoading();
    this.lbLoadForm(nextUrl);
  },

  lbSaveData: function() {
    this.formData += "&" + Form.serialize(this.form);
    this.formData = this.formData.replace("_segmentChief=&", "=&");
    this.formData = this.formData.replace("_periodicity=&", "=&");
  },

  lbSave: function(url) {
    this.lbSaveData();
    new Ajax.Request(url + "?ajax=1", {parameters: this.formData, onSuccess: this.lbSaveDone.bind(this)});
  },

  lbSaveSync: function(url) {
    new Ajax.Request(url + "?ajax=1", {parameters: this.formData, asynchronous: false});
  },

  lbSaveDone: function(transport) {
    this.lbHide();
  },

  lbClearData: function() {
    this.formData = "";
  },

  lbClose: function() {
    this.lbHide();
    if (this.redirectUrl !== undefined) {
      window.location = this.redirectUrl;
    }
  },

  lbSetNext: function(nextURL) {
    this.nextURL = nextURL;
  },

  getWaiting: function() {
    var msg = "$xwiki.getSkinFile('icons/xwiki/ajax-loader-large.gif')";
    return '<div style="padding: 30px;"><img src="' + msg + '"/></div>';
  },

  lbcustominit: function(lbbgcolor, lbbordercolor, lbfontcolor, lbtype) {
    if (!$('lb')) {
      var lbcontent = this.insertlbcontent(lbbgcolor, lbbordercolor, lbfontcolor, lbtype);
        new Insertion.Top('body', lbcontent);
      }
  },

  lbinit: function() {
    return this.lbcustominit("#FFF", "#FFF", "#000", "rounded");
  },

  insertlbcontent: function(lbbgcolor, lbbordercolor, lbfontcolor, lbtype) {
    var str = '<div id="lb-bg" class="hidden"></div>' + 
      '<div id="lb-align" class="hidden">' + 
      '<div id="lb">' +
      '<div id="lb-top">' +
      '<div id="close-wrap">' +
      '<div id="lb-close" onclick="window.lb.lbClose();" title="Cancel and close">&nbsp;</div>' + // TODO: Refresh just the affected data, using an onClose callback
      '</div>';

    if(lbtype == "lightrounded") {
      str += this.roundedlighttop(lbbgcolor, lbbordercolor);
    } else if(lbtype == "rounded") {
      str += this.roundedtop(lbbgcolor, lbbordercolor);
    } else {
      str += '<div class="lb-squarred" style="background:' + lbbgcolor + '; border-color:' + lbbordercolor + '"></div></div>';
    }

    str += '</div><div class="lb-content" style="background:' +  lbbgcolor + '; border-color:' + lbbordercolor + '; color:' + lbfontcolor + '" id="lb-content">Lightbox Content</div>';

    if(lbtype == "lightrounded") {
      str += this.roundedlightbottom(lbbgcolor, lbbordercolor);
    } else if(lbtype == "rounded") {
      str += this.roundedbottom(lbbgcolor, lbbordercolor);
    } else {
      str += '<div class="lb-squarred" style="background:' + lbbgcolor +'; border-color:' + lbbordercolor + '"></div></div></div></div>';
    }
    return str;
  },

  resizeBackground: function() {
    var newHeight = document.body.parentNode.scrollHeight;
    if (document.body.scrollHeight > newHeight) {
      // IE6
      newHeight = document.body.scrollHeight;
    }
    if (document.body.parentNode.clientHeight > newHeight) {
      // IE7
      newHeight = document.body.parentNode.clientHeight;
    }
    $('lb-bg').style.height = newHeight + "px";
  },
  roundedlightbottom:  function(bgcolor, bordercolor) {
    var str = '<div class="roundedlight"><b class="top">' + 
      '<b class="b4b" style="background:' + bordercolor + ';"></b>' +
      '<b class="b3b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b3b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b1b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b></b> </div>';
    return str;
  },

  roundedbottom: function(bgcolor, bordercolor) {
    var str = '<div class="rounded">' +
      '<b class="bottom" style="padding:0px; margin:0px;">' +
      '<b class="b12b" style="background:' + bordercolor +';"></b>' +
      '<b class="b11b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b10b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b9b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b8b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b7b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b6b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b5b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b4b" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b3b" style="background:'+ bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b2b" style="background:'+ bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b1b" style="background:'+ bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '</b></div>';
    return str;
  },

  roundedlighttop: function(bgcolor, bordercolor) {
    var str = '<div class="roundedlight"><b class="top">' + 
      '<b class="b1" style="background:' + bordercolor + ';"></b>' +
      '<b class="b2" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b3" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b4" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b></b> </div>';
    return str;
  },

  roundedtop: function(bgcolor, bordercolor) {
    var str = '<div class="rounded">' +
      '<b class="top">' +
      '<b class="b1" style="background:' + bordercolor +';"></b>' +
      '<b class="b2" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b3" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b4" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b5" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b6" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b7" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b8" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b9" style="background:' + bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b10" style="background:'+ bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b11" style="background:'+ bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '<b class="b12" style="background:'+ bgcolor + '; border-color:' + bordercolor + ';"></b>' +
      '</b></div>';
    return str;
  },

  lightboxlink: function(linktext, lbcontent)	{
    var str = '<a href="#" onclick="javascript:$(\'lb-content\').innerHTML =' + lbcontent +'; toggleClass($(\'lb-bg\'), \'hidden\'); toggleClass($(\'lb-align\'), \'hidden\');">' + linktext + '</a>';
    return str;
  }
});
