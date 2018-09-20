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
      var includingPagesInput = $('#including-export-pages');
      var excludingPagesInput = $('#excluding-export-pages');

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
         * Update the form of the export according to the tree
         *
         * The general idea is to try keeping only the main spaces and put them with a joker-like (%) on the URL
         * argument. The deselected pages will be then put in the "excluding-export-pages" input form.
         *
         * Now in case of the pagination is deselected, we should not use a joker for the request: for that specific
         * case we put the pages in the "including-export-pages" input form.
         */
        $('#exportModal #exportModelOtherCollapse a.btn').click(function (event) {
          event.preventDefault();
          var button = $(this);
          var url = button.data('url').clone();

          // by default the pages URL attribute is empty
          if (url.params !== undefined) {
            url.params.pages = [];
          } else {
            url.params = {pages: []};
          }

          // to store the list of pages ordered by their parent space
          // it will be used in case a space is "excluded" meaning the joker won't be used
          var includingPagesBySpace = {};

          // the deselected pages that the user does not want to export
          var excludingPages = [];

          // the spaces for which the user deselected the pagination node
          // in that case we must not use the joker in the request to avoid retrieving all pages
          var excludedSpaces = [];

          var excludeSpace = function(space) {

            // if we exclude a space we need to remove it from the pages URL param
            // we will use the "including-export-pages" input form
            url.params.pages = url.params.pages.filter(function (elem) {
              return elem !== space;
            });
            excludedSpaces.push(space);
          };

          var isSpaceExcluded = function (space) {
            for (var i = 0; i < excludedSpaces.length; i++) {
              if (excludedSpaces[i] === space) {
                return true;
              }
            }
            return false;
          };

          // In case the node is a pagination node, we have to split the id to retrieve its references:
          // example of id: pagination:document:xwiki:Main.WebHome
          var retrieveReferenceFromPaginationId = function (paginationNodeId) {
            var tokens = paginationNodeId.split(":");

            var result = "";
            for (var i = 2; i < tokens.length; i++) {
              result += tokens[i];

              if (i < tokens.length -1 ) {
                result += ":";
              }
            }
            return result;
          };

          // we go through all nodes to detect checked and unchecked nodes
          // (jsTree does not provide an immediate API to get unchecked nodes)
          $(treeReference.get_json(tree, {
            flat: true
          })).each(function () {
            var node = treeReference.get_node(this.id);

            var nodeId = node.data.id;

            if (node.data.type === "pagination") {
              nodeId = retrieveReferenceFromPaginationId(node.id);
            }

            var pageReference = XWiki.Model.resolve(nodeId, XWiki.EntityType.DOCUMENT);
            var spaceReference = XWiki.Model.serialize(new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT, pageReference.parent));


            // the node is checked
            if (treeReference.is_checked(node)) {

              // if it's pagination we don't process it: we should have already put the main space with a joker
              // when processing the parent node
              if (node.data.type !== "pagination") {

                // In case the page have children: it's a parent node, we process it to be used in pages param
                if (treeReference.is_parent(node)) {

                  if (!isSpaceExcluded(spaceReference)) {
                    url.params.pages.push(spaceReference);
                  }

                  // we put the WebHome page to avoid loosing it in case the space is excluded later
                  if (includingPagesBySpace[spaceReference] === undefined) {
                    includingPagesBySpace[spaceReference] = [];
                  }
                  includingPagesBySpace[spaceReference].push(XWiki.Model.serialize(pageReference));
                  includingPagesBySpace[spaceReference].push(XWiki.Model.serialize(new XWiki.EntityReference('WebPreferences', XWiki.EntityType.DOCUMENT, pageReference.parent)));

                // if it's not a parent, we fill the list of includingPages in case the space is excluded in
                // a next node
                } else {

                  // we need to retrieve the parent space
                  var parentNode = treeReference.get_node(treeReference.get_parent(node));
                  var parentPageReference = XWiki.Model.resolve(parentNode.data.id, XWiki.EntityType.DOCUMENT);
                  var parentSpaceReference = XWiki.Model.serialize(new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT, parentPageReference.parent));

                  if (includingPagesBySpace[parentSpaceReference] === undefined) {
                    includingPagesBySpace[parentSpaceReference] = [];
                  }
                  includingPagesBySpace[parentSpaceReference].push(XWiki.Model.serialize(pageReference));
                }
              }
            // the node is unchecked
            } else {

              // if it's a pagination node: it means the space must be excluded
              if (node.data.type === "pagination") {
                excludeSpace(spaceReference);
              } else {
                // In case the page have children, it won't be include with a joker, so we're safe.
                if (!treeReference.is_parent(node)) {
                  excludingPages.push(XWiki.Model.serialize(pageReference));
                }
              }
            }
          });

          var aggregatePageNames = function (arrayOfNames) {
            return arrayOfNames.map(function (name) { return encodeURIComponent(name); }).join("&");
          };

          excludingPagesInput.val(aggregatePageNames(excludingPages));

          // we only include pages that have an excluded parent space
          var includingPages = [];
          for (var space in includingPagesBySpace) {
            if (isSpaceExcluded(space)) {
              includingPagesBySpace[space].forEach(function (elem) {
                includingPages.push(elem);
              });
            }
          }
          includingPagesInput.val(aggregatePageNames(includingPages));

          // put the right action for the form
          form.attr('action', url.serialize());
          form.submit();
        });
      });
    });

  });
});
