/**
 * Prefix IDs with portlet response namespace.
 */
var ID = function(id) {
  var namespace = "$!request.getAttribute('javax.portlet.response').getNamespace()";
  return id.startsWith(namespace) ? id : (namespace + '-' + id);
}

/**
 * Wrap in order to namespace the ID.
 */
XWiki.displayDocExtra = XWiki.displayDocExtra.wrap(
  function(displayDocExtra, extraID, extraTemplate, scrollToAnchor) {
    displayDocExtra(ID(extraID), extraTemplate, scrollToAnchor);
  }
);

/**
 * Make sure AJAX requests go through the portlet.
 */
Event.observe(document, 'xwiki:dom:loaded', function() {
  var resourceURL = $('resourceURL').value;
  var fRequest = Ajax.Request.prototype.request;
  Ajax.Request.prototype.request = function(servletURL) {
    if (typeof servletURL == 'string' && servletURL.startsWith('$request.contextPath')) {
      // URL relative to the servlet container root.
      if (Object.isString(this.options.parameters)) {
        this.options.parameters = this.options.parameters.toQueryParams();
      }
      this.options.parameters['org.xwiki.portlet.parameter.dispatchURL'] = servletURL;
      servletURL = resourceURL;
    }
    fRequest.call(this, servletURL);
  }
});