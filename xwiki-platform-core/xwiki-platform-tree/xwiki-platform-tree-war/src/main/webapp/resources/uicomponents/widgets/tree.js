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
// TODO: Move this file in the WebJar once we add support for loading JavaScript files from WebJars as Skin Extensions.
require(['xwiki-tree'], function($) {
  // The node actions, displayed when a tree node is hovered.
  const nodeActions = $(`
    <div class="jstree-node-actions">
      <button title="More Actions" aria-haspopup="true" aria-expanded="false">
        <span class="fa fa-ellipsis-h"><span>
      </button>
    </div>
  `);
  $(document).on('mouseenter.treeNodeActions focus.treeNodeActions', '.jstree-node-actions', function() {
    // The tree node is hovered only when its label is hovered, which is an anchor so we can't put the node actions
    // inside. Thus when the user hovers the node actions the node is not hovered anymore so we need to make it hovered
    // ourserlves.
    $(this).addClass('active').prev('.jstree-anchor').addClass('jstree-hovered');
  }).on('mouseleave.treeNodeActions blur.treeNodeActions', '.jstree-node-actions', function() {
    nodeActions.removeClass('active').prev('.jstree-anchor').removeClass('jstree-hovered');
    // We don't remove the node actions right away because the mouse may leave the node actions to enter the node label
    // (and thus hover the node).
    setTimeout(function() {
      if (!nodeActions.prev('.jstree-anchor').hasClass('jstree-hovered')) {
        nodeActions.remove();
      }
    }, 0);
  }).on('click.treeNodeActions', '.jstree-node-actions button', function(event) {
    const tree = $(this).closest('.jstree').jstree(true);
    tree.show_contextmenu(event.currentTarget, event.pageX, event.pageY, event);
  });

  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.xtree').xtree().one('ready.jstree', function(event, data) {
      var tree = data.instance;
      var openToNodeId = tree.element.attr('data-openTo');
      // Open the tree to the specified node and select it.
      openToNodeId && data.instance.openTo(openToNodeId);

      // We don't want to overwrite the browser's context menu (right click).
      $(this).off('contextmenu.jstree', '.jstree-anchor');
    }).filter('[data-contextmenu=true]').on('hover_node.jstree.contextMenu', function(event, data) {
      // Add the node actions when the node is hovered.
      const tree = data.instance;
      const $node = tree.get_node(data.node, true);
      $node.children('.jstree-anchor').after(nodeActions);
    }).on('dehover_node.jstree.contextMenu', function(event, data) {
      const tree = data.instance;
      const $node = tree.get_node(data.node, true);
      const parent = $node.parent()[0];
      // We don't remove the node actions right away because the mouse may leave the node label to enter the node
      // actions (because the node is hovered only when the node label is hovered).
      setTimeout(function() {
        if (!nodeActions.hasClass('active') && nodeActions.parent()[0] === $node[0]) {
          nodeActions.remove();
        }
      }, 0);
    });
  };

  $(init).on('xwiki:dom:updated', init);
});
