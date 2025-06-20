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
const config = JSON.parse(document.getElementById("attachment-move-config").textContent);
require(['jquery', config.treeWebjar], function ($) {
  const targetLocation = $('#targetLocation');
  const openToNodeId = targetLocation.val();
  const selectButton = $('#move.xform .modal-footer .btn-primary');
  const modal = $('#move.xform div.documentTree');
  const treeSelector = $('#move.xform button.newLocationTree');
  var tree = undefined;
  var pageWhiteIcon = undefined;

  function validateSelection(instance) {
    const selected = instance.get_selected(false)[0];
    return selected && selected.startsWith("document:");
  }

  // TODO: Should be moved to a common place (see XWIKI-19320).
  function getPageWhiteIcon() {
    if (pageWhiteIcon === undefined) {
      const iconURL = `${XWiki.contextPath}/rest/wikis/${encodeURIComponent(XWiki.currentWiki)}/iconThemes/icons?name=page_white`;
      var response = window.fetch(iconURL, {
        headers: {
          'Accept': 'application/json'
        }
      });
      response = response.then(response => response.json());
      pageWhiteIcon = response.then(response => response.icons[0]);
    }
    return pageWhiteIcon;
  }

  treeSelector.on('click', function() {
    modal.modal({show: true});
    if (tree === undefined) {
      tree = $('#move.xform .location-tree').xtree({
        core: {
          multiple: false
        }
      }).one('ready.jstree', function (event, data) {
        if (openToNodeId) {
          data.instance.openTo("document:" + openToNodeId);
        }
      }).on('changed.jstree', function (event, data) {
        selectButton.prop('disabled', !validateSelection(data.instance));
      }).on('dblclick', '.jstree-anchor', function (event) {
        if (validateSelection($.jstree.reference(this))) {
          selectButton.click();
        }
      });
    }
  });

  selectButton.on('click', function() {
    modal.modal('hide');
    var docReference = $.jstree.reference(tree).get_selected(false)[0]
    const idx = docReference.indexOf(':')
    docReference = docReference.substring(idx + 1);
    if (docReference) {
      const selectize = targetLocation.data('selectize');
      const doc = new XWiki.Document(XWiki.Model.resolve(docReference, XWiki.EntityType.DOCUMENT));
      $.getJSON(doc.getRestURL(), function (data) {
        getPageWhiteIcon().then(icon => {
          const item = {
            value: data.id,
            //searchValue: searchValue,
            label: data.title,
            hint: '',
            icon: icon,
            url: doc.getURL()
          };

          selectize.addOption(item);
          selectize.createItem(item.value, false);
        })
      });
    }
  });
});
