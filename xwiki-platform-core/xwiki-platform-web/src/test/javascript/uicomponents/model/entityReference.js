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
    'entityReference': 'uicomponents/model/entityReference',
    'prototype': 'js/prototype/prototype'
  },
  shim: {
    entityReference: ['prototype']
  }
});

define(['entityReference'], function() {

describe('EntityReference', function() {
  describe('Serializer', function() {
    function resolveAndSerialize() {
      var input = arguments[0];
      if (typeof arguments[1] === 'number') {
        var output = input;
        var entityType = arguments[1];
        var defaultValueProvider = arguments[2];
      } else {
        var output = arguments[1];
        var entityType = arguments[2];
        var defaultValueProvider = arguments[3];
      }
      expect(output).toEqual(XWiki.Model.serialize(XWiki.Model.resolve(input, entityType, defaultValueProvider)));
    }

    it('Wiki reference', function() {
      resolveAndSerialize('some:wiki', XWiki.EntityType.WIKI);
    });

    it('Space reference', function() {
      resolveAndSerialize('wiki:Al\\.ice.B\\\\ob.Ca\\:rol', XWiki.EntityType.SPACE);
    });

    it('Document reference', function() {
      resolveAndSerialize('wiki:path.to.page', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('wiki:path.to.', 'wiki:path.to.Success', XWiki.EntityType.DOCUMENT, [null, null, 'Success']);
      resolveAndSerialize('path.to.', 'the:path.to.Failure', XWiki.EntityType.DOCUMENT, ['the', null, 'Failure']);
      resolveAndSerialize('page', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('.', 'A.B', XWiki.EntityType.DOCUMENT, [null, 'A', 'B']);
      resolveAndSerialize(null, '', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('wiki1.wiki2:wiki3:space1.sp\\.ace2.sp\\:ace3.page', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('some\\.space.page', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('wiki:page', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('\\.:@\\.', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('\\\\:\\\\.\\\\', XWiki.EntityType.DOCUMENT);

      // The escaping here is not necessary but we want to test that it works.
      resolveAndSerialize('\\wiki:\\space.\\page', 'wiki:space.page', XWiki.EntityType.DOCUMENT);
    });

    it('Attachment reference', function() {
      resolveAndSerialize('wiki:space.page@filename', XWiki.EntityType.ATTACHMENT);
      resolveAndSerialize('', XWiki.EntityType.ATTACHMENT);
      resolveAndSerialize('wiki:space.page@my.png', XWiki.EntityType.ATTACHMENT);
      resolveAndSerialize('some:file.name', XWiki.EntityType.ATTACHMENT);
      resolveAndSerialize(':.\\@', XWiki.EntityType.ATTACHMENT);
    });

    it('Reference with child', function() {
      var reference = XWiki.Model.resolve('wiki:Space.Page', XWiki.EntityType.DOCUMENT);
      expect('wiki:Space').toEqual(XWiki.Model.serialize(reference.parent));
      expect('wiki').toEqual(XWiki.Model.serialize(reference.parent.parent));
    });

    it('Object reference', function() {
      resolveAndSerialize('wiki:space.page^Object', XWiki.EntityType.OBJECT);
      resolveAndSerialize('', XWiki.EntityType.OBJECT);

      // Property reference with no object.
      resolveAndSerialize('wiki:space.page.property', XWiki.EntityType.OBJECT);

      // Test escaping character.
      resolveAndSerialize('wiki:space.page^Obje\\^ct', XWiki.EntityType.OBJECT);
      resolveAndSerialize('wiki:spa^ce.page^Obje\\^ct', XWiki.EntityType.OBJECT);
      resolveAndSerialize(':.\\^@', XWiki.EntityType.OBJECT);
    });

    it('Object property reference', function() {
      resolveAndSerialize('wiki:space.page^xwiki.class[0].prop', XWiki.EntityType.OBJECT_PROPERTY);
      resolveAndSerialize('', XWiki.EntityType.OBJECT_PROPERTY);

      // Using separators.
      resolveAndSerialize('space^page@attachment', XWiki.EntityType.OBJECT_PROPERTY);
      resolveAndSerialize('wiki:space^object', XWiki.EntityType.OBJECT_PROPERTY);

      // Test escaping character.
      resolveAndSerialize('wiki:space.page^xwiki.class[0].prop\\.erty', XWiki.EntityType.OBJECT_PROPERTY);
      resolveAndSerialize(':\\.^@', XWiki.EntityType.OBJECT_PROPERTY);
    });

    it('Class property reference', function() {
      resolveAndSerialize('wiki:space.page^ClassProperty', XWiki.EntityType.CLASS_PROPERTY);
      resolveAndSerialize('', XWiki.EntityType.CLASS_PROPERTY);

      // Property reference with no object.
      var reference = XWiki.Model.resolve('wiki:space.page.property', XWiki.EntityType.CLASS_PROPERTY);
      expect('wiki:space\\.page\\.property').toEqual(XWiki.Model.serialize(reference));

      // Test escaping character.
      resolveAndSerialize('wiki:space.page^Obje\\^ct', XWiki.EntityType.CLASS_PROPERTY);
      resolveAndSerialize('wiki:spa^ce.page^Obje\\^ct', XWiki.EntityType.CLASS_PROPERTY);

      reference = XWiki.Model.resolve(':.\\^@', XWiki.EntityType.CLASS_PROPERTY);
      expect(':\\.\\^@').toEqual(XWiki.Model.serialize(reference));
    });

    it('Relative reference', function() {
      var reference = new XWiki.EntityReference('page', XWiki.EntityType.DOCUMENT)
      expect('page').toEqual(XWiki.Model.serialize(reference));

      reference = new XWiki.EntityReference('page', XWiki.EntityType.DOCUMENT, new XWiki.EntityReference('space', XWiki.EntityType.SPACE));
      expect('space.page').toEqual(XWiki.Model.serialize(reference));
    });
  });

  describe('Resolver', function() {
    var serialize = function(reference) {
      return reference.getReversedReferenceChain().map(function(reference) {
        return XWiki.EntityType.getName(reference.type).toUpperCase() + ':' + reference.name;
      }).join(' ');
    };

    var assertReference = function(expectedSerialization, actualReference) {
      expect(serialize(actualReference)).toEqual(expectedSerialization);
    };

    it('Document reference', function() {
      var reference = XWiki.Model.resolve('wiki:path.to.page', XWiki.EntityType.DOCUMENT);
      assertReference('WIKI:wiki SPACE:path SPACE:to DOCUMENT:page', reference);

      var baseReference = reference;
      reference = XWiki.Model.resolve('wiki:space.', XWiki.EntityType.DOCUMENT, baseReference);
      assertReference('WIKI:wiki SPACE:space DOCUMENT:page', reference);

      reference = XWiki.Model.resolve('', XWiki.EntityType.DOCUMENT, baseReference);
      assertReference('WIKI:wiki SPACE:path SPACE:to DOCUMENT:page', reference);

      reference = XWiki.Model.resolve('.one.Two', XWiki.EntityType.DOCUMENT, baseReference);
      assertReference('WIKI:wiki SPACE:path SPACE:to SPACE:one DOCUMENT:Two', reference);

      reference = XWiki.Model.resolve('one.Two', XWiki.EntityType.DOCUMENT, baseReference);
      assertReference('WIKI:wiki SPACE:one DOCUMENT:Two', reference);

      reference = XWiki.Model.resolve('space.', XWiki.EntityType.DOCUMENT, [null, null, 'Test']);
      assertReference('SPACE:space DOCUMENT:Test', reference);

      reference = XWiki.Model.resolve('page', XWiki.EntityType.DOCUMENT);
      assertReference('DOCUMENT:page', reference);

      reference = XWiki.Model.resolve('', XWiki.EntityType.DOCUMENT, function(type) {return 'X'});
      assertReference('WIKI:X SPACE:X DOCUMENT:X', reference);

      reference = XWiki.Model.resolve('.', XWiki.EntityType.DOCUMENT);
      expect(reference).toBeUndefined();

      reference = XWiki.Model.resolve(null, XWiki.EntityType.DOCUMENT);
      expect(reference).toBeUndefined();

      reference = XWiki.Model.resolve('', XWiki.EntityType.DOCUMENT);
      expect(reference).toBeUndefined();

      reference = XWiki.Model.resolve('wiki1.wiki2:wiki3:space1.sp\\.ace2.sp\\:ace3.page', XWiki.EntityType.DOCUMENT);
      assertReference('WIKI:wiki1.wiki2:wiki3 SPACE:space1 SPACE:sp.ace2 SPACE:sp:ace3 DOCUMENT:page', reference);

      reference = XWiki.Model.resolve('one.two.page', XWiki.EntityType.DOCUMENT);
      assertReference('SPACE:one SPACE:two DOCUMENT:page', reference);

      reference = XWiki.Model.resolve('wiki:page', XWiki.EntityType.DOCUMENT);
      assertReference('DOCUMENT:wiki:page', reference);

      // Resolve without entity type.

      var reference = XWiki.Model.resolve('document:wiki:path.to.page');
      assertReference('WIKI:wiki SPACE:path SPACE:to DOCUMENT:page', reference);

      reference = XWiki.Model.resolve('document:', null, baseReference);
      assertReference('WIKI:wiki SPACE:path SPACE:to DOCUMENT:page', reference);

      // Test escapes.

      reference = XWiki.Model.resolve('\\\\\\.:@\\.', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect(reference.extractReference(XWiki.EntityType.SPACE)).toBeUndefined();
      expect('\\.:@.').toEqual(reference.name);

      reference = XWiki.Model.resolve('some\\.space.page', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect('some.space').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('page').toEqual(reference.name);

      // Escaping characters are escaped.
      reference = XWiki.Model.resolve('\\\\:\\\\.\\\\', XWiki.EntityType.DOCUMENT);
      expect('\\').toEqual(reference.extractReference(XWiki.EntityType.WIKI).name);
      expect('\\').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('\\').toEqual(reference.name);

      reference = XWiki.Model.resolve('\\wiki:\\space.\\page', XWiki.EntityType.DOCUMENT);
      expect('wiki').toEqual(reference.extractReference(XWiki.EntityType.WIKI).name);
      expect('space').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('page').toEqual(reference.name);
    });
  });

  describe('Misc', function() {
    var attach = function(serializedReference) {
      return XWiki.Model.resolve(serializedReference, XWiki.EntityType.ATTACHMENT);
    };
    var doc = function(serializedReference) {
      return XWiki.Model.resolve(serializedReference, XWiki.EntityType.DOCUMENT);
    };
    var space = function(serializedReference) {
      return XWiki.Model.resolve(serializedReference, XWiki.EntityType.SPACE);
    };
    var wiki = function(wikiName) {
      return new XWiki.WikiReference(wikiName);
    };

    var assertRelativeTo = function(reference, baseReference, expected) {
      expect(XWiki.Model.serialize(reference.relativeTo(baseReference))).toEqual(expected);
    };

    var applyAssertRelativeTo = function(currentValue, index, array) {
      assertRelativeTo.apply(this, currentValue);
    };

    it('relativeTo', function() {
      [
        // Absolute base reference.
        [doc('wiki:Path.To.Page'), attach('wiki:Path.To.Page@file'), ''],
        [doc('wiki:Path.To.Page'), doc('wiki:Path.To.Page'), ''],
        [doc('wiki:Path.To.Page'), space('wiki:Path.To'), 'Page'],
        [doc('wiki:Path.To.Page'), space('wiki:Path'), 'To.Page'],
        [doc('wiki:Path.To.Page'), wiki('wiki'), 'Path.To.Page'],

        [doc('wiki:Path.To.Page'), wiki('xwiki'), 'wiki:Path.To.Page'],
        [doc('wiki:Path.To.Page'), doc('wiki:Some.Other.Page'), 'Path.To.Page'],
        [doc('wiki:Path.To.Page'), doc('wiki:Path.Of.Page'), 'Path.To.Page'],
        [doc('wiki:Path.To.Page'), doc('wiki:Path.Page'), 'To.Page'],
        [doc('wiki:Path.To.Page'), doc('wiki:Path.To.OtherPage'), 'Page'],
        [doc('wiki:Path.To.Page'), space('wiki:Path.Space'), 'Path.To.Page'],
        [doc('wiki:Path.To.Page'), space('wiki:Path.From.Page'), 'Path.To.Page'],

        [space('wiki:Path.To.Space'), doc('wiki:Path.To.Space.Home'), ''],
        [space('wiki:Path.To.Space'), doc('wiki:Path.To.Page'), 'Space'],
        [space('wiki:Path.To.Space'), doc('wiki:Path.Page'), 'To.Space'],
        [space('wiki:Path.To.Space'), doc('wiki:Path.Of.Page'), 'Path.To.Space'],

        [wiki('wiki'), doc('wiki:Path.To.Page'), ''],
        [wiki('wiki'), space('xwiki:Home'), 'wiki'],

        // Relative base reference.
        [doc('wiki:Path.To.Page'), attach('file'), ''],
        [doc('wiki:Path.To.Page'), attach('OtherPage@file'), 'Page'],
        [doc('wiki:Path.To.Page'), attach('Path.OtherPage@file'), 'To.Page'],
        [doc('wiki:Path.To.Page'), attach('Path.Of.Page@file'), 'Path.To.Page'],
        [doc('wiki:Path.To.Page'), doc('Page'), ''],
        [doc('wiki:Path.To.Page'), space('Path.To'), 'Page'],
        [doc('wiki:Path.To.Page'), space('Path'), 'To.Page'],
      ].map(applyAssertRelativeTo);
    });

    it('constructor', function() {
        // Construct a Nested Space reference
        var reference = new XWiki.SpaceReference('wiki', ['space1', 'space2']);
        expect(XWiki.Model.serialize(reference)).toEqual('wiki:space1.space2');
        reference = new XWiki.DocumentReference('wiki', ['space1', 'space2'], 'page');
        expect(XWiki.Model.serialize(reference)).toEqual('wiki:space1.space2.page');
        // Construct a non-Nested Space reference
        reference = new XWiki.SpaceReference('wiki', 'space');
        expect(XWiki.Model.serialize(reference)).toEqual('wiki:space');
        // Try passing non-valid space parameters
        expect(function() {new XWiki.SpaceReference('wiki', [])}).toThrow('Missing mandatory space name or invalid type for: []');
        expect(function() {new XWiki.SpaceReference('wiki', 12)}).toThrow('Missing mandatory space name or invalid type for: [12]');
    });

    it('equals', function() {
        var reference1 = new XWiki.DocumentReference('wiki', ['space1', 'space2'], 'page');
        var reference2 = new XWiki.DocumentReference('wiki', ['space1', 'space2'], 'page');
        var reference3 = new XWiki.DocumentReference('wiki2', ['space1', 'space2'], 'page');
        expect(reference1.equals(reference2)).toBe(true);
        expect(reference1.equals(reference3)).toBe(false);
    });

    it('hasParent', function() {
      var documentReference = new XWiki.DocumentReference('wiki', ['Path', 'To'], 'Page');

      expect(documentReference.hasParent(new XWiki.WikiReference('wiki'))).toBe(true);
      expect(documentReference.hasParent(new XWiki.SpaceReference('wiki', 'Path'))).toBe(true);
      expect(documentReference.hasParent(documentReference.parent)).toBe(true);

      expect((new XWiki.WikiReference('wiki')).hasParent(null)).toBe(true);

      expect(documentReference.hasParent(new XWiki.WikiReference('xwiki'))).toBe(false);
      expect(documentReference.hasParent(new XWiki.SpaceReference('wiki', 'To'))).toBe(false);
      expect(documentReference.hasParent(null)).toBe(false);
    });
  });
});

describe('EntityReferenceTree', function() {
  // Input JSON data.
  var jsonTree = {
    "reference":{"name":"xwiki","parent":null,"type":"WIKI"},
    "children":
      [
        {
          "reference":{"name":"Main","parent":{"name":"xwiki","parent":null,"type":"WIKI"},"type":"SPACE"},
          "children":
            [
              {
                "reference":{"name":"Page","parent":{"name":"Main","parent":{"name":"xwiki","parent":null,"type":"WIKI"},"type":"SPACE"},"type":"SPACE"},
                "children":
                  [
                    {
                      "reference":{"name":"WebHome","parent":{"name":"Page","parent":{"name":"Main","parent":{"name":"xwiki","parent":null,"type":"WIKI"},"type":"SPACE"},"type":"SPACE"},"type":"DOCUMENT","locale":""},
                      "children":[],
                      "locales":[{"name":"WebHome","parent":{"name":"Page","parent":{"name":"Main","parent":{"name":"xwiki","parent":null,"type":"WIKI"},"type":"SPACE"},"type":"SPACE"},"type":"DOCUMENT","locale":""}]
                    }
                  ],
               "locales":[]
              },
              {
                "reference":{"name":"Page","parent":{"name":"Main","parent":{"name":"xwiki","parent":null,"type":"WIKI"},"type":"SPACE"},"type":"DOCUMENT","locale":""},
                "children":[],
                "locales":[{"name":"Page","parent":{"name":"Main","parent":{"name":"xwiki","parent":null,"type":"WIKI"},"type":"SPACE"},"type":"DOCUMENT","locale":""}]
              }
            ],
          "locales":[]
        }
      ],
    "locales":[]
  };
  it('fromJSONObject', function() {
    var tree = XWiki.EntityReferenceTree.fromJSONObject(jsonTree);
    expect('xwiki').toEqual(tree.reference.name);
    expect(XWiki.EntityType.WIKI).toEqual(tree.reference.type);

    // Main Space
    expect(tree.children['Main']).not.toBe(null);
    expect(tree.children['Main'][XWiki.EntityType.SPACE]).not.toBeUndefined();
    expect(tree.children['Main'][XWiki.EntityType.WIKI]).toBeUndefined();
    expect(tree.children['Main'][XWiki.EntityType.DOCUMENT]).toBeUndefined();
    var mainSpaceNode = tree.children['Main'][XWiki.EntityType.SPACE];
    expect('Main').toEqual(mainSpaceNode.reference.name);
    expect(XWiki.EntityType.SPACE).toEqual(mainSpaceNode.reference.type);

    // Page both Space and Document at the same level (i.e. siblings)
    expect(mainSpaceNode.children['Page']).not.toBeUndefined();
    expect(mainSpaceNode.children['Page'][XWiki.EntityType.DOCUMENT]).not.toBeUndefined();
    expect(mainSpaceNode.children['Page'][XWiki.EntityType.SPACE]).not.toBeUndefined();
    expect(mainSpaceNode.children['Page'][XWiki.EntityType.WIKI]).toBeUndefined();

    var pageDocumentNode = mainSpaceNode.children['Page'][XWiki.EntityType.DOCUMENT];
    expect('Page').toEqual(pageDocumentNode.reference.name);
    expect(XWiki.EntityType.DOCUMENT).toEqual(pageDocumentNode.reference.type);

    var pageSpaceNode = mainSpaceNode.children['Page'][XWiki.EntityType.SPACE];
    expect('Page').toEqual(pageSpaceNode.reference.name);
    expect(XWiki.EntityType.SPACE).toEqual(pageSpaceNode.reference.type);

    // WebHome Document
    expect(pageSpaceNode.children['WebHome']).not.toBeUndefined();
    expect(pageSpaceNode.children['WebHome'][XWiki.EntityType.DOCUMENT]).not.toBeUndefined();
    expect(pageSpaceNode.children['WebHome'][XWiki.EntityType.SPACE]).toBeUndefined();
    expect(pageSpaceNode.children['WebHome'][XWiki.EntityType.WIKI]).toBeUndefined();

    var webhomeDocumentNode = pageSpaceNode.children['WebHome'][XWiki.EntityType.DOCUMENT];
    expect('WebHome').toEqual(webhomeDocumentNode.reference.name);
    expect(XWiki.EntityType.DOCUMENT).toEqual(webhomeDocumentNode.reference.type);
  });
  it('getChildByReference', function() {
    function execute(treeNode, stringReferencePath, referenceType, expectedNodeStringReference) {
      var referencePath = XWiki.Model.resolve(stringReferencePath, referenceType);
      var node = treeNode.getChildByReference(referencePath);
      if (expectedNodeStringReference == null) {
        expect(node).toBe(null);
        return;
      }

      expect(node).not.toBe(null, 'No node was found for the path [' + stringReferencePath + '] starting from node [' + node.reference + ']');
      var expectedNodeReference = XWiki.Model.resolve(expectedNodeStringReference, referenceType);
      if (expectedNodeReference.type == XWiki.EntityType.DOCUMENT) {
        // The resolver does not set any locale but the JSON specifies '' locales for documents.
        expectedNodeReference.locale = '';
      }
      expect(node.reference).toEqual(expectedNodeReference);
    }
    var tree = XWiki.EntityReferenceTree.fromJSONObject(jsonTree);

    // From the tree root.
    execute(tree, 'Main', XWiki.EntityType.SPACE, 'xwiki:Main');
    execute(tree, 'Main.Page', XWiki.EntityType.DOCUMENT, 'xwiki:Main.Page');
    execute(tree, 'Main.Page', XWiki.EntityType.SPACE, 'xwiki:Main.Page');
    execute(tree, 'Main.Page.WebHome', XWiki.EntityType.DOCUMENT, 'xwiki:Main.Page.WebHome');

    // From the Main Space node.
    var mainSpaceNode = tree.children['Main'][XWiki.EntityType.SPACE];
    execute(mainSpaceNode, 'Page', XWiki.EntityType.DOCUMENT, 'xwiki:Main.Page');

    // Node not found.
    execute(tree, 'NotFound', XWiki.EntityType.SPACE, null);
  })
});

});
