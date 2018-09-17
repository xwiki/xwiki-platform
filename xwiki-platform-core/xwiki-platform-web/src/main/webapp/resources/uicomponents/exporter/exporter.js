// TODO: consider putting it in a webjar instead
require([
  'jquery',
  "$!services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', 'require-config.min.js', {'evaluate': true})"
], function ($) {
  'use strict';

  /**
   * Class that represent a decoded URL.
   * TODO: make it more generic and put it in some library.
   */
  var DecodedURL = function (path, params) {

    /**
     * Fields
     */
    var self = this;
    self.path = path;
    self.params = params;

    /**
     * Parse an URL and returns an object containing the path and the parameters decoded from the query string.
     * Hashes (#) are not supported.
     */
    self.initFromURL = function (url) {
      self.params = {};
      var queryStringLocator = url.indexOf('?');
      if (queryStringLocator == -1) {
        self.path = url;
      } else {
        self.path = url.substring(0, queryStringLocator);
        var queryString = url.substring(queryStringLocator + 1);
        // Parse the query string
        var parts = queryString.split('&');
        for (var i = 0; i < parts.length; ++i) {
          var temp = parts[i].split('=');
          var name = decodeURIComponent(temp[0]);
          var value = decodeURIComponent(temp[1]);
          if (self.params[name] !== undefined) {
            if (Array.isArray(self.params[name])) {
              self.params[name].push(value);
            } else {
              var oldValue = self.params[name];
              self.params[name] = [oldValue, value];
            }
          } else {
            self.params[name] = value;
          }
        }
      }
    };

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
   * Work with the Export Tree
   */
  require(['tree'], function () {
    $(document).ready(function () {
      var form = $('#export-form');
      var checkedPagesInput = $('#checked-pages');
      var uncheckedPagesInput = $('#unchecked-pages');
      var otherPages = $('#other-pages');

      /**
       * Save the default url
       */
      $('#exportModal a.btn').each(function () {
        var button = $(this);
        var url = new DecodedURL();
        url.initFromURL(button.attr('href'));
        button.data('url', url);
      });

      /**
       * Initialize the tree.
       * Note: we need to wait the 'loaded.jstree' event otherwise we have synchronization issues.
       */
      var tree = $('#exportModal .xtree');
      tree.on('loaded.jstree', function () {

        // Change the tree behaviour
        var treeReference = $.jstree.reference(tree);
        treeReference.settings.checkbox.cascade = 'down';
        treeReference.settings.checkbox.three_state = false;
        treeReference.settings.contextmenu.select_node = false;

        // Check all by default
        treeReference.check_all();

        /**
         * Modifying the context menu
         */
        treeReference.settings.contextmenu.items = function (node) {
          var items = {};

          items.select_children = {
            label: "$escapetool.javascript($services.localization.render('core.exporter.selectChildren'))",
            action: function () {
              for (var i = 0; i < node.children.length; ++i) {
                var child = node.children[i];
                treeReference.check_node(child);
              }
            },
            _disabled: !node.state.opened
          };
          items.unselect_children = {
            label: "$escapetool.javascript($services.localization.render('core.exporter.unselectChildren'))",
            action: function () {
              for (var i = 0; i < node.children.length; ++i) {
                var child = node.children[i];
                treeReference.uncheck_node(child);
              }
            },
            _disabled: !node.state.opened
          };

          return items;
        };

        /**
         * Handle 'select all' button
         */
        $('#exportModal .tree_select_all').click(function () {
          treeReference.check_all();
          return false;
        });

        /**
         * Handle 'select none' button
         */
        $('#exportModal .tree_select_none').click(function () {
          treeReference.uncheck_all();
          return false;
        });

        /**
         * Update the URL of the export buttons according to the tree
         */
        $('#exportModal #exportModelOtherCollapse a.btn').click(function (event) {
          event.preventDefault();
          var button = $(this);
          var url = button.data('url').clone();

          var checkedPages = [];
          var uncheckedPages = [];

          // we go through all nodes to detect checked and unchecked nodes
          // (jsTree does not provide an immediate API to get unchecked nodes)
          $(treeReference.get_json(tree, {
            flat: true
          })).each(function () {
            var node = treeReference.get_node(this.id);

            // the node is checked
            if (treeReference.is_checked(node)) {
              // if the node is pagination then we should set the otherPages attribute
              if (node.data.type == "pagination") {
                otherPages.val('true');
              } else {
                var pageReference = XWiki.Model.resolve(node.data.id, XWiki.EntityType.DOCUMENT);
                checkedPages.push(XWiki.Model.serialize(pageReference));
                // In case the page could have children
                if (pageReference.getName() == 'WebHome') {
                  // The node could be checked but not loaded
                  if (node.state.loaded == false) {
                    // In that case, we need to add all children using a wildcard.
                    var spaceReference = new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT,
                      pageReference.parent);
                    checkedPages.push(XWiki.Model.serialize(spaceReference));
                  }
                  // Special behaviour for the XAR export
                  if (url.params.format == 'xar') {
                    // Also add the WebPreferences
                    var webPreferencesReference = new XWiki.EntityReference('WebPreferences', XWiki.EntityType.DOCUMENT,
                      pageReference.parent);
                    checkedPages.push(XWiki.Model.serialize(webPreferencesReference));
                  }
                }
              }
              // the node is unchecked
            } else {
              // if the node is pagination then we should set the otherPages attribute
              if (node.data.type == "pagination") {
                otherPages.val('false');
              } else {
                var pageReference = XWiki.Model.resolve(node.data.id, XWiki.EntityType.DOCUMENT);
                uncheckedPages.push(XWiki.Model.serialize(pageReference));
                // In case the page could have children
                if (pageReference.getName() == 'WebHome') {
                  // The node could be unchecked but not loaded
                  if (node.state.loaded == false) {
                    // In that case, we need to add all children using a wildcard.
                    var spaceReference = new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT,
                      pageReference.parent);
                    uncheckedPages.push(XWiki.Model.serialize(spaceReference));
                  }
                }
              }
            }
          });

          var aggregatePageNames = function (arrayOfNames) {
            return arrayOfNames.map(name => encodeURIComponent(name)).join("&");
          };

          checkedPagesInput.val(aggregatePageNames(checkedPages));
          uncheckedPagesInput.val(aggregatePageNames(uncheckedPages));

          // pages are given through the input, not through the URL
          if (url.params !== undefined) {
            url.params.pages = [];
          }

          // put the right action for the form
          form.attr('action', url.serialize());
          form.submit();
        });
      });
    });

  });
});
