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

/*!
#set ($jsExtension = '.min.js')
#if (!$services.debug.minify)
  #set ($jsExtension = '.js')
#end
*/

require.config({
  paths: {
    jsTree: '$!services.webjars.url("jstree", "jstree$jsExtension")',
    JobRunner: '$!services.webjars.url("org.xwiki.platform:xwiki-platform-job-webjar", "jobRunner$jsExtension")',
    'tree-finder': '$!services.webjars.url("org.xwiki.platform:xwiki-platform-tree-webjar", "finder$jsExtension")',
    tree: '$!services.webjars.url("org.xwiki.platform:xwiki-platform-tree-webjar", "tree$jsExtension")'
  },
  shim: {
    jsTree: {
      deps: ['jquery']
    }
  }
});

require(['jquery', 'xwiki-meta', 'tree'], function($, xm) {
  'use strict';
  var getAnswerProperties = function() {
    var questionForm = $(this);

    var deleteTree = questionForm.find('.deleteTree');

    if (deleteTree.length) {
      var answerProperties = deleteTree.data('job-answer-properties-data');

      var selectedNodes = deleteTree.jstree().get_selected(true);

      answerProperties.selectAllFreePages = false;
      for (var i = 0; i < selectedNodes.length; ++i) {
        var node = selectedNodes[i];
        if (node.id == 'freePages') {
          // For free pages, we can rely on the state of the "freePage" node
          answerProperties.selectAllFreePages = true;
        } else {
          answerProperties.selectedDocuments.push(node.id);
        }
      }

      return answerProperties;
    }
  };

  /**
   * Called when a question is being asked
   */
  var initQuestion = function(event) {
    var uiQuestion = $(this);

    // we want this to be initialized only for the right question
    if (uiQuestion.hasClass('.deleteWarningXClass')) {
      return;
    }

    // Get the form
    var questionForm = uiQuestion.find('.form-question');

    if (questionForm.length) {
      // Get the tree element
      var deleteTree = questionForm.find('.deleteTree');

      if (deleteTree.length) {
        // Register data callback
        questionForm.data('job-answer-properties-extra', getAnswerProperties);

        /**
         * Represent the selected pages & extensions the user can chose to delete
         */
        var answerProperties = {
            // Pages that have been manually marked by the user to be removed
            selectedDocuments: [],

          // All pages that does not contains an used XClass
            selectAllFreePages: false
        };

        deleteTree.data('job-answer-properties-data', answerProperties);

        // Enable the tree that will display the pages to delete
        deleteTree.xtree({plugins: ['checkbox'], core: {themes: {icons: true, dots: true}}});

        // Called when the user click on "select all"
        questionForm.find('.btSelectAllTree').click(function(event){
          event.preventDefault();
          deleteTree.jstree().check_all();
        });

        // Called when a node has been opened on the tree
        deleteTree.on('after_open.jstree', function (event) {
          var treeReference = deleteTree.jstree();
          var nodes = treeReference.get_json(deleteTree, {flat: true});

          for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            if (typeof node.data != "undefined" && node.data.type == "object") {
              var nodeDom = treeReference.get_node(node.id, true);
              nodeDom.find('.jstree-checkbox').hide()
            }
          }
        });

        // Called when the user click on "select none"
        questionForm.find('.btUnselectAllTree').click(function(event){
          event.preventDefault();
          deleteTree.jstree().uncheck_all();
        });
      }
    }
  };

  $('.ui-question').each(function() {
    var uiQuestion = $(this);

    // Register for any event in the future
    uiQuestion.on('job:question:loaded', initQuestion);

    // Try to init just in case
    initQuestion.bind(uiQuestion)(null);
  });
});
