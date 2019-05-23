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
    'xwiki-selectize': "$xwiki.getSkinFile('uicomponents/suggest/xwiki.selectize.js', true)" +
      "?v=$escapetool.url($xwiki.version)"
  }
});

define('xwiki-suggestAttachments', ['jquery', 'xwiki-selectize'], function($) {
  'use strict';

  var getSelectizeOptions = function(select) {
    return {
      create: true,
      maxOptions: 10,
      // The document where the selected values are saved and where new files are being uploaded. Stored attachment
      // references will be relative this document.
      documentReference: select.data('documentReference'),
      // Where to look for attachments. The following is supported:
      // * "wiki:wikiName" look for attachments in the specified wiki
      // * "space:spaceReference": look for attachments in the specified space
      // * "document:documentReference" look for attachments in the specified document
      searchScope: select.data('searchScope'),
      // Whether or not to use the attachment name (instead of its relative reference) as value when the search scope is
      // limited to a specific document (if all the attachments are from the same document then the attachment name is
      // enough to identify an attachment, and there's no need to escape it as an entity reference).
      useAttachmentName: false,
      load: function(text, callback) {
        loadAttachments(text, this.settings).done(callback).fail(callback);
      },
      loadSelected: function(text, callback) {
        loadAttachment(text, this.settings).done(callback).fail(callback);
      }
    };
  };

  var processOptions = function(options) {
    // Resolve the document reference relative to the current document reference.
    if (!options.documentReference || typeof options.documentReference === 'string') {
      options.documentReference = XWiki.Model.resolve(options.documentReference, XWiki.EntityType.DOCUMENT,
        XWiki.currentDocument.documentReference);
    }
    // Resolve the search scope.
    options.searchScope = resolveEntityReference(options.searchScope || 'wiki:' + XWiki.currentWiki);
    return options;
  };

  /**
   * Resolves an entity reference from a string representation of the form "entityType:entityReference".
   */
  var resolveEntityReference = function(typeAndReference) {
    if (typeof typeAndReference === 'string') {
      var separatorIndex = typeAndReference.indexOf(':');
      if (separatorIndex > 0) {
        var entityType = XWiki.EntityType.byName(typeAndReference.substr(0, separatorIndex));
        return XWiki.Model.resolve(typeAndReference.substr(separatorIndex + 1), entityType);
      } else {
        return null;
      }
    }
    return typeAndReference;
  };

  var loadAttachments = function(text, options) {
    var searchScope = options.searchScope;
    // If the user didn't type any text and the specified document is inside the search scope then..
    if (text === '' && (!searchScope || options.documentReference.hasParent(searchScope))) {
      // ..take the default list of suggestions from the specified document (usually the current document).
      searchScope = options.documentReference;
    }
    var attachmentsRestURL = getAttachmentsRestURL(searchScope);
    return $.getJSON(attachmentsRestURL, {
      name: text,
      number: options.maxOptions
    }).then($.proxy(processAttachments, null, options));
  };

  /**
   * Returns the REST URL that can be used to search or retrieve the attachments located inside the specified entity.
   */
  var getAttachmentsRestURL = function(entityReference) {
    var path = ['$request.contextPath', 'rest'];
    entityReference.getReversedReferenceChain().forEach(function(reference) {
      var restResourceType = reference.type === XWiki.EntityType.DOCUMENT ? 'page' :
        XWiki.EntityType.getName(reference.type);
      path.push(restResourceType + 's', encodeURIComponent(reference.name));
    });
    // If the specified entity is an attachment then return the URL to retrieve the attachment metadata.
    if (entityReference.type === XWiki.EntityType.ATTACHMENT) {
      path.push('metadata');
    } else {
      path.push('attachments');
    }
    return path.join('/') + '?prettyNames=true';
  };

  var loadAttachment = function(value, options) {
    var attachmentReference = getAttachmentReferenceFromValue(value, options);
    return $.getJSON(getAttachmentsRestURL(attachmentReference)).then($.proxy(processAttachment, null, options));
  };

  /**
   * Adapt the JSON returned by the REST call to the format expected by the Selectize widget.
   */
  var processAttachments = function(options, response) {
    if ($.isArray(response.attachments)) {
      return response.attachments.map($.proxy(processAttachment, null, options));
    } else {
      return [];
    }
  };

  var processAttachment = function(options, attachment) {
    var attachmentReference = XWiki.Model.resolve(attachment.id, XWiki.EntityType.ATTACHMENT);
    return {
      label: attachment.name,
      value: getAttachmentValueFromReference(attachmentReference, options),
      url: attachment.xwikiRelativeUrl,
      icon: getAttachmentIcon(attachment),
      hint: getAttachmentHint(attachment),
      data: attachment
    };
  };

  /**
   * Resolve the attachment reference from the value of the Selectize widget.
   */
  var getAttachmentReferenceFromValue = function(value, options) {
    if (options.useAttachmentName && options.searchScope && options.searchScope.type === XWiki.EntityType.DOCUMENT) {
      // The value is the attachment name.
      return new XWiki.AttachmentReference(value, options.searchScope);
    } else {
      // The value is the serialized (relative) attachment reference.
      return XWiki.Model.resolve(value, XWiki.EntityType.ATTACHMENT, options.documentReference);
    }
  };

  /**
   * Return the Selectize widget value that corresponds to the given attachment reference.
   */
  var getAttachmentValueFromReference = function(attachmentReference, options) {
    if (options.useAttachmentName && options.searchScope && options.searchScope.type === XWiki.EntityType.DOCUMENT) {
      // The value is the attachment name.
      return attachmentReference.name;
    // The value is the serialized (relative) attachment reference.
    } else if (attachmentReference.parent.equals(options.documentReference)) {
      // Relative to the parent document.
      return XWiki.Model.serialize(attachmentReference.relativeTo(options.documentReference));
    } else {
      // Relative to the root wiki.
      return XWiki.Model.serialize(attachmentReference.relativeTo(options.documentReference.getRoot()));
    }
  };

  var getAttachmentIcon = function(attachment) {
    if (typeof attachment.mimeType === 'string' && attachment.mimeType.substr(0, 6) === 'image/') {
      return {
        iconSetType: 'IMAGE',
        url: attachment.xwikiRelativeUrl + 'width=48'
      };
    } else {
      return getIcon(attachment.mimeType, attachment.name);
    }
  };

  /**!
   * #set ($discard = "#mimetypeimg('' '')")
   * #set ($discard = $mimetypeMap.put('attachment', ['attach', 'attachment']))
   * #foreach ($map in [$mimetypeMap, $extensionMap])
   *   #foreach ($entry in $map.entrySet())
   *     #set ($discard = $entry.value.set(0, $services.icon.getMetaData($entry.value.get(0))))
   *     #set ($translationKey = "core.viewers.attachments.mime.$entry.value.get(1)")
   *     #set ($discard = $entry.value.set(1, $services.localization.render($translationKey)))
   *   #end
   * #end
   */
  var mimeTypeMap = $jsontool.serialize($mimetypeMap);
  var extensionMap = $jsontool.serialize($extensionMap);

  var getIcon = function(mimeType, fileName) {
    var extension = fileName.substr(fileName.lastIndexOf('.') + 1);
    if (mimeTypeMap.hasOwnProperty(mimeType)) {
      return mimeTypeMap[mimeType][0];
    } else if (extensionMap.hasOwnProperty(extension)) {
      return extensionMap[extension][0];
    } else {
      var mimeTypePrefix = mimeType.substr(0, mimeType.indexOf('/') + 1);
      if (mimeTypeMap.hasOwnProperty(mimeTypePrefix)) {
        return mimeTypeMap[mimeTypePrefix][0];
      } else {
        return mimeTypeMap['attachment'][0];
      }
    }
  };

  var getAttachmentHint = function(attachment) {
    return attachment.hierarchy.items.filter(function(item) {
      return item.type === 'space' || (item.type === 'document' && item.name !== 'WebHome');
    }).map(function(item) {
      return item.label;
    }).join(' / ');
  };

  $.fn.suggestAttachments = function(options) {
    return this.each(function() {
      var actualOptions = $.extend(getSelectizeOptions($(this)), options);
      $(this).xwikiSelectize(processOptions(actualOptions));
    });
  };
});

require(['jquery', 'xwiki-suggestAttachments', 'xwiki-events-bridge'], function($) {
  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.suggest-attachments').suggestAttachments();
  };

  $(document).on('xwiki:dom:updated', init);
  XWiki.domIsLoaded && init();
});
