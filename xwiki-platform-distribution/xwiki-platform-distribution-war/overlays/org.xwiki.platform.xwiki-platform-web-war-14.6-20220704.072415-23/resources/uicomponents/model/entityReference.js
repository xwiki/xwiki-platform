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
// We cannot make this a RequireJS module yet because it has to be loaded (synchronously) before xwiki.js which doesn't
// use RequireJS. We tried various things, like using the synchronous require('xwiki-entityReferene') call, but it did
// not work because the define('xwiki-entityReference') call is queued and thus the module is not available right away.
// See https://github.com/requirejs/requirejs/issues/241 .
var XWiki = (function (XWiki) {
'use strict';

var ordinal = 0;
const EntityType = {
  WIKI: ordinal++,
  SPACE: ordinal++,
  DOCUMENT: ordinal++,
  ATTACHMENT: ordinal++,
  OBJECT: ordinal++,
  OBJECT_PROPERTY: ordinal++,
  CLASS_PROPERTY: ordinal++
};

const entityTypeNames = [];
Object.keys(EntityType).forEach(entityType => {
  const parts = entityType.toLowerCase().split('_');
  entityTypeNames[EntityType[entityType]] = [parts[0]]
    // Capitalize all parts except for the first one.
    .concat(parts.slice(1).map(part => part.substr(0, 1).toUpperCase() + part.substr(1)))
    .join('');
});
EntityType.getName = function(entityType) {
  return entityTypeNames[entityType];
};
EntityType.byName = function(name) {
  var lowerName = name.toLowerCase();
  for(var index = 0; index < entityTypeNames.length; index++) {
    if (entityTypeNames[index].toLowerCase() === lowerName) {
      return index;
    }
  }
  return -1;
};

class EntityReference {
  constructor(name, type, parent, locale) {
    this.name = name;
    this.type = type;
    this.parent = parent;
    this.locale = locale;
  }

  /**
   * Extract the entity of the given type from this one. This entity may be returned if it has the type requested.
   *
   * @param type the type of the entity to be extracted
   * @return the entity of the given type
   */
  extractReference(type) {
    var reference = this;
    while (reference && reference.type != type) {
      reference = reference.parent;
    }
    return reference;
  }

  /**
   * Extract the value identifying the entity with the given type from this reference. The name of this entity may be
   * returned if it has the type requested.
   *
   * @param type the type of the entity to be extracted
   * @return the value corresponding to the entity of the given type
   */
  extractReferenceValue(type) {
    var reference = this.extractReference(type);
    return reference ? reference.name : null;
  }

  /**
   * @return a new reference pointing to the same entity but relative to the given reference
   */
  relativeTo(baseReference) {
    var components = this.getReversedReferenceChain();
    var baseComponents = baseReference ? baseReference.getReversedReferenceChain() : [];
    var i, j = i = 0;
    while (i < components.length && j < baseComponents.length && components[i].type != baseComponents[j].type) {
      components[i].type > baseComponents[j].type ? j++ : i++;
    }
    while (i < components.length && j < baseComponents.length && components[i].type === baseComponents[j].type
      && components[i].name === baseComponents[j].name) {
      i++;
      j++;
    }
    if (j < baseComponents.length && j > 0 && baseComponents[j].type === baseComponents[j - 1].type && i > 0) {
      // If the current base entity type has not been fully matched then we need to add back the previously matched entity.
      for(i--; i > 0 && components[i].type === components[i - 1].type; i--) {};
    }
    var relativeReference;
    for (; i < components.length; i++) {
      relativeReference = new EntityReference(components[i].name, components[i].type, relativeReference);
    }
    return relativeReference || new EntityReference('', this.type);
  }

  getReversedReferenceChain() {
    var components = [];
    var reference = this;
    while (reference) {
      components.push(reference);
      reference = reference.parent;
    }
    return components.reverse();
  }

  getRoot() {
    var root = this;
    while (root.parent) {
      root = root.parent;
    }
    return root;
  }

  appendParent(parent) {
    this.getRoot().parent = parent;
    return this;
  }

  hasParent(expectedParent) {
    var actualParent = this.parent;
    // Handle the case when both the expectedParent and the actualParent are null.
    if (actualParent == expectedParent) {
      return true;
    }
    while (actualParent && !actualParent.equals(expectedParent)) {
      actualParent = actualParent.parent;
    }
    return actualParent != null;
  }

  equals(reference) {
    if (reference == null) {
      return false;
    }
    if ((this.parent == null && reference.parent != null) || (this.parent != null && reference.parent == null)) {
      return false;
    }
    return this.name === reference.name && this.type === reference.type
      && (this.parent == null || this.parent.equals(reference.parent));
  }

  toString() {
    return Model.serialize(this);
  }

  getName() {
    return this.name;
  }

  static fromJSONObject(object) {
    var parent;
    if (object.parent != null) {
      parent = EntityReference.fromJSONObject(object.parent);
    } else {
      parent = undefined;
    }

    return new EntityReference(object.name, EntityType.byName(object.type), parent, object.locale);
  }
};

class WikiReference extends EntityReference {
  constructor(wikiName) {
    super(wikiName, EntityType.WIKI);
  }
};

class SpaceReference extends EntityReference {
  constructor(wikiName, spaceNames) {
    var wikiReference = new WikiReference(wikiName);
    if (Array.isArray(spaceNames) && spaceNames.length > 0) {
      // Support passing an Array of Spaces (Nested Spaces)
      var reference = wikiReference;
      var i;
      for (i = 0; i < spaceNames.length - 1; ++i) {
          reference = new EntityReference(spaceNames[i], EntityType.SPACE, reference);
      }
      super(spaceNames[i], EntityType.SPACE, reference);
    } else if (typeof spaceNames === "string" || typeof spaceNames == 'undefined') {
      // Support passing a single space as a String for both backward-compatibility reason but also simplicity
      super(spaceNames, EntityType.SPACE, wikiReference);
    } else {
      throw 'Missing mandatory space name or invalid type for: [' + spaceNames + ']';
    }
  }
};

class DocumentReference extends EntityReference {
  constructor(wikiName, spaceNames, pageName) {
    super(pageName, EntityType.DOCUMENT, new SpaceReference(wikiName, spaceNames));
  }
};

class AttachmentReference extends EntityReference {
  constructor(fileName, documentReference) {
    super(fileName, EntityType.ATTACHMENT, documentReference);
  }
};

class EntityReferenceTreeNode {
  constructor() {
    this.children = {};
    this.locales = {};
  }

  _fromJSONObject(node) {
    // Reference
    if (node.reference != null) {
      this.reference = EntityReference.fromJSONObject(node.reference);
    }

    // Children
    node.children.each(this._childFromJSONObject.bind(this));

    // Locales
    node.locales.each(this._localeFromJSONObject.bind(this));
  }

  _childFromJSONObject(jsonNode) {
    var node = new EntityReferenceTreeNode();
    node._fromJSONObject(jsonNode);

    // For the reference name, you can have both a space and a document as children.
    var childrenByType = this.children[node.reference.name];
    if (typeof childrenByType == 'undefined') {
      childrenByType = {};
      this.children[node.reference.name] = childrenByType;
    }

    // Add the child node including its type.
    childrenByType[node.reference.type] = node;
  }

  _localeFromJSONObject(jsonReference) {
    this.locales[jsonReference.locale] = EntityReference.fromJSONObject(jsonReference);
  }

  /**
   * @return true if the node contains children
   */
  hasChildren() {
    return Object.keys(this.children).length !== 0;
  }

  /**
   * @return true if the node contains locales
   */
  hasLocales() {
    return Object.keys(this.locales).length !== 0;
  }

  /**
   * @param referencePath a path in the tree starting from this node, specified as an EntityReference
   * @return the node associated to the specified path
   */
  getChildByReference(referencePath) {
    if (typeof referencePath == "undefined") {
      return null;
    }

    var descendant = this;
    var references = referencePath.getReversedReferenceChain();
    for (var i = 0; i < references.length; i++) {
      var element = references[i];

      var descendantByType = descendant.children[element.name];
      // Get the children with the same reference element name.
      if (typeof descendantByType == 'undefined') {
        return null;
      }

      // Get the child with the same reference element type.
      descendant = descendantByType[element.type];
      if (typeof descendant == "undefined") {
        return null;
      }
    }

    return descendant;
  }
};

class EntityReferenceTree extends EntityReferenceTreeNode {
  constructor() {
    super();
  }

  static fromJSONObject(object) {
    var tree = new EntityReferenceTree();
    tree._fromJSONObject(object);
    return tree;
  }
};

const ESCAPE = '\\';
const DBLESCAPE = ESCAPE + ESCAPE;
const WIKISEP = ':';
const SPACESEP = '.';
const ATTACHMENTSEP = '@';
const OBJECTSEP = '^';
const PROPERTYSEP = SPACESEP;
const CLASSPROPSEP = OBJECTSEP;

const ESCAPES = [
  /* WIKI */            [],
  /* SPACE */           [SPACESEP, WIKISEP, ESCAPE],
  /* DOCUMENT */        [SPACESEP, ESCAPE],
  /* ATTACHMENT */      [ATTACHMENTSEP, ESCAPE],
  /* OBJECT */          [OBJECTSEP, ESCAPE],
  /* OBJECT_PROPERTY */ [PROPERTYSEP, ESCAPE],
  /* CLASS_PROPERTY */  [CLASSPROPSEP, SPACESEP, ESCAPE]
];

const REPLACEMENTS = [
  /* WIKI */            [],
  /* SPACE */           [ESCAPE + SPACESEP, ESCAPE + WIKISEP, DBLESCAPE],
  /* DOCUMENT */        [ESCAPE + SPACESEP, DBLESCAPE],
  /* ATTACHMENT */      [ESCAPE + ATTACHMENTSEP, DBLESCAPE],
  /* OBJECT */          [ESCAPE + OBJECTSEP, DBLESCAPE],
  /* OBJECT_PROPERTY */ [ESCAPE + PROPERTYSEP, DBLESCAPE],
  /* CLASS_PROPERTY */  [ESCAPE + CLASSPROPSEP, ESCAPE + SPACESEP, DBLESCAPE]
];

const SEPARATORS = [
  /* WIKI */            [],
  /* SPACE */           [SPACESEP, WIKISEP],
  /* DOCUMENT */        [SPACESEP, WIKISEP],
  /* ATTACHMENT */      [ATTACHMENTSEP, SPACESEP, WIKISEP],
  /* OBJECT */          [OBJECTSEP, SPACESEP, WIKISEP],
  /* OBJECT_PROPERTY */ [PROPERTYSEP, OBJECTSEP, SPACESEP, WIKISEP],
  /* CLASS_PROPERTY */  [CLASSPROPSEP, SPACESEP, WIKISEP]
];

const DEFAULT_PARENT = [
  /* WIKI */            null,
  /* SPACE */           EntityType.WIKI,
  /* DOCUMENT */        EntityType.SPACE,
  /* ATTACHMENT */      EntityType.DOCUMENT,
  /* OBJECT */          EntityType.DOCUMENT,
  /* OBJECT_PROPERTY */ EntityType.OBJECT,
  /* CLASS_PROPERTY */  EntityType.DOCUMENT
];

const REFERENCE_SETUP = [];
// Skip the WIKI entity type because it doesn't require special handling.
for (var i = 1; i < SEPARATORS.length ; i++) {
  REFERENCE_SETUP[i] = {};
}
REFERENCE_SETUP[EntityType.SPACE][WIKISEP] = EntityType.WIKI;
REFERENCE_SETUP[EntityType.SPACE][SPACESEP] = EntityType.SPACE;
REFERENCE_SETUP[EntityType.DOCUMENT][SPACESEP] = EntityType.SPACE;
REFERENCE_SETUP[EntityType.ATTACHMENT][ATTACHMENTSEP] = EntityType.DOCUMENT;
REFERENCE_SETUP[EntityType.OBJECT][OBJECTSEP] = EntityType.DOCUMENT;
REFERENCE_SETUP[EntityType.OBJECT_PROPERTY][PROPERTYSEP] = EntityType.OBJECT;
REFERENCE_SETUP[EntityType.CLASS_PROPERTY][CLASSPROPSEP] = EntityType.DOCUMENT;

const ESCAPE_MATCHING = [DBLESCAPE, ESCAPE];
const ESCAPE_MATCHING_REPLACE = [ESCAPE, ''];

function contains(text, position, subText) {
  for (var i = 0; i < subText.length; i++) {
    var j = position + i;
    if (j >= text.length || text.charAt(j) != subText.charAt(i)) {
      return false;
    }
  }
  return true;
}

function replaceEach(text, matches, replacements) {
  var i = -1;
  while (++i < text.length) {
    for (var j = 0; j < matches.length; j++) {
      if (contains(text, i, matches[j])) {
        text = text.substr(0, i) + replacements[j] + text.substr(i + matches[j].length);
        i += replacements[j].length - 1;
        break;
      }
    }
  }
  return text;
}

class EntityReferenceResolver {
  resolve(value, type, defaultValueProvider) {
    // Create a char array from the input string (as an equivalent to Java's StringBuilder).
    var representation = (value || '').split('');
    type = parseInt(type);

    // First, check if the given entity type is valid.
    if (isNaN(type) || type < 0 || type >= SEPARATORS.length) {
      throw 'No parsing definition found for Entity Type [' + type + ']';
    }

    var typeSetup = REFERENCE_SETUP[type];

    // Check if the specified entity type requires anything specific.
    if (!typeSetup) {
      return this._getEscapedReference(representation, type, defaultValueProvider);
    }

    var reference, currentType = type;
    do {
        // Search all characters for a non escaped separator. If found, then consider the part after the
        // character as the reference name and continue parsing the part before the separator.
        var parentType = null;
        var i = representation.length;
        while (--i >= 0) {
          var currentChar = representation[i];
          var nextIndex = i - 1;
          var nextChar = nextIndex < 0 ? 0 : representation[nextIndex];
          if (typeof typeSetup[currentChar] === 'number') {
            var numberOfBackslashes = this._getNumberOfCharsBefore(ESCAPE, representation, nextIndex);
            if (numberOfBackslashes % 2 === 0) {
              parentType = typeSetup[currentChar];
              break;
            } else {
              // Unescape the character.
              representation.splice(nextIndex, 1);
              --i;
            }
          } else if (nextChar === ESCAPE) {
            // Unescape the character.
            representation.splice(nextIndex, 1);
            --i;
          }
        }
        var parent = this._getNewReference(i, representation, currentType, defaultValueProvider);
        reference = this._appendNewReference(reference, parent);
        currentType = parentType ? parentType : DEFAULT_PARENT[currentType];
        typeSetup = REFERENCE_SETUP[currentType];
    } while (typeSetup != null);

    // Handle last entity reference's name.
    var root = this._getEscapedReference(representation, currentType, defaultValueProvider);
    reference = this._appendNewReference(reference, root);

    return reference;
  }

  _getEscapedReference(representation, type, defaultValueProvider) {
    if (representation.length > 0) {
      var name = replaceEach(representation.join(''), ESCAPE_MATCHING, ESCAPE_MATCHING_REPLACE);
      return new EntityReference(name, type);
    } else {
      return this._resolveDefaultReference(type, defaultValueProvider);
    }
  }

  _getNewReference(index, representation, type, defaultValueProvider) {
    // Found a valid separator (not escaped), separate content on its left from content on its right.
    var reference;
    if (index < representation.length - 1) {
      var name = representation.slice(index + 1).join('');
      reference = new EntityReference(name, type);
    } else {
      reference = this._resolveDefaultReference(type, defaultValueProvider);
    }
    representation.splice(Math.max(index, 0), representation.length);
    return reference;
  }

  _resolveDefaultReference(type, defaultValueProvider) {
    var name, reference;
    if (typeof defaultValueProvider === 'object') {
      if (defaultValueProvider && typeof defaultValueProvider.extractReference === 'function') {
        reference = defaultValueProvider.extractReference(type);
        name = [];
        // Extract all the reference components with the specified type.
        while (reference && reference.type === type) {
          name.push(reference.name);
          reference = reference.parent;
        }
        name.reverse();
      } else {
        name = defaultValueProvider && defaultValueProvider[type];
      }
    } else if (typeof defaultValueProvider === 'function') {
      name = defaultValueProvider(type);
    }
    reference = null;
    if (name && name.length > 0) {
      if (typeof name === 'string') {
        name = [name];
      }
      for (var i = 0; i < name.length; i++) {
        reference = new EntityReference(name[i], type, reference);
      }
    }
    return reference;
  }

  _appendNewReference(reference, parent) {
    if (parent) {
      if (reference) {
        return reference.appendParent(parent);
      } else {
        return parent;
      }
    } else {
      return reference;
    }
  }

  _getNumberOfCharsBefore(character, representation, currentPosition) {
    var position = currentPosition;
    while (position >= 0 && representation[position] === character) {
      --position;
    }
    return currentPosition - position;
  }
};

class EntityReferenceSerializer {
  serialize(entityReference) {
    return entityReference ? this.serialize(entityReference.parent) + this._serializeComponent(entityReference) : '';
  }

  _serializeComponent(entityReference) {
    var representation = '';
    var escapes = ESCAPES[entityReference.type];

    // Add the separator if this is not the first component.
    if (entityReference.parent) {
      representation += entityReference.parent.type === EntityType.WIKI ? WIKISEP : escapes[0];
    }

    // The root reference doesn't have to escape its separator because the reference is parsed from the right.
    if (escapes.length > 0) {
      representation += replaceEach(entityReference.name, escapes, REPLACEMENTS[entityReference.type]);
    } else {
      representation += entityReference.name.replace(ESCAPE, DBLESCAPE);
    }

    return representation;
  }
};

const resolver = new EntityReferenceResolver();
const serializer = new EntityReferenceSerializer();

const Model = {
  serialize: function(entityReference) {
    return serializer.serialize(entityReference);
  },
  resolve: function(representation, entityType, defaultValueProvider) {
    if (entityType == undefined && typeof representation === 'string') {
      // Try to extract the entity type from the representation.
      var separatorIndex = representation.indexOf(':');
      if (separatorIndex > 0) {
        entityType = EntityType.byName(representation.substring(0, separatorIndex));
        if (entityType >= 0) {
          representation = representation.substring(separatorIndex + 1);
        }
      }
    }
    return resolver.resolve(representation, entityType, defaultValueProvider);
  }
};

const api = {
  EntityType,
  EntityReference,
  WikiReference,
  SpaceReference,
  DocumentReference,
  AttachmentReference,
  EntityReferenceTreeNode,
  EntityReferenceTree,
  EntityReferenceResolver,
  EntityReferenceSerializer,
  Model
};

// Extend the XWiki API with the Entity Reference API.
Object.keys(api).forEach(key => XWiki[key] = api[key]);

return XWiki;
}(window.XWiki || {}));
