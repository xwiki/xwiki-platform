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

/**
 * Expand hierarchy breadcrumbs on clicks.
 */
require(['jquery', 'xwiki-events-bridge'], function($) {
  'use strict';
  
  $(document).ready(function() {
    
    /**
     * Function that expand a breadcumb on some events.
     */
    var expandBreadCrumb = function(event) {
      event.preventDefault();
      var ellipsis = $(this).parent('li');
      ellipsis.addClass('loading');
      // Get the full breadcumb with an AJAX call
      var breadcrumb  = $(this).parents('.breadcrumb-expandable');
      var ajaxURL     = new XWiki.Document(XWiki.Model.resolve(breadcrumb.data('entity'), XWiki.EntityType.DOCUMENT)).getURL('get', 'xpage=hierarchy_reference');
      $.ajax(ajaxURL, { 'data': {
          'id'           : breadcrumb[0].id,
          'displayTitle' : breadcrumb.data('displaytitle'),
          'local'        : breadcrumb.data('local'),
          'excludeSelf'  : breadcrumb.data('excludeself')
      }}).done(function (newBreadcrumb) {
          var $newBreadcrumb = $(newBreadcrumb);
          breadcrumb.replaceWith($newBreadcrumb);
          $(document).trigger('xwiki:dom:updated', {'elements': $newBreadcrumb.toArray()});
        })
        .fail(function (){
          new XWiki.widgets.Notification("$escapetool.javascript($services.localization.render('web.hierarchy.error'))", 'error');
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
          ellipsis.wrapInner(function () {
            // Wrap the ellipsis with a link (to be consistent with other path items) that expands the breadcrumb
            return $('<a href="#"></a>').click(expandBreadCrumb);
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
require(["$!services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', 'require-config.min.js', {'evaluate': true})"], function() {
  require(['tree', 'bootstrap'], function($) {
    $('ol.breadcrumb > li.dropdown > .dropdown-menu').click(function(event) {
      // Prevent the drop-down from closing when the user expands the tree nodes.
      event.stopPropagation();
    });
    $('ol.breadcrumb > li.dropdown').on('shown.bs.dropdown', function(event) {
      $(this).find('.dropdown-menu > .xtree').each(function() {
        if (!$.jstree.reference($(this))) {
          $(this).xtree().one('ready.jstree', function(event, data) {
            var tree = data.instance;
            var openToNodeId = tree.element.attr('data-openTo');
            // Open the tree to the specified node and select it.
            openToNodeId && tree.openTo(openToNodeId);
          });
        }
      });
    });
  });
});
