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
define('xwiki-wysiwyg-resource-translation-keys', {
  prefix: 'resource.',
  keys: [
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
  ]
});

define('xwiki-wysiwyg-resource-icons', [], {
  icons: ['attach', 'briefcase', 'page', 'flag', 'envelope', 'chart-organisation', 'drive', 'question', 'world', 'user']
});

define('xwiki-wysiwyg-resource', [
  'xwiki-l10n!xwiki-wysiwyg-resource-translation-keys',
  'xwiki-icon!xwiki-wysiwyg-resource-icons'
], function(translations, icons) {
  'use strict';

  const types = {
    attach: {
      label: translations.get('attach.label'),
      icon: icons.attach,
      placeholder: translations.get('attach.placeholder'),
      entityType: 'attachment',
      mustBeSelected: true
    },
    data: {
      label: translations.get('data.label'),
      icon: icons.briefcase,
      placeholder: 'image/png;base64,AAAAAElFTkSuQmCC'
    },
    doc: {
      label: translations.get('doc.label'),
      icon: icons.page,
      placeholder: translations.get('doc.placeholder'),
      allowEmptyReference: true,
      entityType: 'document',
      mustBeSelected: true
    },
    icon: {
      label: translations.get('icon.label'),
      icon: icons.flag,
      placeholder: 'help'
    },
    mailto: {
      label: translations.get('mailto.label'),
      icon: icons.envelope,
      placeholder: translations.get('mailto.placeholder')
    },
    path: {
      label: translations.get('path.label'),
      icon: icons['chart-organisation'],
      placeholder: translations.get('path.placeholder')
    },
    unc: {
      label: translations.get('unc.label'),
      icon: icons.drive,
      placeholder: translations.get('unc.placeholder')
    },
    unknown: {
      label: translations.get('unknown.label'),
      icon: icons.question,
      placeholder: ''
    },
    url: {
      label: translations.get('url.label'),
      icon: icons.world,
      placeholder: translations.get('url.placeholder')
    },
    user: {
      label: translations.get('user.label'),
      icon: icons.user,
      placeholder: translations.get('user.placeholder'),
      mustBeSelected: true
    }
  };

  const entityTypeToResourceType = ['wiki', 'space', 'doc', 'attach'];
  function convertEntityReferenceToResourceReference(entityReference, baseEntityReference) {
    baseEntityReference = baseEntityReference || XWiki.currentDocument.getDocumentReference();
    return {
      type: entityTypeToResourceType[entityReference.type],
      // We know the target entity precisely (no ambiguity).
      typed: true,
      reference: getSerializedRelativeReference(entityReference, baseEntityReference)
    };
  }

  function getSerializedRelativeReference(entityReference, baseReference) {
    const relativeReference = entityReference.relativeTo(baseReference);
    let relativeRootReference = getRelativeRootReference(entityReference, relativeReference);
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
        const parent = relativeRootReference.parent;
        delete relativeRootReference.parent;
        const serializedRelativeReference = XWiki.Model.serialize(entityReference);
        relativeRootReference.parent = parent;
        return serializedRelativeReference;
      }
    } else {
      return XWiki.Model.serialize(relativeReference);
    }
  }

  function getRelativeRootReference(absoluteReference, relativeReference) {
    while (relativeReference.parent) {
      absoluteReference = absoluteReference.parent;
      relativeReference = relativeReference.parent;
    }
    return absoluteReference;
  }

  function convertResourceReferenceToEntityReference(resourceReference, baseEntityReference) {
    baseEntityReference = baseEntityReference || XWiki.currentDocument.getDocumentReference();
    const entityType = XWiki.EntityType.byName(types[resourceReference.type].entityType);
    return XWiki.Model.resolve(resourceReference.reference, entityType, baseEntityReference);
  }

  return {
    types: types,
    pickers: {},
    suggesters: {},
    displayers: {},
    convertEntityReferenceToResourceReference,
    convertResourceReferenceToEntityReference
  };
});
