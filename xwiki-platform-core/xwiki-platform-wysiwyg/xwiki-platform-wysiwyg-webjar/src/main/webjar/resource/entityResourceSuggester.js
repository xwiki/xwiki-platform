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
define('xwiki-wysiwyg-entity-resource-suggester-translation-keys', {
  prefix: 'entityResourceSuggester.',
  keys: [
    'doc.placeholder',
    'attach.placeholder',
    'create.resolvedInSpace',
    'create.fullyResolvedInput'
  ]
});

define('xwiki-wysiwyg-entity-resource-suggester', [
  'jquery',
  'xwiki-wysiwyg-resource',
  'xwiki-l10n!xwiki-wysiwyg-entity-resource-suggester-translation-keys'
], function($, $resource, translations) {
  'use strict';

  async function search(query, input, expectedEntityType, base, withCreateSuggestions) {
    let suggestions = [];
    try {
      const results = await $.post(new XWiki.Document('SuggestSolrService', 'XWiki').getURL('get'), {
        outputSyntax: 'plain',
        media: 'json',
        language: $('html').attr('lang'),
        query: query.join('\n'),
        input: input,
        nb: 8
      });

      let containsExactMatch = false;
      if (Array.isArray(results)) {
        let inputReference = XWiki.Model.resolve(input, expectedEntityType);
        let inputResourceReference = $resource.convertEntityReferenceToResourceReference(inputReference, base);
        for (let result of results) {
          const resource = convertSearchResultToResource(result, base);
          containsExactMatch = containsExactMatch || isSameResource(resource.reference, inputResourceReference);
          suggestions.push(resource);
        }
      } else {
        console.error("Unexpected search response: ", results);
      }

      if (!containsExactMatch && withCreateSuggestions) {
        suggestions = await addCreateDocumentSuggestions(input, base, suggestions);
      }
    } catch (error) {
      console.error("Failed to get suggestions.", error);
    }

    return adaptSuggestions(suggestions);
  }

  function isSameResource(resource1, resource2) {
    return JSON.stringify(resource1) === JSON.stringify(resource2);
  }

  async function addCreateDocumentSuggestions(input, base, suggestions) {
    try {
      const serviceReference = XWiki.Model.resolve('XWiki.WYSIWYG.LinkNameStrategyHelper', XWiki.EntityType.DOCUMENT,
        XWiki.currentDocument.documentReference);
      const data = await $.post(new XWiki.Document(serviceReference).getURL('get'), {
        outputSyntax: 'plain',
        input: input,
        base: XWiki.Model.serialize(base),
        action: 'suggest'
      });
      data.forEach(item => {
        if (item.type === 'exactMatch') {
          suggestions = handleExactMatch(item, base, suggestions);
        } else {
          suggestions.push(createDocumentFromLinkNameStrategyHelperResult(item, base));
        }
      });
    } catch (error) {
      console.error("Failed to get the page name strategy", error);
    }

    return suggestions;
  }

  function handleExactMatch(item, base, suggestions) {
    let entityReference = XWiki.Model.resolve(item.reference, XWiki.EntityType.DOCUMENT);
    let resourceReference = $resource.convertEntityReferenceToResourceReference(entityReference, base);
    suggestions = suggestions.filter(suggestion => !isSameResource(suggestion.reference, resourceReference));
    suggestions.unshift(createDocumentFromLinkNameStrategyHelperResult(item, base));
    return suggestions;
  }

  // The kinds of suggestions we can preselect: either the page already exists, or it's the page that would be created
  // from the given text, inside the base page. We never preselect 'fullyResolvedInput' because it interprets the given
  // text as a resource reference (e.g. dots become space separators), which is wrong for a free text label like
  // "Version 1.2", and because it is suggested only to advanced users.
  const preselectableCreationTypes = ['exactMatch', 'resolvedInSpace'];

  /**
   * Compute the document resource that corresponds to the given free text, following the configured page name
   * strategy: either the page that already exists or the page that would be created. This is the resource behind the
   * "Create new page..." suggestion, computed without searching for existing pages.
   *
   * @param label the free text to compute the resource from, e.g. the text selected in the rich text area; it is not
   *          interpreted as a resource reference
   * @param base the entity reference relative to which the resource is computed
   * @return a promise resolved with the resource to select, or with undefined when there's nothing to suggest for the
   *          given text (it's empty, it doesn't lead to a valid page name, or the request failed)
   */
  async function suggestNewDocument(label, base) {
    // Normalize the white space because the given text may span multiple lines.
    const input = (label || '').trim().replace(/\s+/g, ' ');
    if (!input) {
      return;
    }
    // Note that addCreateDocumentSuggestions() doesn't reject: it logs the error and returns the suggestions it has.
    const suggestions = await addCreateDocumentSuggestions(input, base, []);
    const suggestion = suggestions.find(candidate => preselectableCreationTypes.includes(candidate.creationType) &&
      // The page name strategy may transform the given text into an empty name, e.g. when it has only punctuation.
      getEntityName(candidate.entityReference));
    // The suggestions computed by addCreateDocumentSuggestions() are not adapted yet, see search().
    return suggestion && adaptSuggestion(suggestion);
  }

  function createDocumentFromLinkNameStrategyHelperResult(item, base) {
    const entityReference = XWiki.Model.resolve(item.reference, XWiki.EntityType.DOCUMENT);
    const entityName = getEntityName(entityReference);
    let label = item.title;
    if (!label && item.type !== 'exactMatch') {
      label = translations.get('create.' + item.type);
    } else if (!label) {
      label = entityName;
    }
    return {
      reference: $resource.convertEntityReferenceToResourceReference(entityReference, base),
      entityReference,
      label,
      toCreate: true,
      // The kind of suggestion computed by the page name strategy: 'exactMatch' (the page already exists),
      // 'resolvedInSpace' (the page that would be created from the given text, inside the base page) or
      // 'fullyResolvedInput' (the page that would be created from the given text used as a resource reference).
      creationType: item.type,
      // Use a different label when this resource to be created is selected.
      labelWhenSelected: entityName,
      hint: item.location,
    };
  }

  function convertSearchResultToResource(result, base) {
    const entityReference = XWiki.Model.resolve(result.reference);
    return {
      reference: $resource.convertEntityReferenceToResourceReference(entityReference, base),
      entityReference,
      label: result.title_ || getEntityName(entityReference),
      hint: result.location
    };
  }

  function resolve(resource, base) {
    const resourceTypeDefinition = $resource.types[resource.reference.type];
    if (resourceTypeDefinition.entityType && !resource.entityReference) {
      resource.entityReference = $resource.convertResourceReferenceToEntityReference(resource.reference, base);
    }
    return adaptSuggestion(resource);
  }

  function adaptSuggestions(suggestions) {
    return suggestions.map(adaptSuggestion);
  }

  function adaptSuggestion(suggestion) {
    suggestion.value = XWiki.Model.serialize(suggestion.entityReference);
    // Use a dedicated search field to avoid matching 'WebHome' for existing document resources.
    suggestion.searchValue = suggestion.toCreate ? suggestion.value : getEntityName(suggestion.entityReference);
    suggestion.icon = $resource.types[suggestion.reference.type]?.icon || $resource.types.unknown.icon;
    suggestion.label = suggestion.label || suggestion.searchValue;
    suggestion.hint = suggestion.hint?.trim() || '';
    return suggestion;
  }

  function getEntityName(entityReference) {
    let name = entityReference.name;
    if (entityReference.name === 'WebHome' && entityReference.type === XWiki.EntityType.DOCUMENT) {
      name = entityReference.parent.name;
    }
    return name;
  }

  function getEntityParent(entityReference) {
    let parent = entityReference.parent;
    if (entityReference.name === 'WebHome' && entityReference.type === XWiki.EntityType.DOCUMENT) {
      parent = parent?.parent;
    }
    return parent;
  }

  // Fetch only the fields needed to identify and locate the resource.
  const fieldList = 'fl=type,reference,wiki,spaces,name,filename,class,number,propertyname,doclocale,title_,location';

  async function retrieveSelected(resourceReference, base) {
    const entityType = XWiki.EntityType.byName($resource.types[resourceReference.type].entityType);
    const entityTypeName = XWiki.EntityType.getName(entityType);
    const query = [
      'q="__INPUT__"',
      'qf=reference',
      fieldList
    ];
    let entityReference = `${entityTypeName}:${resourceReference.reference}`;
    const suggestions = await search(query, entityReference, entityType, base, false);
    if (!suggestions.length) {
      entityReference = XWiki.Model.resolve(resourceReference.reference, entityType, base);
      suggestions.push(adaptSuggestion({
        reference: $resource.convertEntityReferenceToResourceReference(entityReference, base),
        entityReference,
        hint: getEntityParent(entityReference)?.relativeTo(base.extractReference(XWiki.EntityType.WIKI))
          .getReversedReferenceChain().map(reference => reference.name).join(' / ')
      }));
    }
    return suggestions;
  }

  const advancedSearchPattern = /[+\-!(){}[\]^"~*?:\\]+/;

  $resource.types.doc.placeholder = translations.get('doc.placeholder');
  $resource.suggesters.doc = {
    retrieve: function(resourceReference, base) {
      const query = [
        'q=__INPUT__',
        'fq=type:DOCUMENT',
        fieldList
      ];
      let input = resourceReference.reference.trim();
      const withCreateSuggestions = input.length > 0;
      if (input) {
        query.push('qf=title^2 name');
        if (!advancedSearchPattern.test(input)) {
          query[0] = 'q=(__INPUT__)^2 __INPUT__*';
        }
      } else {
        // Recently modified pages.
        input = '*:*';
        query.push('sort=date desc');
      }
      return search(query, input, XWiki.EntityType.DOCUMENT, base, withCreateSuggestions);
    },
    retrieveSelected,
    resolve,
    suggestNew: suggestNewDocument
  };

  $resource.types.attach.placeholder = translations.get('attach.placeholder');
  $resource.suggesters.attach = {
    retrieve: function(resourceReference, base) {
      const query = [
        'q=__INPUT__',
        'fq=type:ATTACHMENT',
        fieldList
      ];
      let input = resourceReference.reference.trim();
      if (input) {
        query.push('qf=filename');
        if (!advancedSearchPattern.test(input)) {
          query[0] = 'q=(__INPUT__)^2 __INPUT__*';
        }
      } else {
        // Recently modified attachments.
        input = '*:*';
        query.push('sort=attdate_sort desc');
      }
      return search(query, input, XWiki.EntityType.ATTACHMENT, base, false);
    },
    retrieveSelected,
    resolve
  };
});
