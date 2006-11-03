var XWiki = {
  Version: '0.8_pre1',
  require: function(libraryName) {
    // inserting via DOM fails in Safari 2.0, so brute force approach
    document.write('<script type="text/javascript" src="'+libraryName+'"></script>');
  },
  addLibrary: function(scriptLibraryName) {
    JSfileName = 'xwiki.js'; // This should be added in a xwiki.js file 'xwiki.js'
    if(scriptLibraryName=='scriptaculous') {
	libraries = ['prototype.js', 'util.js', 'effects.js', 'dragdrop.js', 'controls.js'];
    }
    if(scriptLibraryName=='rico') {
	libraries = ['rico.js'];
    }
    var scriptTags = document.getElementsByTagName("script");
    for(var i=0;i<scriptTags.length;i++) {
      if(scriptTags[i].src && scriptTags[i].src.match(JSfileName)) {
        var path = scriptTags[i].src.replace(JSfileName,scriptLibraryName) + '/';
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
