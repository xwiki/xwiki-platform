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
define('resourceTranslationKeys', [], [
  'attach.label',
  'attach.placeholder',
  'data.label',
  'doc.label',
  'doc.placeholder',
  'icon.label',
  'mailto.label',
  'mailto.placeholder',
  'path.label',
  'path.placeholder',
  'unc.label',
  'unc.placeholder',
  'unknown.label',
  'url.label',
  'url.placeholder',
  'user.label',
  'user.placeholder'
]);

define('resource', ['l10n!resource'], function(translations) {
  'use strict';

  var types = {
    attach: {
      label: translations.get('attach.label'),
      icon: 'glyphicon glyphicon-paperclip',
      placeholder: translations.get('attach.placeholder'),
      entityType: 'attachment'
    },
    data: {
      label: translations.get('data.label'),
      icon: 'glyphicon glyphicon-briefcase',
      placeholder: 'image/png;base64,AAAAAElFTkSuQmCC'
    },
    doc: {
      label: translations.get('doc.label'),
      icon: 'glyphicon glyphicon-file',
      placeholder: translations.get('doc.placeholder'),
      allowEmptyReference: true,
      entityType: 'document'
    },
    icon: {
      label: translations.get('icon.label'),
      icon: 'glyphicon glyphicon-flag',
      placeholder: 'help'
    },
    mailto: {
      label: translations.get('mailto.label'),
      icon: 'glyphicon glyphicon-envelope',
      placeholder: translations.get('mailto.placeholder')
    },
    path: {
      label: translations.get('path.label'),
      icon: 'glyphicon glyphicon-road',
      placeholder: translations.get('path.placeholder')
    },
    unc: {
      label: translations.get('unc.label'),
      icon: 'glyphicon glyphicon-hdd',
      placeholder: translations.get('unc.placeholder')
    },
    unknown: {
      label: translations.get('unknown.label'),
      icon: 'glyphicon glyphicon-question-sign',
      placeholder: ''
    },
    url: {
      label: translations.get('url.label'),
      icon: 'glyphicon glyphicon-globe',
      placeholder: translations.get('url.placeholder')
    },
    user: {
      label: translations.get('user.label'),
      icon: 'glyphicon glyphicon-user',
      placeholder: translations.get('user.placeholder')
    }
  };

  var entityTypeToResourceType = ['wiki', 'space', 'doc', 'attach'];
  var convertEntityReferenceToResourceReference = function(entityReference, baseEntityReference) {
    baseEntityReference = baseEntityReference || XWiki.currentDocument.getDocumentReference();
    return {
      type: entityTypeToResourceType[entityReference.type],
      // We know the target entity precisely (no ambiguity).
      typed: true,
      reference: getSerializedRelativeReference(entityReference, baseEntityReference)
    };
  };

  var getSerializedRelativeReference = function(entityReference, baseReference) {
    var relativeReference = entityReference.relativeTo(baseReference);
    var relativeRootReference = getRelativeRootReference(entityReference, relativeReference);
    if (relativeRootReference.parent && relativeRootReference.parent.type === relativeRootReference.type) {
      // The root of the relative reference is not complete: there are more components with the same type in the
      // absolute reference. Check if the entity type supports partial relative reference.
      if (entityReference.type === XWiki.EntityType.DOCUMENT) {
        // We need to prefix the serialized relative reference with the separator that corresponds to the root entity
        // type (SPACE). TODO: Don't hard-code the space separator.
        return '.' + XWiki.Model.serialize(relativeReference);
      } else {
        // We need to add the remaining components of the same type.
        do {
          relativeRootReference = relativeRootReference.parent;
        } while (relativeRootReference.parent && relativeRootReference.parent.type === relativeRootReference.type);
        var parent = relativeRootReference.parent;
        delete relativeRootReference.parent;
        var serializedRelativeReference = XWiki.Model.serialize(entityReference);
        relativeRootReference.parent = parent;
        return serializedRelativeReference;
      }
    } else {
      return XWiki.Model.serialize(relativeReference);
    }
  };

  var getRelativeRootReference = function(absoluteReference, relativeReference) {
    while (relativeReference.parent) {
      absoluteReference = absoluteReference.parent;
      relativeReference = relativeReference.parent;
    }
    return absoluteReference;
  };

  var convertResourceReferenceToEntityReference = function(resourceReference, baseEntityReference) {
    baseEntityReference = baseEntityReference || XWiki.currentDocument.getDocumentReference();
    var entityType = XWiki.EntityType.byName(types[resourceReference.type].entityType);
    return XWiki.Model.resolve(resourceReference.reference, entityType, baseEntityReference);
  };

  return {
    types: types,
    pickers: {},
    suggesters: {},
    displayers: {},
    convertEntityReferenceToResourceReference: convertEntityReferenceToResourceReference,
    convertResourceReferenceToEntityReference: convertResourceReferenceToEntityReference
  };
});
