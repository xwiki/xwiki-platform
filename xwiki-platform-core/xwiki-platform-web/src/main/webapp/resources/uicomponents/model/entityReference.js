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
var XWiki = (function (XWiki) {
// Start XWiki augmentation.

var ordinal = 0;
XWiki.EntityType = {
  WIKI: ordinal++,
  SPACE: ordinal++,
  DOCUMENT: ordinal++,
  ATTACHMENT: ordinal++,
  OBJECT: ordinal++,
  OBJECT_PROPERTY: ordinal++,
  CLASS_PROPERTY: ordinal++
};

var entityTypeNames = [];
for(var entityType in XWiki.EntityType) {
  if (XWiki.EntityType.hasOwnProperty(entityType)) {
    var index = XWiki.EntityType[entityType];
    var parts = entityType.toLowerCase().split('_');
    for (var i = 1; i < parts.length; i++) {
      parts[i] = parts[i].substr(0, 1).toUpperCase() + parts[i].substr(1);
    }
    entityTypeNames[index] = parts.join('');
  }
}
XWiki.EntityType.getName = function(entityType) {
  return entityTypeNames[entityType];
};
XWiki.EntityType.byName = function(name) {
  var lowerName = name.toLowerCase();
  for(var index = 0; index < entityTypeNames.length; index++) {
    if (entityTypeNames[index] === lowerName) {
      return index;
    }
  }
  return -1;
};

XWiki.EntityReference = Class.create({
  initialize: function(name, type, parent, locale) {
    this.name = name;
    this.type = type;
    this.parent = parent;
    this.locale = locale;
  },

  /**
   * Extract the entity of the given type from this one. This entity may be returned if it has the type requested.
   *
   * @param type the type of the entity to be extracted
   * @return the entity of the given type
   */
  extractReference: function(type) {
    var reference = this;
    while (reference && reference.type != type) {
      reference = reference.parent;
    }
    return reference;
  },

  /**
   * Extract the value identifying the entity with the given type from this reference. The name of this entity may be
   * returned if it has the type requested.
   *
   * @param type the type of the entity to be extracted
   * @return the value corresponding to the entity of the given type
   */
  extractReferenceValue: function(type) {
    var reference = this.extractReference(type);
    return reference ? reference.name : null;
  },

  /**
   * @return a new reference pointing to the same entity but relative to the given reference
   */
  relativeTo: function(baseReference) {
    var components = this.getReversedReferenceChain();
    var baseComponents = baseReference ? baseReference.getReversedReferenceChain() : [];
    var i = j = 0;
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
      for(i--; i > 0 && components[i].type === components[i - 1].type; i--);
    }
    var relativeReference;
    for (; i < components.length; i++) {
      relativeReference = new XWiki.EntityReference(components[i].name, components[i].type, relativeReference);
    }
    return relativeReference || new XWiki.EntityReference('', this.type);
  },

  getReversedReferenceChain: function() {
    var components = [];
    var reference = this;
    while (reference) {
      components.push(reference);
      reference = reference.parent;
    }
    return components.reverse();
  },

  getRoot: function() {
    var root = this;
    while (root.parent) {
      root = root.parent;
    }
    return root;
  },

  appendParent: function(parent) {
    this.getRoot().parent = parent;
    return this;
  },

  hasParent: function(expectedParent) {
    var actualParent = this.parent;
    // Handle the case when both the expectedParent and the actualParent are null.
    if (actualParent == expectedParent) {
      return true;
    }
    while (actualParent && !actualParent.equals(expectedParent)) {
      actualParent = actualParent.parent;
    }
    return actualParent != null;
  },

  equals: function(reference) {
      if (reference == null) {
          return false;
      }
      if ((this.parent == null && reference.parent != null) || (this.parent != null && reference.parent == null)) {
          return false;
      }
      return this.name === reference.name && this.type === reference.type
          && (this.parent == null || this.parent.equals(reference.parent));
  },

  toString : function() {
    return XWiki.Model.serialize(this);
  },

  getName : function() {
    return this.name;
  }
});

XWiki.EntityReference.fromJSONObject = function (object) {
  var parent;
  if (object.parent != null) {
    parent = XWiki.EntityReference.fromJSONObject(object.parent);
  } else {
    parent = undefined;
  }

  return new XWiki.EntityReference(object.name, XWiki.EntityType.byName(object.type), parent, object.locale);
};

XWiki.WikiReference = Class.create(XWiki.EntityReference, {
  initialize: function($super, wikiName) {
    $super(wikiName, XWiki.EntityType.WIKI);
  }
});

XWiki.SpaceReference = Class.create(XWiki.EntityReference, {
  initialize: function($super, wikiName, spaceNames) {
    var wikiReference = new XWiki.WikiReference(wikiName);
    if (Array.isArray(spaceNames) && spaceNames.length > 0) {
      // Support passing an Array of Spaces (Nested Spaces)
      var reference = wikiReference;
      var i;
      for (i = 0; i < spaceNames.length - 1; ++i) {
          reference = new XWiki.EntityReference(spaceNames[i], XWiki.EntityType.SPACE, reference);
      }
      $super(spaceNames[i], XWiki.EntityType.SPACE, reference);
    } else if (typeof spaceNames === "string" || typeof spaceNames == 'undefined') {
      // Support passing a single space as a String for both backward-compatibility reason but also simplicity
      $super(spaceNames, XWiki.EntityType.SPACE, wikiReference);
    } else {
      throw 'Missing mandatory space name or invalid type for: [' + spaceNames + ']';
    }
  }
});

XWiki.DocumentReference = Class.create(XWiki.EntityReference, {
  initialize: function($super, wikiName, spaceNames, pageName) {
    $super(pageName, XWiki.EntityType.DOCUMENT, new XWiki.SpaceReference(wikiName, spaceNames));
  }
});

XWiki.AttachmentReference = Class.create(XWiki.EntityReference, {
  initialize: function($super, fileName, documentReference) {
    $super(fileName, XWiki.EntityType.ATTACHMENT, documentReference);
  }
});

XWiki.EntityReferenceTreeNode = Class.create({
  initialize: function() {
    this.children = {};
    this.locales = {};
  },

  _fromJSONObject: function(node) {
    // Reference
    if (node.reference != null) {
      this.reference = XWiki.EntityReference.fromJSONObject(node.reference);
    }

    // Children
    node.children.each(this._childFromJSONObject.bind(this));

    // Locales
    node.locales.each(this._localeFromJSONObject.bind(this));
  },

  _childFromJSONObject: function(jsonNode) {
    var node = new XWiki.EntityReferenceTreeNode();
    node._fromJSONObject(jsonNode);

    // For the reference name, you can have both a space and a document as children.
    var childrenByType = this.children[node.reference.name];
    if (typeof childrenByType == 'undefined') {
      childrenByType = {};
      this.children[node.reference.name] = childrenByType;
    }

    // Add the child node including its type.
    childrenByType[node.reference.type] = node;
  },

  _localeFromJSONObject: function(jsonReference) {
    this.locales[jsonReference.locale] = XWiki.EntityReference.fromJSONObject(jsonReference);
  },

  /**
   * @return true if the node contains children
   */
  hasChildren: function() {
    return Object.keys(this.children).length !== 0;
  },

  /**
   * @return true if the node contains locales
   */
  hasLocales: function() {
    return Object.keys(this.locales).length !== 0;
  },

  /**
   * @param referencePath a path in the tree starting from this node, specified as an EntityReference
   * @return the node associated to the specified path
   */
  getChildByReference: function(referencePath) {
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
});

XWiki.EntityReferenceTree = Class.create(XWiki.EntityReferenceTreeNode, {
});

XWiki.EntityReferenceTree.fromJSONObject = function (object) {
   var tree = new XWiki.EntityReferenceTree();
   tree._fromJSONObject(object);

   return tree;
};

var ESCAPE = '\\';
var DBLESCAPE = ESCAPE + ESCAPE;
var WIKISEP = ':';
var SPACESEP = '.';
var ATTACHMENTSEP = '@';
var OBJECTSEP = '^';
var PROPERTYSEP = SPACESEP;
var CLASSPROPSEP = OBJECTSEP;

var ESCAPES = [
  /* WIKI */            [],
  /* SPACE */           [SPACESEP, WIKISEP, ESCAPE],
  /* DOCUMENT */        [SPACESEP, ESCAPE],
  /* ATTACHMENT */      [ATTACHMENTSEP, ESCAPE],
  /* OBJECT */          [OBJECTSEP, ESCAPE],
  /* OBJECT_PROPERTY */ [PROPERTYSEP, ESCAPE],
  /* CLASS_PROPERTY */  [CLASSPROPSEP, SPACESEP, ESCAPE]
];

var REPLACEMENTS = [
  /* WIKI */            [],
  /* SPACE */           [ESCAPE + SPACESEP, ESCAPE + WIKISEP, DBLESCAPE],
  /* DOCUMENT */        [ESCAPE + SPACESEP, DBLESCAPE],
  /* ATTACHMENT */      [ESCAPE + ATTACHMENTSEP, DBLESCAPE],
  /* OBJECT */          [ESCAPE + OBJECTSEP, DBLESCAPE],
  /* OBJECT_PROPERTY */ [ESCAPE + PROPERTYSEP, DBLESCAPE],
  /* CLASS_PROPERTY */  [ESCAPE + CLASSPROPSEP, ESCAPE + SPACESEP, DBLESCAPE]
];

var SEPARATORS = [
  /* WIKI */            [],
  /* SPACE */           [SPACESEP, WIKISEP],
  /* DOCUMENT */        [SPACESEP, WIKISEP],
  /* ATTACHMENT */      [ATTACHMENTSEP, SPACESEP, WIKISEP],
  /* OBJECT */          [OBJECTSEP, SPACESEP, WIKISEP],
  /* OBJECT_PROPERTY */ [PROPERTYSEP, OBJECTSEP, SPACESEP, WIKISEP],
  /* CLASS_PROPERTY */  [CLASSPROPSEP, SPACESEP, WIKISEP]
];

var DEFAULT_PARENT = [
  /* WIKI */            null,
  /* SPACE */           XWiki.EntityType.WIKI,
  /* DOCUMENT */        XWiki.EntityType.SPACE,
  /* ATTACHMENT */      XWiki.EntityType.DOCUMENT,
  /* OBJECT */          XWiki.EntityType.DOCUMENT,
  /* OBJECT_PROPERTY */ XWiki.EntityType.OBJECT,
  /* CLASS_PROPERTY */  XWiki.EntityType.DOCUMENT
];

var REFERENCE_SETUP = [];
// Skip the WIKI entity type because it doesn't require special handling.
for (var i = 1; i < SEPARATORS.length ; i++) {
  REFERENCE_SETUP[i] = {};
}
REFERENCE_SETUP[XWiki.EntityType.SPACE][WIKISEP] = XWiki.EntityType.WIKI;
REFERENCE_SETUP[XWiki.EntityType.SPACE][SPACESEP] = XWiki.EntityType.SPACE;
REFERENCE_SETUP[XWiki.EntityType.DOCUMENT][SPACESEP] = XWiki.EntityType.SPACE;
REFERENCE_SETUP[XWiki.EntityType.ATTACHMENT][ATTACHMENTSEP] = XWiki.EntityType.DOCUMENT;
REFERENCE_SETUP[XWiki.EntityType.OBJECT][OBJECTSEP] = XWiki.EntityType.DOCUMENT;
REFERENCE_SETUP[XWiki.EntityType.OBJECT_PROPERTY][PROPERTYSEP] = XWiki.EntityType.OBJECT;
REFERENCE_SETUP[XWiki.EntityType.CLASS_PROPERTY][CLASSPROPSEP] = XWiki.EntityType.DOCUMENT;

/**
 * Map defining ordered entity types of a proper reference chain for a given entity type.
 */
var ENTITY_TYPES = [
  /* WIKI */            [XWiki.EntityType.WIKI],
  /* SPACE */           [XWiki.EntityType.SPACE, XWiki.EntityType.WIKI],
  /* DOCUMENT */        [XWiki.EntityType.DOCUMENT, XWiki.EntityType.SPACE, XWiki.EntityType.WIKI],
  /* ATTACHMENT */      [XWiki.EntityType.ATTACHMENT, XWiki.EntityType.DOCUMENT, XWiki.EntityType.SPACE, XWiki.EntityType.WIKI],
  /* OBJECT */          [XWiki.EntityType.OBJECT, XWiki.EntityType.DOCUMENT, XWiki.EntityType.SPACE, XWiki.EntityType.WIKI],
  /* OBJECT_PROPERTY */ [XWiki.EntityType.OBJECT_PROPERTY, XWiki.EntityType.OBJECT, XWiki.EntityType.DOCUMENT, XWiki.EntityType.SPACE, XWiki.EntityType.WIKI],
  /* CLASS_PROPERTY */  [XWiki.EntityType.CLASS_PROPERTY, XWiki.EntityType.DOCUMENT, XWiki.EntityType.SPACE, XWiki.EntityType.WIKI]
];

var ESCAPE_MATCHING = [DBLESCAPE, ESCAPE];
var ESCAPE_MATCHING_REPLACE = [ESCAPE, ''];

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

XWiki.EntityReferenceResolver = Class.create({
  resolve: function(value, type, defaultValueProvider) {
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
  },

  _getEscapedReference: function(representation, type, defaultValueProvider) {
    if (representation.length > 0) {
      var name = replaceEach(representation.join(''), ESCAPE_MATCHING, ESCAPE_MATCHING_REPLACE);
      return new XWiki.EntityReference(name, type);
    } else {
      return this._resolveDefaultReference(type, defaultValueProvider);
    }
  },

  _getNewReference: function(index, representation, type, defaultValueProvider) {
    // Found a valid separator (not escaped), separate content on its left from content on its right.
    var reference;
    if (index < representation.length - 1) {
      var name = representation.slice(index + 1).join('');
      reference = new XWiki.EntityReference(name, type);
    } else {
      reference = this._resolveDefaultReference(type, defaultValueProvider);
    }
    representation.splice(Math.max(index, 0), representation.length);
    return reference;
  },

  _resolveDefaultReference: function(type, defaultValueProvider) {
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
        reference = new XWiki.EntityReference(name[i], type, reference);
      }
    }
    return reference;
  },

  _appendNewReference: function(reference, parent) {
    if (parent) {
      if (reference) {
        return reference.appendParent(parent);
      } else {
        return parent;
      }
    } else {
      return reference;
    }
  },

  _getNumberOfCharsBefore: function(character, representation, currentPosition) {
    var position = currentPosition;
    while (position >= 0 && representation[position] === character) {
      --position;
    }
    return currentPosition - position;
  }
});

XWiki.EntityReferenceSerializer = Class.create({
  serialize: function(entityReference) {
    return entityReference ? this.serialize(entityReference.parent) + this._serializeComponent(entityReference) : '';
  },
  _serializeComponent : function(entityReference) {
    var representation = '';
    var escapes = ESCAPES[entityReference.type];

    // Add the separator if this is not the first component.
    if (entityReference.parent) {
      representation += entityReference.parent.type === XWiki.EntityType.WIKI ? WIKISEP : escapes[0];
    }

    // The root reference doesn't have to escape its separator because the reference is parsed from the right.
    if (escapes.length > 0) {
      representation += replaceEach(entityReference.name, escapes, REPLACEMENTS[entityReference.type]);
    } else {
      representation += entityReference.name.replace(ESCAPE, DBLESCAPE);
    }

    return representation;
  }
});

var resolver = new XWiki.EntityReferenceResolver();
var serializer = new XWiki.EntityReferenceSerializer();

XWiki.Model = {
  serialize: function(entityReference) {
    return serializer.serialize(entityReference);
  },
  resolve: function(representation, entityType, defaultValueProvider) {
    return resolver.resolve(representation, entityType, defaultValueProvider);
  }
};

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
