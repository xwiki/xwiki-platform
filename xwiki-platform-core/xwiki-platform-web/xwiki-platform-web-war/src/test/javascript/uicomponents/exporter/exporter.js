/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
require.config({
  paths: {
    'xwiki-export-tree': 'uicomponents/exporter/exporter.min',
    'xwiki-export-tree-filter': 'uicomponents/exporter/exporter.min'
  }
});

// Mock Velocity bindings.
var $jsontool = {
  serialize: function(input) {return input;}
};
var $paths = {};
var $l10n = {};
var $icons = {};

define(['jquery', 'xwiki-export-tree', 'xwiki-export-tree-filter'], function($) {
  var dataURLPrefix = 'data:application/json';

  // Ignore parameters when data URI is used.
  var originalGet = $.get;
  $.get = function() {
    var url = arguments[0];
    // PhantomJS, used to run the tests in headless mode, doesn't seem to support AJAX requests to data URIs.
    if (url.substring(0, dataURLPrefix.length) === dataURLPrefix) {
      var jsonStart = url.indexOf(',') + 1;
      var data = JSON.parse(url.substring(jsonStart));
      var matches = url.match(/filters=(\w+)[,;]/);
      if (matches) {
        data = data[matches[1]];
      }
      // We also need to handle the tree pagination.
      // See XWIKI-17425: A tree node with checkboxes and pagination is removed from the tree when you open it
      if (Array.isArray(data)) {
        var offset = (arguments[1] || {}).offset || 0;
        data = extractPage(data, offset);
      }
      return $.Deferred().resolve(data).promise();
    } else {
      return originalGet.apply(this, arguments);
    }
  };

  var extractPage = function(data, offset) {
    var page = [];
    for (var i = offset; i < data.length; i++) {
      page.push(data[i]);
      if ((data[i].data || {}).type === 'pagination') {
        break;
      }
    }
    return page;
  };

  var createExportTree = function(data, filter, $element) {
    var deferred = $.Deferred();
    // The 'data-url' attribute is mandatory for dynamic trees (otherwise the tree is static).
    var dataURL = dataURLPrefix + ';filters=' + (filter || '') + ',' + JSON.stringify(data);
    // The root node type and reference (id) are required in order to be able to express the selection of the top level
    // pages using exclusions (we have to use exclusions if there is a top level pagination node that is checked).
    ($element || $('<div></div>')).attr('data-url', dataURL).data('root', {
      id: 'xwiki',
      type: 'wiki'
    }).one('ready.jstree', function(event, data) {
      setTimeout(function() {
        deferred.resolve(data.instance);
      }, 0);
    }).exportTree();
    return deferred.promise();
  };

  var treeData = [
    {
      id: 'document:xwiki:A.WebHome',
      text: 'A',
      children: false,
      data: {
        id: 'xwiki:A.WebHome',
        type: 'document'
      }
    },
    {
      id: 'document:xwiki:B.WebHome',
      text: 'B',
      children: true,
      data: {
        id: 'xwiki:B.WebHome',
        type: 'document',
        validChildren: ['document', 'pagination'],
        childrenURL: 'data:application/json,' + JSON.stringify([
          {
            id: 'document:xwiki:B.D.WebHome',
            text: 'D',
            children: true,
            data: {
              id: 'xwiki:B.D.WebHome',
              type: 'document',
              validChildren: ['document', 'pagination'],
              childrenURL: 'data:application/json,' + JSON.stringify([
                {
                  id: 'document:xwiki:B.D.F.WebHome',
                  text: 'F',
                  children: false,
                  data: {
                    id: 'xwiki:B.D.F.WebHome',
                    type: 'document'
                  }
                }
              ])
            }
          },
          {
            id: 'document:xwiki:B.E',
            text: 'E',
            children: false,
            data: {
              id: 'xwiki:B.E',
              type: 'document'
            }
          },
          {
            id: 'pagination:document:xwiki:B.WebHome',
            text: 'More...',
            children: false,
            data: {
              type: 'pagination',
              offset: 3
            }
          },
          {
            id: 'document:xwiki:B.F',
            text: 'F',
            children: false,
            data: {
              id: 'xwiki:B.F',
              type: 'document'
            }
          }
        ])
      }
    },
    {
      id: 'document:xwiki:C.WebHome',
      text: 'C',
      children: true,
      data: {
        id: 'xwiki:C.WebHome',
        type: 'document',
        validChildren: ['document', 'pagination'],
        childrenURL: 'data:application/json,' + JSON.stringify([
          {
            id: 'document:xwiki:C.G.WebHome',
            text: 'G',
            children: false,
            data: {
              id: 'xwiki:C.G.WebHome',
              type: 'document'
            }
          },
          {
            id: 'document:xwiki:C.H',
            text: 'H',
            children: false,
            data: {
              id: 'xwiki:C.H',
              type: 'document'
            }
          }
        ])
      }
    }
  ];

  describe('Export Tree', function() {
    it('All nodes enabled', function(done) {
      createExportTree(treeData).done(function(tree) {
        // Everything is selected initially.
        expect(tree.isExportingAllPages()).toBe(true);
        expect(tree.hasExportPages()).toBe(true);
        expect(tree.getExportPages()).toEqual({
          'xwiki:A.WebHome': [],
          'xwiki:B.%': [],
          'xwiki:C.%': []
        });

        // Deselect all.
        tree.deselect_all();
        expect(tree.isExportingAllPages()).toBe(false);
        expect(tree.hasExportPages()).toBe(false);
        expect(tree.getExportPages()).toEqual({});

        // Select some nodes.
        tree.select_node(['document:xwiki:A.WebHome', 'document:xwiki:B.WebHome']);
        expect(tree.isExportingAllPages()).toBe(false);
        expect(tree.hasExportPages()).toBe(true);
        expect(tree.getExportPages()).toEqual({
          'xwiki:A.WebHome': [],
          'xwiki:B.%': []
        });

        // Expand a tree node.
        tree.open_node('document:xwiki:B.WebHome', function() {
          // Child nodes are selected by default if the parent node is selected.
          expect(tree.getExportPages()).toEqual({
            'xwiki:A.WebHome': [],
            'xwiki:B.%': []
          });

          // Exclude a leaf child node.
          tree.deselect_node('document:xwiki:B.E');
          expect(tree.getExportPages()).toEqual({
            'xwiki:A.WebHome': [],
            'xwiki:B.%': ['xwiki:B.E']
          });

          // Exclude a non-leaf child node.
          tree.deselect_node('document:xwiki:B.D.WebHome');
          tree.select_node('document:xwiki:B.E');
          expect(tree.getExportPages()).toEqual({
            'xwiki:A.WebHome': [],
            'xwiki:B.%': ['xwiki:B.D.%']
          });

          // Deselect the pagination node to force using includes.
          tree.deselect_node('pagination:document:xwiki:B.WebHome');
          tree.select_node('document:xwiki:B.D.WebHome');
          expect(tree.getExportPages()).toEqual({
            'xwiki:A.WebHome': [],
            'xwiki:B.WebHome': [],
            'xwiki:B.D.%': [],
            'xwiki:B.E': []
          });

          // Deselecting the children leaves the parent selected.
          tree.deselect_node(['document:xwiki:B.D.WebHome', 'document:xwiki:B.E']);
          expect(tree.getExportPages()).toEqual({
            'xwiki:A.WebHome': [],
            'xwiki:B.WebHome': []
          });

          // Expand a child node.
          tree.open_node('document:xwiki:B.D.WebHome', function() {
            // Child nodes are not selected by default if the parent node is not selected.
            expect(tree.getExportPages()).toEqual({
              'xwiki:A.WebHome': [],
              'xwiki:B.WebHome': []
            });

            tree.select_node('document:xwiki:B.D.F.WebHome');
            expect(tree.getExportPages()).toEqual({
              'xwiki:A.WebHome': [],
              'xwiki:B.WebHome': [],
              'xwiki:B.D.F.WebHome': []
            });

            // Deselecting the parent deselects all descendants.
            tree.deselect_node('document:xwiki:B.WebHome');
            expect(tree.getExportPages()).toEqual({
              'xwiki:A.WebHome': []
            });

            // Select only a descendant node.
            tree.deselect_node('document:xwiki:A.WebHome');
            tree.select_node('document:xwiki:B.D.F.WebHome');
            expect(tree.isExportingAllPages()).toBe(false);
            expect(tree.hasExportPages()).toBe(true);
            expect(tree.getExportPages()).toEqual({
              'xwiki:B.D.F.WebHome': []
            });

            // Verify a parent node without pagination.
            tree.open_node('document:xwiki:C.WebHome', function() {
              expect(tree.getExportPages()).toEqual({
                'xwiki:B.D.F.WebHome': []
              });

              // Selecting the parent node should select the child nodes.
              tree.select_node('document:xwiki:C.WebHome');
              expect(tree.getExportPages()).toEqual({
                'xwiki:B.D.F.WebHome': [],
                'xwiki:C.%': []
              });

              // Deselect a child node. Excludes should be used because the parent is selected.
              tree.deselect_node('document:xwiki:C.H');
              expect(tree.getExportPages()).toEqual({
                'xwiki:B.D.F.WebHome': [],
                'xwiki:C.%': ['xwiki:C.H']
              });

              // Deselect the parent node but keep a child node selected. Includes should be used this time.
              tree.deselect_node('document:xwiki:C.WebHome');
              tree.select_node('document:xwiki:C.G.WebHome');
              expect(tree.getExportPages()).toEqual({
                'xwiki:B.D.F.WebHome': [],
                'xwiki:C.G.WebHome': []
              });

              done();
            }, false);
          }, false);
        }, false);
      });
    });

    it('Top level pagination node', function(done) {
      var newTreeData = treeData.slice(0);
      newTreeData.push({
        id: 'pagination:wiki:xwiki',
        text: 'More...',
        children: false,
        data: {
          type: 'pagination'
        }
      });

      createExportTree(newTreeData).done(function(tree) {
        // Everything is selected initially, including the pagination node, which means we can't use includes.
        expect(tree.getExportPages()).toEqual({
          'xwiki:%.%': []
        });

        tree.deselect_node(['document:xwiki:A.WebHome', 'document:xwiki:C.WebHome']);
        expect(tree.getExportPages()).toEqual({
          'xwiki:%.%': ['xwiki:A.WebHome', 'xwiki:C.%']
        });

        // Deselect the pagination node to switch to includes.
        tree.deselect_node('pagination:wiki:xwiki');
        expect(tree.getExportPages()).toEqual({
          'xwiki:B.%': []
        });

        done();
      });
    });

    var disableNodes = function(nodes) {
      nodes.forEach(function(node) {
        node.state = {disabled: true, undetermined: true};
      });
    };

    it('With disabled nodes', function(done) {
      var newTreeData = $.extend(true, {}, {data: treeData}).data;
      var bChildren = JSON.parse(newTreeData[1].data.childrenURL.substring(22));
      // Disable B.WebHome, C.WebHome and B.D.WebHome
      disableNodes([newTreeData[1], newTreeData[2], bChildren[0]]);
      newTreeData[1].data.childrenURL = 'data:application/json,' + JSON.stringify(bChildren);

      createExportTree(newTreeData).done(function(tree) {
        // Everything is selected initially.
        expect(tree.isExportingAllPages()).toBe(true);
        expect(tree.hasExportPages()).toBe(true);
        expect(tree.getExportPages()).toEqual({
          'xwiki:A.WebHome': [],
          'xwiki:B.%': ['xwiki:B.WebHome'],
          'xwiki:C.%': ['xwiki:C.WebHome']
        });

        // Deselect A.WebHome in order to have only undetermined nodes.
        tree.deselect_node('document:xwiki:A.WebHome');
        expect(tree.isExportingAllPages()).toBe(false);
        expect(tree.hasExportPages()).toBe(true);
        expect(tree.getExportPages()).toEqual({
          'xwiki:B.%': ['xwiki:B.WebHome'],
          'xwiki:C.%': ['xwiki:C.WebHome']
        });

        // Open undetermined node.
        tree.open_node('document:xwiki:B.WebHome', function() {
          // Child nodes are selected by default. Excludes are used because of the checked pagination node.
          expect(tree.getExportPages()).toEqual({
            'xwiki:B.%': ['xwiki:B.WebHome', 'xwiki:B.D.%' ],
            'xwiki:B.D.%': ['xwiki:B.D.WebHome'],
            'xwiki:C.%': ['xwiki:C.WebHome']
          });

          // Deselect the pagination node to use includes instead.
          tree.deselect_node('pagination:document:xwiki:B.WebHome');
          expect(tree.getExportPages()).toEqual({
            'xwiki:B.D.%': ['xwiki:B.D.WebHome'],
            'xwiki:B.E': [],
            'xwiki:C.%': ['xwiki:C.WebHome']
          });

          // Open B.D.WebHome to verify an undetermined node without pagination.
          tree.open_node('document:xwiki:B.D.WebHome', function() {
            // Should use includes because there's no pagination node and the parent node is not selected.
            expect(tree.getExportPages()).toEqual({
              'xwiki:B.D.F.WebHome': [],
              'xwiki:B.E': [],
              'xwiki:C.%': ['xwiki:C.WebHome']
            });

            // Deselecting the child should deselect the parent.
            tree.deselect_node('document:xwiki:B.D.F.WebHome');
            expect(tree.is_undetermined('document:xwiki:B.D.WebHome')).toBe(false);
            expect(tree.getExportPages()).toEqual({
              'xwiki:B.E': [],
              'xwiki:C.%': ['xwiki:C.WebHome']
            });

            // Selecting the child should mark the parent as undetermined.
            tree.select_node('document:xwiki:B.D.F.WebHome');
            expect(tree.is_undetermined('document:xwiki:B.D.WebHome')).toBe(true);

            // Select none.
            tree.deselect_all();
            expect(tree.isExportingAllPages()).toBe(false);
            expect(tree.hasExportPages()).toBe(false);
            expect(tree.getExportPages()).toEqual({});

            // Select all preserves disabled nodes undetermined.
            tree.select_all();
            expect(tree.isExportingAllPages()).toBe(true);
            expect(tree.hasExportPages()).toBe(true);
            expect(tree.getExportPages()).toEqual({
              'xwiki:A.WebHome': [],
              'xwiki:B.%': ['xwiki:B.WebHome', 'xwiki:B.D.%'],
              'xwiki:B.D.F.WebHome': [],
              'xwiki:C.%': ['xwiki:C.WebHome']
            });

            // Select none again.
            tree.deselect_all();
            tree.open_node('document:xwiki:C.WebHome', function() {
              // Child nodes should be deselected because the parent is.
              expect(tree.isExportingAllPages()).toBe(false);
              expect(tree.hasExportPages()).toBe(false);
              expect(tree.getExportPages()).toEqual({});

              tree.select_node('document:xwiki:C.H');
              expect(tree.isExportingAllPages()).toBe(false);
              expect(tree.hasExportPages()).toBe(true);
              expect(tree.getExportPages()).toEqual({
                'xwiki:C.H': []
              });

              done();
            });
          });
        });
      });
    });
  });

  var createExportTreeWithFilter = function(data, defaultFilter) {
    var container = $([
      '<div class="export-tree-container">',
        '<ul class="export-tree-filter"></ul>',
        '<div class="export-tree"></div>',
      '</div>'
    ].join('')).appendTo(document.body);

    // Add the filters.
    Object.keys(data).forEach(function(filterId) {
      var filterList = container.find('ul.export-tree-filter');
      $('<li><a href="#"></a></li>').appendTo(filterList).find('a').attr('data-filter', filterId).text(filterId);
    });

    return createExportTree(data, defaultFilter, container.find('.export-tree'));
  };

  var treeDataWithFilters = {
    'alice': [
      {
        id: 'document:xwiki:A.WebHome',
        text: 'A',
        children: false,
        data: {
          id: 'xwiki:A.WebHome',
          type: 'document'
        }
      },
      {
        id: 'document:xwiki:C.WebHome',
        text: 'C',
        children: true,
        data: {
          id: 'xwiki:C.WebHome',
          type: 'document',
          validChildren: ['document', 'pagination'],
          childrenURL: 'data:application/json,' + JSON.stringify([
            {
              id: 'document:xwiki:C.H',
              text: 'H',
              children: false,
              data: {
                id: 'xwiki:C.H',
                type: 'document'
              }
            },
            {
              id: 'document:xwiki:C.I.WebHome',
              text: 'I',
              children: false,
              data: {
                id: 'xwiki:C.I.WebHome',
                type: 'document'
              }
            }
          ])
        }
      }
    ],
    'carol': [
      {
        id: 'document:xwiki:C.WebHome',
        text: 'C',
        children: true,
        data: {
          id: 'xwiki:C.WebHome',
          type: 'document',
          validChildren: ['document', 'pagination'],
          childrenURL: 'data:application/json,' + JSON.stringify([
            {
              id: 'document:xwiki:C.G.WebHome',
              text: 'G',
              children: false,
              data: {
                id: 'xwiki:C.G.WebHome',
                type: 'document'
              }
            },
            {
              id: 'document:xwiki:C.H',
              text: 'H',
              children: false,
              data: {
                id: 'xwiki:C.H',
                type: 'document'
              }
            },
            {
              id: 'document:xwiki:C.I.WebHome',
              text: 'I',
              children: false,
              data: {
                id: 'xwiki:C.I.WebHome',
                type: 'document'
              }
            }
          ])
        }
      }
    ]
  };

  var applyFilter = function(tree, filterId) {
    var deferred = $.Deferred();
    tree.get_container().one('refresh.jstree', function() {
      // Resolve after all the other refresh event listeners have been called.
      setTimeout(function() {
        deferred.resolve();
      }, 0);
    }).closest('.export-tree-container')
      .find('.export-tree-filter a[data-filter="' + filterId + '"]').click();
    return deferred.promise();
  };

  describe('Export Tree Filter', function() {
    it('Switch filters back and forth', function(done) {
      createExportTreeWithFilter(treeDataWithFilters, 'carol').done(function(tree) {
        tree.open_node('document:xwiki:C.WebHome', function() {
          tree.deselect_node('document:xwiki:C.G.WebHome');
          tree.deselect_node('document:xwiki:C.H');
          expect(tree.getExportPages()).toEqual({'xwiki:C.%': ['xwiki:C.G.WebHome', 'xwiki:C.H']});

          // Change the current tree filter.
          applyFilter(tree, 'alice').done(function() {
            // C.H is not excluded because its node is not yet loaded for this filter.
            expect(tree.getExportPages()).toEqual({'xwiki:C.%': []});

            tree.open_node('document:xwiki:C.WebHome', function() {
              // C.H is still not excluded because when a selected node is loaded, all its children are selected.
              expect(tree.getExportPages()).toEqual({'xwiki:C.%': []});

              tree.select_node('document:xwiki:A.WebHome');
              tree.deselect_node('document:xwiki:C.I.WebHome');
              expect(tree.getExportPages()).toEqual({'xwiki:A.WebHome': [], 'xwiki:C.%': ['xwiki:C.I.WebHome']});

              // Switch back to the initial filter.
              applyFilter(tree, 'carol').done(function() {
                // C.G.WebHome preserves its state from this filter because the previous filter didn't have this node.
                // C.I.WebHome inherits the state from the previous filter because both filters have this node.
                expect(tree.getExportPages()).toEqual({'xwiki:C.%': ['xwiki:C.G.WebHome', 'xwiki:C.I.WebHome']});

                done();
              });
            });
          });
        });
      });
    });
  });
});
