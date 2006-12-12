function hideForm(form){
  form.getElementsByTagName("fieldset").item(0).className = "collapsed";
}
function toggleForm(form){
  var fieldset = form.getElementsByTagName("fieldset").item(0);
  if(fieldset.className == "collapsed"){
    fieldset.className = "expanded";
  }
  else{
    fieldset.className = "collapsed";
  }
}

function togglePanelVisibility(element){
  if(element.className.indexOf("expanded") >= 0){
    element.className = element.className.replace('expanded', 'collapsed');
  }
  else{
    element.className = element.className.replace('collapsed', 'expanded');
  }
}
function showsubmenu(element){
  if(element.lastChild.tagName.toLowerCase() == "span"){
    if(window.hidetimer){
      if(window.hideelement == element.lastChild){
        clearTimeout(window.hidetimer);
        window.hidetimer = null;
        window.hideelement = null;
      }
      else{
        doHide();
      }
    }
    /*if(element.lastChild.style.display == "block") return;*/
    element.lastChild.style.left = (computeAbsoluteLeft(element)  - 10) + "px";
    element.lastChild.style.top = computeAbsoluteTop(element)   + "px";
    element.lastChild.className = element.lastChild.className.replace("hidden", "visible");
  }
}
function computeAbsoluteLeft(element) {
  if(window.ActiveXObject){
    return element.offsetLeft - element.parentNode.parentNode.parentNode.currentStyle.marginLeft.match("[0-9]+") - element.lastChild.currentStyle.marginLeft.substring(0, 1);
  }
  return element.offsetLeft;
}
function computeAbsoluteTop(element) {
  if(window.ActiveXObject){
    return element.offsetHeight;
  }
  return element.offsetTop + element.offsetHeight;
}
function hidesubmenu(element){
  if(element.lastChild.tagName.toLowerCase() == "span"){
    window.hideelement = element.lastChild;
    window.hidetimer = setTimeout(doHide, 100);
  }
}

function setDirty(dirty){
  if(dirty === undefined){
    dirty = true;
  }
  window.docdirty = dirty;
}

function doHide(){
  window.hideelement.className = window.hideelement.className.replace("visible", "hidden");
  clearTimeout(window.hidetimer);
  window.hidetimer = null;
  window.hideelement = null;
}
function updateAttachName(form, msg) {
  var fname = form.filepath.value;

  if (fname == "") {
    return false;
  }

  var i = fname.lastIndexOf('\\');
  if (i == -1){
    i = fname.lastIndexOf('/');
  }

  fname = fname.substring(i + 1);
  if (form.filename.value == fname){
    return true;
  }

  if (form.filename.value == ""){
    form.filename.value = fname;
  }
  else {
    if (confirm(msg + " '" + fname + "' ?")){
      form.filename.value = fname;
    }
  }
  return true;
}

function toggleClass(o, className){
  if(!isClassExist(o,className)) {
    o.className += ' ' + className
  }
  else {
    rmClass(o, className);
  }
}
function addClass(o, className){
  if(!isClassExist(o,className))
    o.className += ' ' + className
}

function isClassExist(o,className){
    if(!o.className)
      return false;
    return new RegExp('\\b' + className + '\\b').test(o.className)
}

function rmClass(o, className){
  o.className = o.className.replace(new RegExp('\\s*\\b' + className + '\\b'),'')
}

function openURL(url) {
  win = open( url, "win", "titlebar=0,width=750,height=480,resizable,scrollbars");
  if( win ) {
    win.focus();
  }
}

function openHelp() {
  win = open( "http://www.xwiki.com/xwiki/bin/view/Doc/XWikiSyntax?xpage=plain", "syntax", "titlebar=0,width=750,height=480,resizable,scrollbars");
  if( win ) {
    win.focus();
  }
}

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

function updateName(field1, field2, removeclass) {
  var name = field1.value;
  name = name.replace(/[àâä]/gi,"a")
  name = name.replace(/[éèêë]/gi,"e")
  name = name.replace(/[îï]/gi,"i")
  name = name.replace(/[ôö]/gi,"o")
  name = name.replace(/[ùûü]/gi,"u")
  name = name.replace(/["!?]/g,"");
  name = name.replace(/[_':,;]/g," ");
  name = name.replace(/[\.]/g,"");
  if (removeclass!=false)
   name = name.replace(/class$/gi,"");
  if (navigator.userAgent.indexOf("Safari")==-1) {
   name = name.replace(/\s(.)/g,function(str, p1) { return p1.toUpperCase(); });
   name = name.replace(/^(.)/g,function(str, p1) { return p1.toUpperCase(); });
  }
  name = name.replace(/\s+/g,"");
  if (field2 == null) {
         field1.value = name;
     } else {
         field2.value = name;
     }
  if (name=="")
   return false;
  return true;
}

function noaccent(chaine) {
  temp = chaine.replace(/[àâä]/gi,"a")
  temp = temp.replace(/[éèêë]/gi,"e")
  temp = temp.replace(/[îï]/gi,"i")
  temp = temp.replace(/[ôö]/gi,"o")
  temp = temp.replace(/[ùûü]/gi,"u")
  temp = temp.replace(/["!?]/g,"");
  temp = temp.replace(/[_':]/g," ");
  if (navigator.userAgent.indexOf("Safari")==-1) {
  temp = temp.replace(/\s(.)/g,function(str, p2) { return p2.toUpperCase(); });
  temp = temp.replace(/^(.)/g,function(str, p2) { return p2.toUpperCase(); });
  }
  temp = temp.replace(/\s+/g,"");
  return temp;
}
function prepareName(form) {
 var fname = form.register_first_name.value;
 var lname = form.register_last_name.value;
 var cxwikiname = form.xwikiname;
 if (fname != "") {
   fname = fname.substring(0,1).toUpperCase() + fname.substring(1);
   fname.replace(/ /g,"");
 }
 if (lname != "") {
   lname = lname.substring(0,1).toUpperCase() + lname.substring(1)
   lname.replace(/ /g,"");
 }
 if (cxwikiname.value == "") {
   cxwikiname.value  =  noaccent(fname + lname);
 }
}

