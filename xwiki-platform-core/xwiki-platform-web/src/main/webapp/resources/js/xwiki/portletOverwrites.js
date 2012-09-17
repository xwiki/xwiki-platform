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

Event.observe(document, 'xwiki:dom:loading', function() {
  /**
   * Make sure AJAX requests go through the portlet.
   */
  var resourceURL = $('resourceURL').value;
  var dispatchURLParamName = 'org.xwiki.portlet.request.parameter.dispatchURL';
  var fRequest = Ajax.Request.prototype.request;
  Ajax.Request.prototype.request = function(servletURL) {
    if (Object.isString(this.options.parameters)) {
      // Parse the query string to manipulate the parameters easily.
      this.options.parameters = this.options.parameters.toQueryParams();
    }
    this.options.parameters = $H(this.options.parameters);
    if (typeof servletURL == 'string' && servletURL.startsWith('$request.contextPath')) {
      // URL relative to the servlet container root.
      this.options.parameters.set(dispatchURLParamName, servletURL);
      servletURL = resourceURL;
    } else {
      var dispatchURL = this.options.parameters.get(dispatchURLParamName);
      if (Object.isString(dispatchURL)) {
        // The given URL is probably the value of the action attribute of an HTML form and thus it is already rewritten.
        // We can't use this URL because it is an Action PortletURL. We need a resource URL.
        servletURL = resourceURL;
        // Strip request parameters from the dispatch URL query string to prevent them from being overwriten.
        var queryStringParams = $H(dispatchURL.toQueryParams());
        this.options.parameters.keys().each(queryStringParams.unset.bind(queryStringParams));
        dispatchURL = dispatchURL.replace(/\?.*/, '?' + queryStringParams.toQueryString());
        if (dispatchURL.endsWith('?')) {
          dispatchURL = dispatchURL.substr(0, dispatchURL.length - 1);
        }
        if (dispatchURL == '') {
          // Don't submit an empty dispatch URL because the request will be made to the home page.
          // The resource URL is enough in this case.
          this.options.parameters.unset(dispatchURLParamName);
        } else {
          // Update the value of the dispatch URL parameter.
          this.options.parameters.set(dispatchURLParamName, dispatchURL);
        }
      }
    }
    fRequest.call(this, servletURL);
  }

  // Namespace the ID parameter passed to the live table constructor.
  if (XWiki.widgets && XWiki.widgets.LiveTable) {
    XWiki.widgets.LiveTable.prototype.initialize = XWiki.widgets.LiveTable.prototype.initialize.wrap(
      function(initialize, url, domNodeName, handler, options) {
        initialize(url, ID(domNodeName), handler, options);
      }
    );
  }

  // The form action URL can be modified from JavaScript before the form is sumitted.
  // Make sure the form is submitted with a portlet URL.
  document.observe('submit', function(event) {
    var form = event.element();
    var servletURL = form.readAttribute('action') || '';
    var actionURL = $('actionURL').value;
    if (servletURL == '') {
      form.action = actionURL;
      // Don't submit an empty dispatch URL because the request will be made to the home page.
      form.getInputs('hidden', dispatchURLParamName).invoke('disable');
    } else if (servletURL.startsWith('$request.contextPath')) {
      form.action = actionURL;
      var dispatchURLs = form.getInputs('hidden', dispatchURLParamName);
      if (dispatchURLs.length > 0) {
        // Update the dispatch URL.
        dispatchURLs[0].value = servletURL;
      } else {
        // Add the dispatch URL parameter.
        form.insert({top: new Element('input', {type: 'hidden', name: dispatchURLParamName, value: servletURL})});
      }
    }
  });
});