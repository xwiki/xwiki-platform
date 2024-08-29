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
#set ($l10nKeys = [
  'web.hierarchy.error'
])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($discard = $l10n.put($key, $services.localization.render($key)))
#end
#[[*/
// Start JavaScript-only code.
(function(l10n) {
  "use strict";

/**
 * Expand hierarchy breadcrumbs on clicks.
 */
require(['jquery', 'xwiki-events-bridge'], function($) {
  $(function() {

    /**
     * Function that expand a breadcumb on some events.
     */
    var expandBreadCrumb = function(event) {
      event.preventDefault();
      var ellipsis = $(this).parent('li');
      ellipsis.addClass('loading');
      // Get the full breadcumb with an AJAX call
      var breadcrumb = $(this).parents('.breadcrumb-expandable');
      var breadcrumbURL = new XWiki.Document(XWiki.Model.resolve(breadcrumb.data('entity'), XWiki.EntityType.DOCUMENT))
        .getURL('get', 'xpage=hierarchy_reference');
      $.get(breadcrumbURL, {
        id: breadcrumb[0].id,
        displayTitle: breadcrumb.data('displaytitle'),
        local: breadcrumb.data('local'),
        excludeSelf: breadcrumb.data('excludeself'),
        treeNavigation: breadcrumb.data('treenavigation')
      }).then(data => {
        var updatedBreadcrumb = $(data);
        breadcrumb.replaceWith(updatedBreadcrumb);
        $(document).trigger('xwiki:dom:updated', {'elements': updatedBreadcrumb.toArray()});
      }).catch(() => {
        new XWiki.widgets.Notification(l10n['web.hierarchy.error'], 'error');
        ellipsis.removeClass('loading');
      });
    };

   /**
    * Add a link to expand breadcrumbs.
    */
    var addExpandLinkToBreadcrumbs = function () {
      $('.breadcrumb-expandable .ellipsis').each(function() {
        var ellipsis = $(this);
        if (!ellipsis.children().first().is('a')) {
          ellipsis.wrapInner(function () {
            // Wrap the ellipsis with a link (to be consistent with other path items) that expands the breadcrumb
            return $('<a href="#"></a>').on('click', expandBreadCrumb);
          });
        }
      });
    };

    addExpandLinkToBreadcrumbs();

    // Initialize breadcrumbs on livetable refresh (because now livetables could have breadcrumbs to display locations)
    $(document).on('xwiki:livetable:displayComplete', addExpandLinkToBreadcrumbs);

  });

});

/**
 * Extend the breadcrumbs with tree navigation.
 */
require(['xwiki-tree', 'bootstrap'], function($) {
  var enhanceBreadcrumb = function(breadcrumb) {
    breadcrumb.children('li.dropdown').on('shown.bs.dropdown', function(event) {
      $(this).find('.dropdown-menu > .breadcrumb-tree').each(function() {
        if (!$.jstree.reference($(this))) {
          $(this).xtree().one('ready.jstree', function(event, data) {
            var tree = data.instance;
            var openToNodeId = tree.element.attr('data-openTo');
            // Open the tree to the specified node and select it.
            openToNodeId && tree.openTo(openToNodeId);
          });
        }
      });
    }).children('.dropdown-menu').on('click', function(event) {
      // Prevent the drop-down from closing when the user expands the tree nodes.
      event.stopPropagation();
    });
  };

  // Re-initialize the tree navigation when the breadcrumb is expanded.
  $(document).on('xwiki:dom:updated', function(event, data) {
    var source = $(data.elements);
    source.is('.breadcrumb') && enhanceBreadcrumb(source);
  });

  // Initialize the tree navigation on page load.
  enhanceBreadcrumb($('ol.breadcrumb'));
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$l10n]));
