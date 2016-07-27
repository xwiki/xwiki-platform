// TODO: consider putting it in a webjar instead
require(['jquery', '$xwiki.getSkinFile('uicomponents/widgets/tree.min.js', true)'], function ($) {
  'use strict';

  require(['tree'], function () {
    /**
     * Class that represent a decoded URL.
     */
    var DecodedURL = function (path, params) {
      
      /**
       * Fields
       */
      var self    = this;
      self.path   = path;
      self.params = params;
      
      /**
       * Return a serialized version of the url
       */
      self.serialize = function () {
        var url = self.path;
        var separator = '?';
        for (var param in self.params) {
          var value = self.params[param];
          if (param != '' && value !== undefined) {
            if (Array.isArray(value)) {
              for (var i = 0; i < value.length; ++i) {
                url += separator + encodeURIComponent(param) + '=' + encodeURIComponent(value[i]);
                separator = '&';
              }
            } else if (typeof value === 'string') {
              url += separator + encodeURIComponent(param) + '=' + encodeURIComponent(value);
              separator = '&';
            }
          }
        }
        return url;
      };

      /**
       * Return a clone of the object
       */
      self.clone = function () {
        var clone = new DecodedURL();
        clone.path = self.path;
        clone.params = [];
        for (var param in self.params) {
          if (Array.isArray(self.params[param])) {
            clone.params[param] = [];
            for (var i = 0; i < self.params[param].length; ++i) {
              clone.params[param].push(self.params[param][i]);
            }
          } else if (typeof self.params[param] === 'string') {
            clone.params[param] = self.params[param];
          }
        }
        return clone;
      }
    };
    
    /**
     * Parse an URL and returns an object containing the path and the parameters decoded from the query string.
     * Hashes (#) are not supported.
     * TODO: make it more generic and put it in some library.
     */
    var parseURL = function (url) {
      var result = new DecodedURL();
      var queryStringLocator = url.indexOf('?');
      if (queryStringLocator == -1) {
        result.path = url;
      } else {
        result.path = url.substring(0, queryStringLocator);
        result.queryString = url.substring(queryStringLocator + 1);
        result.params = {};
        // Parse the query string
        var parts = result.queryString.split('&');
        for (var i = 0; i < parts.length; ++i) {
          var temp  = parts[i].split('=');
          var name  = decodeURIComponent(temp[0]);
          var value = decodeURIComponent(temp[1]);
          if (result.params[name] !== undefined) {
            if (Array.isArray(result.params[name])) {
              result.params[name].push(value);
            } else {
              var oldValue = result.params[name];
              result.params[name] = [oldValue, value];
            }
          } else {
            result.params[name] = value;
          }
        }
      } 
      
      return result;
    };
    
    
    $(document).ready(function () {
      
      /**
       * Save the default url
       */
      $('#exportModal a.btn').each(function() {
        var button = $(this);
        var url = parseURL(button.attr('href'));
        button.data('url', url);
      });
      
      /**
       * Change the tree behaviour
       */
      var tree          = $('#exportModal .xtree');
      var treeReference = $.jstree.reference(tree);
      treeReference.settings.checkbox.cascade     = 'down';
      treeReference.settings.checkbox.three_state = false ;
      tree.on('loaded.jstree', function () {
        treeReference.check_all();
      });
        
      /**
       * Update the URL of the export buttons according to the tree
       */
       $('#exportModal a.btn').click(function() {
        var button = $(this);
        var tree = $.jstree.reference($('#exportModal .xtree'));
        var url = button.data('url').clone();
        var checkedPages = tree.get_checked();
        if (url.params === undefined) {
          url.params = {};
        }
        url.params.pages = [];
        for (var i = 0; i < checkedPages.length; ++i) {
          var page = checkedPages[i].substring('document:'.length);
          url.params.pages.push(page);
        }
        // Add WebPreferences pages to the XAR export
        if (url.params.format == 'xar') {
          for (var i = 0; i < url.params.pages.length; ++i) {
            var pageReference = XWiki.Model.resolve(url.params.pages[i], XWiki.EntityType.DOCUMENT);
            // It only concerns non-terminal pages
            if (pageReference.getName() == 'WebHome') {
              pageReference.name = 'WebPreferences';
              url.params.pages.push(XWiki.Model.serialize(pageReference));
            }
          }
        }
        button.attr('href', url.serialize());
      });
    });
    
  });
});
