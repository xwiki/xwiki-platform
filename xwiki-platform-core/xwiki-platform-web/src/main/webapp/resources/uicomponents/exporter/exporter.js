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
         * This function is called recursively to process each parent node in order to built properly the map of pages
         * to include and exclude.
         *
         * @param rootNode: the parent node to process
         * @param exportPages: a map of list, keys are pages to include, values are list of pages to exclude
         */
        var processLevel = function(rootNode, exportPages) {

          // set default value for pagination
          var isPaginationExisting = false;
          var isPaginationChecked = false;

          var nodeId = rootNode.data.id;

          var pageReference = XWiki.Model.resolve(nodeId, XWiki.EntityType.DOCUMENT);
          var pageJoker = XWiki.Model.serialize(new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT, pageReference.parent));

          var nodeWithChildren = [];
          var includedPages = [];
          var excludedPages = [];

          // first we need to put the root node in the right list
          if (!treeReference.is_checked(rootNode)) {
            excludedPages.push(XWiki.Model.serialize(pageReference));
          } else {
            includedPages.push(XWiki.Model.serialize(pageReference));
          }

          // then process its children
          rootNode.children.forEach(function (nodeDom) {

            var node = treeReference.get_node(nodeDom);
            var subNodeId = node.data.id;
            var subPageReference;

            // if the node does not have children (easy case)
            if (treeReference.is_leaf(node)) {

              // pagination node is actually a leaf node
              if (node.data.type === "pagination") {
                isPaginationExisting = true;
                isPaginationChecked = treeReference.is_checked(node);

              // standard document node
              } else {
                subPageReference = XWiki.Model.serialize(XWiki.Model.resolve(subNodeId, XWiki.EntityType.DOCUMENT));

                // the page ref in the right list
                if (treeReference.is_checked(node)) {
                  includedPages.push(subPageReference);
                } else {
                  excludedPages.push(subPageReference);
                }
              }

            // if the node has children
            } else {
              subPageReference = XWiki.Model.resolve(subNodeId, XWiki.EntityType.DOCUMENT);
              var subPageJoker = XWiki.Model.serialize(new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT, subPageReference.parent));

              // if it's not loaded, we'll manage the entire space
              if (!treeReference.is_loaded(node)) {

                // put the space in the right list
                if (treeReference.is_checked(node)) {
                  includedPages.push(subPageJoker);
                } else {
                  excludedPages.push(subPageJoker);
                }
              } else {
                // in that case the page will be managed within its own space, so we need to exclude it
                // in the current context to avoid selecting it in the request
                // e.g. I'm in Foo.% I selected Foo.Bar but only some of its children:
                // I need Foo.Bar.% to be excluded in the request with Foo.%
                // then another part of the request will select Foo.Bar.X, X being the selected children.
                excludedPages.push(subPageJoker);
                nodeWithChildren.push(node);
              }
            }
          });

          // if we don't have a pagination node, or we have one and it's checked:
          // we manage the export with exporting the space and excluding stuff that needs to be
          if (!isPaginationExisting || (isPaginationExisting && isPaginationChecked)) {
            exportPages[pageJoker] = excludedPages;

          // a pagination exists but it's not checked:
          // we need to manage the export by specifying exactly what we include, we don't need to specify exclude
          } else {
            includedPages.forEach(function (elem) {
                exportPages[elem] = [];
              }
            );
          }

          // we call the same function for the children
          nodeWithChildren.forEach(function (node) {
            processLevel(node, exportPages);
          });
        };

        /**
         * Update the form of the export according to the tree
         *
         */
        $('#exportModal #exportModelOtherCollapse a.btn').click(function (event) {
          event.preventDefault();
          var button = $(this);
          var url = button.data('url').clone();
          var exportPages = {};

          // by default the pages URL attribute is empty
          if (url.params !== undefined) {
            url.params.pages = [];
          } else {
            url.params = {pages: []};
          }

          // get the rootNode of the tree
          var rootNodeId = treeReference.get_node("#").children[0];
          var rootNode = treeReference.get_node(rootNodeId);

          // process starting by the rootNode and build map of pages
          processLevel(rootNode, exportPages);

          // useful to create quickly the right String given an array of page names
          var aggregatePageNames = function (arrayOfNames) {
            return arrayOfNames.map(function (name) { return encodeURIComponent(name); }).join("&");
          };

          // in case an old form was already existing
          // we could remove it after the submit, but it's easier that way for testing
          $('#export-form').remove();

          var form = $('<form id="export-form" />').appendTo("body");
          form.attr({
            action: url.serialize(),
            method: 'post'
          });

          // create on the fly the hidden inputs
          for (var pages in exportPages) {
            $('<input>').attr({
              type: 'hidden',
              name: 'pages',
              value: pages
            }).appendTo(form);

            $('<input>').attr({
              type: 'hidden',
              name: 'excludes',
              value: aggregatePageNames(exportPages[pages])
            }).appendTo(form);
          }
          form.submit();
        });
      });
    });

  });
});
