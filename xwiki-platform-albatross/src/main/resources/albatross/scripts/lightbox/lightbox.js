Lightbox = Class.create();

Lightbox.prototype =  {	
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
    $('lb-bg').style.height = (document.body.offsetHeight + 35)+"px";
  },

  lbHide: function() {
    toggleClass($('lb-bg'), 'hidden');
    toggleClass($('lb-align'), 'hidden');
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
      var c = $('lb-content');
      $('lb-content').innerHTML = "";
      $('lb-content').appendChild(this.loadedForms[url]);
      this.form = c.getElementsByTagName('form')[0];
      var scripts = c.getElementsByTagName("script");
      for(var i = 0; i < scripts.length; ++i) {
        eval(scripts[i].text);
      }
    } else {
      new Ajax.Request(url, {onSuccess: this.lbFormDataLoaded.bind(this)});
    }
  },

  lbFormDataLoaded: function(transport) {
    var c = $('lb-content');
    c.innerHTML = "<div>" + transport.responseText + "</div>";
    this.form = c.getElementsByTagName('form')[0];
    var scripts = c.getElementsByTagName("script");
    for(var i = 0; i < scripts.length; ++i) {
      eval(scripts[i].text);
    }
    $('lb-bg').style.height = (document.body.offsetHeight + 35)+"px";
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
    window.location = this.redirectUrl;
  },

  lbSetNext: function(nextURL) {
    this.nextURL = nextURL;
  },

  getWaiting: function() {
    return '<div style="padding: 30px;"><img src="$xwiki.getSkinFile('icons/ajax-loader.gif')"/></div>';
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
      str += '<div class="lb-squarred" style="backgrounddee:' + lbbgcolor + '; border-color:' + lbbordercolor + '"></div></div>';
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
};
