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
  for(var index = 0; index < entityTypeNames.length; index++) {
    if (entityTypeNames[index] === name) {
      return index;
    }
  }
  return -1;
};

XWiki.EntityReference = Class.create({
  initialize: function(name, type, parent) {
    this.name = name;
    this.type = type;
    this.parent = parent;
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
    var components = this._extractComponents().reverse();
    var baseComponents = baseReference ? baseReference._extractComponents().reverse() : [];
    while (components.length > 0 && baseComponents.length > 0 && components[0].type != baseComponents[0].type) {
      components[0].type > baseComponents[0].type ? baseComponents.shift() : components.shift();
    }
    while (components.length > 0 && baseComponents.length > 0 && components[0].type == baseComponents[0].type
      && components[0].name == baseComponents[0].name) {
      components.shift();
      baseComponents.shift();
    }
    if (components.length == 0) {
      return new XWiki.EntityReference('', this.type);
    } else {
      components = components.reverse();
      for (var i = 0; i < components.length; i++) {
        components[i] = new XWiki.EntityReference(components[i].name, components[i].type);
        if (i > 0) {
          components[i - 1].parent = components[i];
        }
      }
      return components[0];
    }
  },

  _extractComponents: function() {
    var components = [];
    var reference = this;
    while (reference) {
      components.push(reference);
      reference = reference.parent;
    }
    return components;
  }
});

XWiki.WikiReference = Class.create(XWiki.EntityReference, {
  initialize: function($super, wikiName) {
    $super(wikiName, XWiki.EntityType.WIKI);
  }
});

XWiki.SpaceReference = Class.create(XWiki.EntityReference, {
  initialize: function($super, wikiName, spaceName) {
    $super(spaceName, XWiki.EntityType.SPACE, new XWiki.WikiReference(wikiName));
  }
});

XWiki.DocumentReference = Class.create(XWiki.EntityReference, {
  initialize: function($super, wikiName, spaceName, pageName) {
    $super(pageName, XWiki.EntityType.DOCUMENT, new XWiki.SpaceReference(wikiName, spaceName));
  }
});

XWiki.AttachmentReference = Class.create(XWiki.EntityReference, {
  initialize: function($super, fileName, documentReference) {
    $super(fileName, XWiki.EntityType.ATTACHMENT, documentReference);
  }
});

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
  /* SPACE */           [WIKISEP],
  /* DOCUMENT */        [SPACESEP, WIKISEP],
  /* ATTACHMENT */      [ATTACHMENTSEP, SPACESEP, WIKISEP],
  /* OBJECT */          [OBJECTSEP, SPACESEP, WIKISEP],
  /* OBJECT_PROPERTY */ [PROPERTYSEP, OBJECTSEP, SPACESEP, WIKISEP],
  /* CLASS_PROPERTY */  [CLASSPROPSEP, SPACESEP, WIKISEP]
];

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
  var result = '', i = -1;
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
  resolve: function(representation, entityType) {
    representation = representation || '';
    entityType = parseInt(entityType);

    // First, check if the given entity type is valid.
    if (isNaN(entityType) || entityType < 0 || entityType >= SEPARATORS.length) {
      throw 'No parsing definition found for Entity Type [' + entityType + ']';
    }

    var reference;
    var separatorsForType = SEPARATORS[entityType];
    var entityTypesForType = ENTITY_TYPES[entityType];

    // Iterate over the representation string looking for separators in the correct order (rightmost separator first).
    // Note that the representation is nullified when we reach the start without hitting the current separator.
    for (var i = 0; i < separatorsForType.length && representation != null; i++) {
      var parts = this._splitAndUnescape(representation, separatorsForType[i]);
      representation = parts[0];
      var parent = new XWiki.EntityReference(parts[1], entityTypesForType[i]);
      reference = this._appendParent(reference, parent);
    }

    // Handle last entity reference's name.
    if (representation != null) {
      var name = replaceEach(representation, ESCAPE_MATCHING, ESCAPE_MATCHING_REPLACE);
      var parent = new XWiki.EntityReference(name, entityTypesForType[separatorsForType.length]);
      reference = this._appendParent(reference, parent);
    }

    return reference;
  },

  _appendParent: function(reference, parent) {
    if (reference) {
      var root = reference;
      while (root.parent) {
        root = root.parent;
      }
      root.parent = parent;
      return reference;
    } else {
      return parent;
    }
  },

  _splitAndUnescape: function(representation, separator) {
    var name = [];
    var i = representation.length;
    while (--i >= 0) {
      var currentChar = representation.charAt(i);
      var nextIndex = i - 1;
      var nextChar = 0;
      if (nextIndex >= 0) {
        nextChar = representation.charAt(nextIndex);
      }
      if (currentChar == separator) {
        var numberOfBackslashes = this._getNumberOfCharsBefore(ESCAPE, representation, nextIndex);
        if (numberOfBackslashes % 2 == 0) {
          // Found a valid separator (not escaped), separate content on its left from content on its right.
          break;
        } else {
          // Skip the escape character.
          --i;
        }
      } else if (nextChar == ESCAPE) {
        // Skip the escape character.
        --i;
      }
      name.push(currentChar);
    }

    return [i < 0 ? null : representation.substring(0, i), name.reverse().join('')];
  },

  _getNumberOfCharsBefore: function(character, representation, currentPosition) {
    var position = currentPosition;
    while (position >= 0 && representation.charAt(position) == character) {
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
      representation += entityReference.parent.type == XWiki.EntityType.WIKI ? WIKISEP : escapes[0];
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
  resolve: function(representation, entityType) {
    return resolver.resolve(representation, entityType);
  }
};

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));
