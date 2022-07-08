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
define('entityResourceDisplayer', ['jquery', 'resource'], function($, $resource) {
  'use strict';

  var displayFromBreadcrumb = function(resourceReference, breadcrumb) {
    breadcrumb.addClass('resource-hint');
    var label = breadcrumb.find('.active').remove();
    label = label.has('a').length ? label.find('a') : $('<span></span>').text(label.text());
    label.addClass('resource-label');
    var icon = $('<span class="resource-icon"></span>').addClass($resource.types[resourceReference.type].icon);
    var remove = $('<span class="glyphicon glyphicon-remove remove"></span>');
    // Remove the home icon from the breadcrumb because it distracts the user (from the resource icon) and because it
    // doesn't bring additional information to identify the resource. We all know that every path starts from home.
    breadcrumb.find('.wiki').first().remove();
    return $('<div></div>').append(icon).append(document.createTextNode(' ')).append(label)
      .append(document.createTextNode(' ')).append(remove).add(breadcrumb);
  };

  var maybeDisplayFromBreadcrumb = function(resourceReference, breadcrumb, deferred) {
    if (breadcrumb.hasClass('breadcrumb')) {
      deferred.resolve(displayFromBreadcrumb(resourceReference, breadcrumb));
    } else {
      deferred.reject();
    }
  };

  $resource.displayers.doc = function(resourceReference) {
    var deferred = $.Deferred();
    $.post(XWiki.currentDocument.getURL('get'), {
      language: $('html').attr('lang'),
      xpage: 'hierarchy_reference',
      reference: resourceReference.reference,
      limit: 5
    }).done(function(html) {
      maybeDisplayFromBreadcrumb(resourceReference, $(html), deferred);
    }).fail(function() {
      deferred.reject();
    });
    return deferred.promise();
  };

  $resource.displayers.attach = function(resourceReference) {
    var deferred = $.Deferred();
    var attachmentReference = $resource.convertResourceReferenceToEntityReference(resourceReference);
    $.post(XWiki.currentDocument.getURL('get'), {
      language: $('html').attr('lang'),
      xpage: 'hierarchy_reference',
      reference: XWiki.Model.serialize(attachmentReference.parent),
      limit: 4
    }).done(function(html) {
      var attachmentLink = $('<li class="attachment active"><a></a></li>');
      var attachmentURL = new XWiki.Document(attachmentReference.parent).getURL('download') + '/' +
        encodeURIComponent(attachmentReference.name);
      attachmentLink.find('a').attr('href', attachmentURL).text(attachmentReference.name);
      var breadcrumb = $(html);
      breadcrumb.find('.active').removeClass('active').after(attachmentLink);
      maybeDisplayFromBreadcrumb(resourceReference, breadcrumb, deferred);
    }).fail(function() {
      deferred.reject();
    });
    return deferred.promise();
  };
});
