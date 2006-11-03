
// Thanks to the great article from Paul Sowden
// http://www.alistapart.com/articles/alternate/
// No copyright on this file.

function setActiveStyleSheet(title) {
  var i, a, main;
  for(i=0; (a = document.getElementsByTagName("link")[i]); i++) {
    if(a.getAttribute("rel").indexOf("style") != -1 && a.getAttribute("title")) {
      a.disabled = true;
      if(a.getAttribute("title") == title) a.disabled = false;
    }
  }
}

function getActiveStyleSheet() {
  var i, a;
  for(i=0; (a = document.getElementsByTagName("link")[i]); i++) {
    if(a.getAttribute("rel").indexOf("style") != -1 && a.getAttribute("title") && !a.disabled) return a.getAttribute("title");
  }
  return null;
}

function getPreferredStyleSheet() {
  var i, a;
  for(i=0; (a = document.getElementsByTagName("link")[i]); i++) {
    if(a.getAttribute("rel").indexOf("style") != -1
       && a.getAttribute("rel").indexOf("alt") == -1
       && a.getAttribute("title")
       ) return a.getAttribute("title");
  }
  return null;
}

function createCookie(name,value,days) {
  if (days) {
    var date = new Date();
    date.setTime(date.getTime()+(days*24*60*60*1000));
    var expires = "; expires="+date.toGMTString();
  }
  else expires = "";
  document.cookie = name+"="+value+expires+"; path=/";
}

function readCookie(name) {
  var nameEQ = name + "=";
  var ca = document.cookie.split(';');
  for(var i=0;i < ca.length;i++) {
    var c = ca[i];
    while (c.charAt(0)==' ') c = c.substring(1,c.length);
    if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
  }
  return null;
}

window.onload = function(e) {
  var cookie = readCookie("style");
  var title = cookie ? cookie : getPreferredStyleSheet();
  setActiveStyleSheet(title);
}

window.onunload = function(e) {
  var title = getActiveStyleSheet();
  createCookie("style", title, 365);
}

var cookie = readCookie("style");
var title = cookie ? cookie : getPreferredStyleSheet();
setActiveStyleSheet(title);

var XWiki = {
  Version: '0.8_pre1',
  require: function(libraryName) {
    // inserting via DOM fails in Safari 2.0, so brute force approach
    document.write('<script type="text/javascript" src="'+libraryName+'"></script>');
  },
  addLibrary: function(scriptLibraryName) {
    JSfileName = 'skin.js'; // This should be added in a xwiki.js file 'xwiki.js'
    if(scriptLibraryName=='scriptaculous') {
	libraries = ['prototype.js', 'util.js', 'effects.js', 'dragdrop.js', 'controls.js'];
    }
    if(scriptLibraryName=='rico') {
	libraries = ['rico.js'];
    }
    var scriptTags = document.getElementsByTagName("script");
    for(var i=0;i<scriptTags.length;i++) {
      if(scriptTags[i].src && scriptTags[i].src.match(JSfileName)) {
        // var path = scriptTags[i].src.replace(JSfileName,scriptLibraryName) + '/';
	  var path = '/xwiki/bin/download/XWiki/MySkin/';

	  for (var j=0;j<libraries.length;j++) {
	    this.require (path + libraries[j]);
	  }
      }
    }
  }
};

var XWikiAjax = {
  requests: new Array(),
  start: function(status) {
    this.status = $(status);
    ajaxEngine.registerRequest ('setValue', 'SetValueResponse?xpage=rdf');
    ajaxEngine.registerAjaxElement (this.status.id);
  },
  addRequest: function(dName, cName, field, value) {
    var request = Object.extend({
      type: 'set'
    });
    request.className = cName;
    request.document = dName;
    request.field = field;
    request.value = value;
    this.requests.push(request);
  },
  end: function() {
    this.requests.reverse();
    var req = this.requests.pop();
    while (req) {
      if (req.type=='set') {
        ajaxEngine.sendRequest ('setValue', 'status='+this.status.id, 'doc='+req.document, 
	    'typedoc='+req.className, 'field='+req.field, 'value='+req.value);
      };
	req = this.requests.pop();
    }
    this.status.innerHtml = 'updated';
  }
};
