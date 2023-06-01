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
(function() {
  'use strict';

  class QuickActions {

    /**
     * Creates a new QuickActions instance with the provided search configuration.
     *
     * @param {Object} config - the quick actions configuration options
     */
    constructor(config) {
      this.config = Object.assign({
        // Default values for quick action / group fields.
        defaultAction: {group: '', id: '', name: '', description: '', iconClass: '', iconURL: '', shortcut: ''},
        defaultGroup: {id: '', name: '', order: Infinity}
      }, config);

      // Configuration for the quick actions search.
      this.config.search = Object.assign({
        includeScore: true,
        // Allow one character missmatch in 4 (i.e. match at least 3 characters when the input text is 4 characters).
        // With the default distance parameter being 100 and location 0 this also means we're matching only the first 25
        // characters which is fine considering that we split the indexed information into tokens (by whitespace).
        threshold: 0.25,
        keys: [
          // We obviously give more weight to the action id and name, but we also match the action description and
          // group.
          {name: 'id', weight: 5},
          {name: 'nameTokens', weight: 4},
          {name: 'descriptionTokens', weight: 2},
          {name: 'group.id', weight: 1},
          {name: 'group.nameTokens', weight: 1}
        ]
      }, this.config.search);

      // Start with an empty list of quick actions and no groups;
      this.actions = [];
      this.groups = {};

      // Initialize the quick action search asynchronously.
      this.searchReady = new Promise(function(resolve, reject) {
        require(['fuse'], function(Fuse) {
          resolve(new Fuse(this.actions, this.config.search));
        }.bind(this), reject);
      }.bind(this));
    }

    /**
     * Adds one or more quick actions.
     *
     * @param {Object | Array} the quick actions to add
     */
    addActions() {
      for (var i = 0; i < arguments.length; i++) {
        if (Array.isArray(arguments[i])) {
          this.addActions.apply(this, arguments[i]);
        } else {
          this.addAction(arguments[i]);
        }
      }
    }

    /**
     * Adds a quick action.
     *
     * @param {Object} action - the action to add
     */
    addAction(action) {
      action = Object.assign({}, this.config.defaultAction, action);
      action.nameTokens = action.name.split(/\s+/);
      action.descriptionTokens = action.description.split(/\s+/);
      var group;
      if (!Object.hasOwn(this.groups, action.group)) {
        if (typeof action.group === 'string') {
          group = this.addGroup({id: action.group, name: action.group});
        } else {
          group = this.addGroup(action.group);
        }
      } else {
        group = this.groups[action.group];
      }
      action.group = group;
      return this.searchReady.then(function(fuse) {
        fuse.add(action);
      }).catch(function (error) {
        console.error('Failed to initialize quick action search because: ' + error);
        this.actions.push(action);
      }.bind(this));
    }

    /**
     * Defines one or more quick action groups.
     *
     * @param {Object | Array} the groups to add
     */
    addGroups() {
      for (var i = 0; i < arguments.length; i++) {
        if (Array.isArray(arguments[i])) {
          this.addGroups.apply(this, arguments[i]);
        } else {
          this.addGroup(arguments[i]);
        }
      }
    }

    /**
     * Defines a new quick action group.
     *
     * @param {Object} the group to add
     */
    addGroup(group) {
      group = Object.assign({}, this.config.defaultGroup, group);
      group.nameTokens = (group.name || '').split(/\s+/);
      this.groups[group.id] = group;
      return group;
    }

    /**
     * Searches for quick actions that match the given text.
     *
     * @param {String} text - the text to match
     * @return {Promise} a promise that resolves with the array of quick actions that match the given text
     */
    search(text) {
      if (text.length) {
        return this.searchReady.then(function(fuse) {
          return fuse.search(text);
        });
      } else {
        // Return a shallow copy of the actions array in order to be able to modify it safely (e.g. add groups).
        return Promise.resolve(this.actions.slice());
      }
    }

    /**
     * Sort the given quick actions by group score and action score.
     *
     * @param {Array} the quick actions to sort
     * @return {Array} the sorted array of quick actions
     */
    sort(actions) {
      if (actions.length && actions[0].score) {
        // Sort search results based on the group and action score.
        var matchedGroups = actions.reduce(function(matchedGroups, action) {
          matchedGroups[action.item.group.id] = matchedGroups[action.item.group.id] || [];
          matchedGroups[action.item.group.id].push(action);
          return matchedGroups;
        }, {});
        // The group score is given by the best action score within that group. But note that a lower score is better, 0
        // meaning a perfect match.
        var groupScores = {};
        Object.entries(matchedGroups).forEach(function(entry) {
          groupScores[entry[0]] = entry[1].reduce(function(min, action) {
            return Math.min(min, action.score);
          }, Infinity);
        });
        return actions.sort(function(alice, bob) {
          if (alice.item.group.id === bob.item.group.id) {
            // Order by action score inside each group (lower score means higher importance).
            return alice.score - bob.score;
          } else {
            // Order by group score outside groups (lower score means higher importance).
            return groupScores[alice.item.group.id] - groupScores[bob.item.group.id];
          }
        }).map(function(action) {
          return action.item;
        });
      } else {
        // Sort quick actions based on the group order.
        return actions.sort(function(alice, bob) {
          if (alice.group.id === bob.group.id) {
            // Preserve the action order inside each group (the insertion order).
            return 0;
          } else {
            // Lower order value means higher importance.
            return alice.group.order - bob.group.order;
          }
        });
      }
    }
  }

  //----------------------------------
  // Extend the autocomplete feature with support for:
  // * grouping items
  // * skipping groups when navigating the items with the keyboard
  // * having a different output template per item
  //----------------------------------
  var AutoComplete = CKEDITOR.plugins.autocomplete;

  var View = AutoComplete.view;
  var AdvancedView = function(editor) {
    // Call the parent class constructor.
    View.call(this, editor);
  };
  // Inherit the view methods.
  AdvancedView.prototype = CKEDITOR.tools.prototypedCopy(View.prototype);

  /**
   * Opens the suggest panel.
   */
  AdvancedView.prototype.open = function() {
    // The mouse over listener registered by the AutoComplete checks all the ascendants of the event target in order to
    // find the wrapping item. There's no wrapping item when the mouse is over a group so the root document is reached.
    // Unfortunately the mouse over listener calls hasAttribute without checking the node type and the document node
    // doesn't have this method. The following is a workaround for this bug.
    CKEDITOR.dom.document.prototype.hasAttribute = function() {
      return false;
    };
    return View.prototype.open.apply(this, arguments);
  };

  /**
   * Closes the suggest panel.
   */
  AdvancedView.prototype.close = function() {
    // Remove the hack in order to limit the side effects only for the time the suggest panel is open.
    delete CKEDITOR.dom.document.prototype.hasAttribute;
    return View.prototype.close.apply(this, arguments);
  };

  /**
   * Updates the list of items in the suggest panel. Overwrites the inherited behaviour in order to add support for
   * grouping items.
   *
   * @param {CKEDITOR.plugins.autocomplete.model.item[]} items - the items that match the current query (typed text)
   */
  AdvancedView.prototype.updateItems = function(items) {
    var fragment = new CKEDITOR.dom.documentFragment(this.document);
    var previousGroupId;
    items.forEach(function(item) {
      if (item.group && previousGroupId !== item.group.id) {
        previousGroupId = item.group.id;
        fragment.append(this.createGroup(item.group));
      }
      fragment.append(this.createItem(item));
    }.bind(this));

    this.appendItems(fragment);
    this.selectedItemId = null;
  };

  /**
   * Creates the group element based on the {@link #groupTemplate}.
   *
   * @param {Object} group - the group for which an element will be created
   * @returns {CKEDITOR.dom.element}
   */
  AdvancedView.prototype.createGroup = function(group) {
    var encodedGroup = encodeItem(group),
      groupElement = CKEDITOR.dom.element.createFromHtml(this.groupTemplate.output(encodedGroup), this.document);
    return groupElement;
  };

  /**
   * Applies HTML encoding to all the property values from the given object.
   *
   * @param {Object} item - the item to encode
   */
  var encodeItem = function(item) {
    return CKEDITOR.tools.array.reduce(CKEDITOR.tools.object.keys(item), function(encodedItem, key) {
      encodedItem[key] = CKEDITOR.tools.htmlEncode(item[key]);
      return encodedItem;
    }, {});
  };

  // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Regular_Expressions#escaping
  function escapeRegExp(string) {
    return string.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"); // $& means the whole matched string
  }

  var AdvancedAutoComplete = function(editor, config) {
    config = Object.assign({}, {
      textTestCallback: function(range) {
        // Suggest actions only when the selection is collapsed (caret).
        if (!range.collapsed) {
          return null;
        }
        // Use the text match plugin which does the tricky job of performing a text search in the DOM.
        return CKEDITOR.plugins.textMatch.match(range, function(text, offset) {
          // Get the text before the caret.
          var left = text.slice(0, offset),
              // Will look for the marker followed by text.
              match = left.match(new RegExp(escapeRegExp(config.marker) + '.{0,30}$'));
          if (match) {
            return {
              start: match.index,
              end: offset
            };
          }
        });
      }
    }, config);

    // Call the parent class constructor.
    AutoComplete.call(this, editor, config);

    var groupTemplate = config.groupTemplate || [
      '<li data-group="{id}" class="ckeditor-autocomplete-group" title="{name}">',
        '<h6>{name}</h6>',
      '</li>'
    ].join('');
    this.view.groupTemplate = new CKEDITOR.template(groupTemplate);
  };
  // Inherit the autocomplete methods.
  AdvancedAutoComplete.prototype = CKEDITOR.tools.prototypedCopy(AutoComplete.prototype);

  /**
   * Overwrite to return the advanced view.
   */
  AdvancedAutoComplete.prototype.getView = function() {
    return new AdvancedView(this.editor);
  };

  /**
   * Returns HTML that should be inserted into the editor when the item is committed, optionally triggering an editor
   * command.
   *
   * See also the {@link #commit} method.
   *
   * @param {CKEDITOR.plugins.autocomplete.model.item} item
   * @returns {String} The HTML to insert.
   */
  AdvancedAutoComplete.prototype.getHtmlToInsert = function(item) {
    // Schedule the command execution after the AutoComplete panel is closed.
    this.maybeScheduleCommand(item);
    return item.outputHTML || '';
  };

  /**
   * If the given item specifies an editor command then schedule its execution on the next event cycle.
   *
   * @param {Object} item - the selected AutoComplete item
   */
  AdvancedAutoComplete.prototype.maybeScheduleCommand = function(item) {
    var command = item.command || {};
    if (typeof command === 'string') {
      command = {name: command};
    }
    if (command.name) {
      setTimeout(function() {
        this.editor.execCommand(command.name, command.data);
      }.bind(this), 0);
    }
  };

  // Expose the advanced autocomplete so it can be used later.
  CKEDITOR.plugins.AdvancedAutoComplete = AdvancedAutoComplete;
  CKEDITOR.plugins.AdvancedAutoComplete.View = AdvancedView;

  /**
   * The quick actions (slash) plugin.
   */
  CKEDITOR.plugins.add('xwiki-slash', {
    requires: 'autocomplete,xwiki-focusedplaceholder',

    beforeInit: function(editor) {
      editor.quickActions = new QuickActions();

      // Advertise the quick actions feature on empty lines, if the placeholder hasn't been customized already.
      ['p', 'li', 'td'].forEach(tagName => {
        if (!editor.config['xwiki-focusedplaceholder'].placeholder[tagName]) {
          editor.config['xwiki-focusedplaceholder'].placeholder[tagName] = 'xwiki-slash.placeholder';
        }
      });
    },

    init: function(editor) {
      editor.quickActions.addGroups([
        {
          id: 'structure',
          name: editor.localization.get('xwiki-slash.group.structure'),
          order: 100
        }, {
          id: 'content',
          name: editor.localization.get('xwiki-slash.group.content'),
          order: 200
        }, {
          id: 'macros',
          name: editor.localization.get('xwiki-slash.group.macros'),
          order: 400
        }, {
          id: 'actions',
          name: editor.localization.get('xwiki-slash.group.actions'),
          order: 600
        }, {
          id: 'table',
          name: editor.localization.get('xwiki-slash.group.table'),
          order: 700
        }
      ]);

      editor.quickActions.addActions([
        //
        // Structure
        //
        {
          group: 'structure',
          id: 'h1',
          name: editor.lang.format.tag_h1, // jshint ignore:line
          iconClass: 'fa fa-header',
          description: editor.localization.get('xwiki-slash.action.h1.hint'),
          command: {
            name: 'xwiki-applyStyle',
            data: {
              element: 'h1'
            }
          }
        }, {
          group: 'structure',
          id: 'h2',
          name: editor.lang.format.tag_h2, // jshint ignore:line
          iconClass: 'fa fa-header',
          description: editor.localization.get('xwiki-slash.action.h2.hint'),
          command: {
            name: 'xwiki-applyStyle',
            data: {
              element: 'h2'
            }
          }
        }, {
          group: 'structure',
          id: 'h3',
          name: editor.lang.format.tag_h3, // jshint ignore:line
          iconClass: 'fa fa-header',
          description: editor.localization.get('xwiki-slash.action.h3.hint'),
          command: {
            name: 'xwiki-applyStyle',
            data: {
              element: 'h3'
            }
          }
        }, {
          group: 'structure',
          id: 'p',
          name: editor.localization.get('xwiki-slash.action.p.name'),
          iconClass: 'fa fa-paragraph',
          description: editor.localization.get('xwiki-slash.action.p.hint'),
          command: {
            name: 'xwiki-applyStyle',
            data: {
              element: 'p'
            }
          }
        }, {
          group: 'structure',
          id: 'ul',
          name: editor.localization.get('xwiki-toolbar.bulletedlist'),
          iconClass: 'fa fa-list-ul',
          description: editor.localization.get('xwiki-slash.action.ul.hint'),
          command: 'bulletedlist'
        }, {
          group: 'structure',
          id: 'ol',
          name: editor.localization.get('xwiki-toolbar.numberedlist'),
          iconClass: 'fa fa-list-ol',
          description: editor.localization.get('xwiki-slash.action.ol.hint'),
          command: 'numberedlist'
        }, {
          group: 'structure',
          id: 'table',
          name: editor.lang.table.toolbar,
          iconClass: 'fa fa-table',
          description: editor.localization.get('xwiki-slash.action.table.hint'),
          command: 'table'
        }, {
          group: 'structure',
          id: 'blockquote',
          name: editor.localization.get('xwiki-slash.action.blockquote.name'),
          iconClass: 'fa fa-quote-left',
          description: editor.localization.get('xwiki-slash.action.blockquote.hint'),
          command: 'blockquote'
        }, {
          group: 'structure',
          id: 'info',
          name: editor.localization.get('xwiki-toolbar.infoBox'),
          iconClass: 'fa fa-info-circle',
          description: editor.localization.get('xwiki-slash.action.info.hint'),
          command: {
            name: 'xwiki-macro-insert',
            data: {
              name: 'info',
              content: editor.localization.get('xwiki-slash.action.info.defaultContent')
            }
          }
        }, {
          group: 'structure',
          id: 'success',
          name: editor.localization.get('xwiki-toolbar.successBox'),
          iconClass: 'fa fa-check-circle',
          description: editor.localization.get('xwiki-slash.action.success.hint'),
          command: {
            name: 'xwiki-macro-insert',
            data: {
              name: 'success',
              content: editor.localization.get('xwiki-slash.action.success.defaultContent')
            }
          }
        }, {
          group: 'structure',
          id: 'warning',
          name: editor.localization.get('xwiki-toolbar.warningBox'),
          iconClass: 'fa fa-exclamation-triangle',
          description: editor.localization.get('xwiki-slash.action.warning.hint'),
          command: {
            name: 'xwiki-macro-insert',
            data: {
              name: 'warning',
              content: editor.localization.get('xwiki-slash.action.warning.defaultContent')
            }
          }
        }, {
          group: 'structure',
          id: 'error',
          name: editor.localization.get('xwiki-toolbar.errorBox'),
          iconClass: 'fa fa-exclamation-circle',
          description: editor.localization.get('xwiki-slash.action.error.hint'),
          command: {
            name: 'xwiki-macro-insert',
            data: {
              name: 'error',
              content: editor.localization.get('xwiki-slash.action.error.defaultContent')
            }
          }
        }, {
          group: 'structure',
          id: 'hr',
          name: editor.localization.get('xwiki-toolbar.horizontalrule'),
          iconClass: 'fa fa-minus',
          description: editor.localization.get('xwiki-slash.action.hr.hint'),
          command: 'horizontalrule',
        },

        //
        // Content
        //
        {
          group: 'content',
          id: 'a',
          name: editor.lang.link.toolbar,
          iconClass: 'fa fa-link',
          shortcut: '[',
          description: editor.localization.get('xwiki-slash.action.a.hint'),
          outputHTML: '['
        }, {
          group: 'content',
          id: 'img',
          name: editor.lang.common.image,
          iconClass: 'fa fa-image',
          description: editor.localization.get('xwiki-slash.action.img.hint'),
          command: 'image'
        }, {
          group: 'content',
          id: 'mention',
          name: editor.localization.get('xwiki-slash.action.mention.name'),
          iconClass: 'fa fa-user-o',
          shortcut: '@',
          description: editor.localization.get('xwiki-slash.action.mention.hint'),
          outputHTML: '@'
        }, {
          group: 'content',
          id: 'emoji',
          name: editor.localization.get('xwiki-slash.action.emoji.name'),
          iconClass: 'fa fa-smile-o',
          shortcut: ':',
          description: editor.localization.get('xwiki-slash.action.emoji.hint'),
          outputHTML: ':sm'
        },

        //
        // Macros
        //
        {
          group: 'macros',
          id: 'code',
          name: editor.localization.get('xwiki-toolbar.code'),
          iconClass: 'fa fa-code',
          description: editor.localization.get('xwiki-slash.action.code.hint'),
          command: {
            name: 'xwiki-macro',
            data: {
              name: 'code',
              parameters: {
                language: 'none'
              },
            }
          }
        }, {
          group: 'macros',
          id: 'toc',
          name: editor.localization.get('xwiki-toolbar.toc'),
          iconClass: 'fa fa-list',
          description: editor.localization.get('xwiki-slash.action.toc.hint'),
          command: {
            name: 'xwiki-macro-insert',
            data: {
              name: 'toc'
            }
          }
        }, {
          group: 'macros',
          id: 'include',
          name: editor.localization.get('xwiki-toolbar.include'),
          iconClass: 'fa fa-file-text-o',
          description: editor.localization.get('xwiki-slash.action.include.hint'),
          command: {
            name: 'xwiki-macro',
            data: {
              name: 'include'
            }
          }
        }, {
          group: 'macros',
          id: 'macros',
          name: editor.localization.get('xwiki-slash.action.macros.name'),
          iconClass: 'fa fa-th-list',
          description: editor.localization.get('xwiki-slash.action.macros.hint'),
          command: 'xwiki-macro'
        },

        //
        // Actions
        //
        {
          group: 'actions',
          id: 'find',
          name: editor.lang.find.title,
          iconClass: 'fa fa-search',
          description: editor.localization.get('xwiki-slash.action.find.hint'),
          command: 'find'
        },

        //
        // Table
        //

        // Rows
        {
          group: 'table',
          id: 'table_row_before',
          name: editor.lang.table.row.insertBefore,
          iconClass: 'fa fa-plus',
          description: editor.localization.get('xwiki-slash.action.table_row_before.hint'),
          command: 'rowInsertBefore'
        }, {
          group: 'table',
          id: 'table_row_after',
          name: editor.lang.table.row.insertAfter,
          iconClass: 'fa fa-plus',
          description: editor.localization.get('xwiki-slash.action.table_row_after.hint'),
          command: 'rowInsertAfter'
        }, {
          group: 'table',
          id: 'table_row_delete',
          name: editor.localization.get('xwiki-slash.action.table_row_delete.name'),
          iconClass: 'fa fa-trash',
          description: editor.localization.get('xwiki-slash.action.table_row_delete.hint'),
          command: 'rowDelete'
        },

        // Columns
        {
          group: 'table',
          id: 'table_col_before',
          name: editor.lang.table.column.insertBefore,
          iconClass: 'fa fa-plus',
          description: editor.localization.get('xwiki-slash.action.table_col_before.hint'),
          command: 'columnInsertBefore'
        }, {
          group: 'table',
          id: 'table_col_after',
          name: editor.lang.table.column.insertAfter,
          iconClass: 'fa fa-plus',
          description: editor.localization.get('xwiki-slash.action.table_col_after.hint'),
          command: 'columnInsertAfter'
        }, {
          group: 'table',
          id: 'table_col_delete',
          name: editor.localization.get('xwiki-slash.action.table_col_delete.name'),
          iconClass: 'fa fa-trash',
          description: editor.localization.get('xwiki-slash.action.table_col_delete.hint'),
          command: 'columnDelete'
        },

        // Cells
        {
          group: 'table',
          id: 'table_cell_split_horizontal',
          name: editor.lang.table.cell.splitHorizontal,
          iconClass: 'fa fa-bars',
          description: editor.localization.get('xwiki-slash.action.table_cell_split_horizontal.hint'),
          command: 'cellHorizontalSplit'
        }, {
          group: 'table',
          id: 'table_cell_split_vertical',
          name: editor.lang.table.cell.splitVertical,
          iconClass: 'fa fa-columns',
          description: editor.localization.get('xwiki-slash.action.table_cell_split_vertical.hint'),
          command: 'cellVerticalSplit'
        }, {
          group: 'table',
          id: 'table_cell_merge_right',
          name: editor.localization.get('xwiki-slash.action.table_cell_merge_right.name'),
          iconClass: 'fa fa-compress',
          description: editor.localization.get('xwiki-slash.action.table_cell_merge_right.hint'),
          command: 'cellMergeRight'
        }, {
          group: 'table',
          id: 'table_cell_merge_down',
          name: editor.localization.get('xwiki-slash.action.table_cell_merge_down.name'),
          iconClass: 'fa fa-compress',
          description: editor.localization.get('xwiki-slash.action.table_cell_merge_down.hint'),
          command: 'cellMergeDown'
        }, {
          group: 'table',
          id: 'table_cell_before',
          name: editor.lang.table.cell.insertBefore,
          iconClass: 'fa fa-plus',
          description: editor.localization.get('xwiki-slash.action.table_cell_before.hint'),
          command: 'cellInsertBefore'
        }, {
          group: 'table',
          id: 'table_cell_after',
          name: editor.lang.table.cell.insertAfter,
          iconClass: 'fa fa-plus',
          description: editor.localization.get('xwiki-slash.action.table_cell_after.hint'),
          command: 'cellInsertAfter'
        }, {
          group: 'table',
          id: 'table_cell_delete',
          name: editor.localization.get('xwiki-slash.action.table_cell_delete.name'),
          iconClass: 'fa fa-trash',
          description: editor.localization.get('xwiki-slash.action.table_cell_delete.hint'),
          command: 'cellDelete'
        },

        {
          group: 'table',
          id: 'table_delete',
          name: editor.lang.table.deleteTable,
          iconClass: 'fa fa-trash',
          description: editor.localization.get('xwiki-slash.action.table_delete.hint'),
          command: 'tableDelete'
        }
      ]);

      editor.quickActions.addGroups(editor.config.extraQuickActionGroups || []);
      editor.quickActions.addActions(editor.config.extraQuickActions || []);

      editor.addCommand('xwiki-applyStyle', {
        exec: function(editor, style) {
          editor.applyStyle(new CKEDITOR.style(style));
        }
      });

      var filterActiveActions = function(matchedActions) {
        var removedActions = editor.config.removeQuickActions || [];
        return matchedActions.filter(function(action) {
          var actionId = action.score ? action.item.id : action.id;
          return removedActions.indexOf(actionId) < 0;
        }).filter(function(action) {
          var command = action.score ? action.item.command : action.command;
          if (command && typeof command != 'string') {
            command = command.name;
          }
          return !command || editor.getCommand(command).state !== CKEDITOR.TRISTATE_DISABLED;
        });
      };

      new CKEDITOR.plugins.AdvancedAutoComplete(editor, {
        marker: '/',
        itemTemplate: [
          '<li data-id="{id}" class="ckeditor-autocomplete-item">',
            '<div class="ckeditor-autocomplete-item-head">',
              '<span class="ckeditor-autocomplete-item-icon-wrapper">',
                // We have to output both icon types but normally only one is defined and the other is hidden.
                '<img src="{iconURL}"/>',
                '<span class="{iconClass}"></span>',
              '</span>',
              '<span class="ckeditor-autocomplete-item-label">{name}</span>',
              '<span class="ckeditor-autocomplete-item-shortcut">{shortcut}</span>',
            '</div>',
            '<div class="ckeditor-autocomplete-item-hint">{description}</div>',
          '</li>'].join(''),

        dataCallback: function(matchInfo, callback) {
          // Remove the slash '/'.
          var query = matchInfo.query.substring(1);
          // Filter the available actions.
          editor.quickActions.search(query).then(function(matchedActions) {
            // Sort by group best score and action score.
            callback(editor.quickActions.sort(filterActiveActions(matchedActions)));
          }).catch(function(error) {
            console.error('Failed to search quick actions because: ' + error);
            callback([]);
          });
        }

      });
    }
  });
})();
