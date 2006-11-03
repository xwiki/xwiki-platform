Ajax.XWikiRequest = Class.create();

Object.extend(Object.extend(Ajax.XWikiRequest.prototype, Ajax.Request.prototype), {
  initialize: function(space, docName, options) {

    this.transport = Ajax.getTransport();
    this.setOptions(options);
    this.baseUrl = "/xwiki/bin/view";

    var onComplete = this.options.onComplete || Prototype.emptyFunction;
    this.options.onComplete = (function() {
      this.returnValue(onComplete);
      //onComplete(this.transport);
    }).bind(this);

    this.request(this.generateUrl(space, docName));
  },

    generateUrl: function(space, docName){
        return this.baseUrl + "/" + space + "/" + docName;
  },

  returnValue: function(callBack) {

    if (callBack)
        callBack(this.transport);
    else
        alert("error, callback");
  }
});



var XWiki = Class.create();

XWiki.prototype = {
        initialize: function(wikiUrl){this.wikiUrl = wikiUrl;},
        getSpaces: function(callBack){
            var params = '';
            var myAjax = new Ajax.XWikiRequest( "Ajax", "getSpaces", {method: 'get', parameters: params, onComplete: getSpacesCallBack} );
        },

        getSpacesCallBack: function(ajaxResponse){
            var xml = ajaxResponse.responseXML;

        }
}

