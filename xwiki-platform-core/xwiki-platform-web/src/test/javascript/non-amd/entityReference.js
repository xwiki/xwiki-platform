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
    it('Document reference', function() {
      var reference = XWiki.Model.resolve('wiki:path.to.page', XWiki.EntityType.DOCUMENT);
      expect('wiki').toEqual(reference.extractReference(XWiki.EntityType.WIKI).name);
      var spaceReference = reference.extractReference(XWiki.EntityType.SPACE);
      expect('path').toEqual(spaceReference.parent.extractReference(XWiki.EntityType.SPACE).name);
      expect('to').toEqual(spaceReference.name);
      expect('page').toEqual(reference.name);

      reference = XWiki.Model.resolve('wiki:space.', XWiki.EntityType.DOCUMENT, reference);
      expect('wiki').toEqual(reference.extractReference(XWiki.EntityType.WIKI).name);
      expect('space').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('page').toEqual(reference.name);

      reference = XWiki.Model.resolve('space.', XWiki.EntityType.DOCUMENT, [null, null, 'Test']);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect('space').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('Test').toEqual(reference.name);

      reference = XWiki.Model.resolve('page', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect(reference.extractReference(XWiki.EntityType.SPACE)).toBeUndefined();
      expect('page').toEqual(reference.name);

      reference = XWiki.Model.resolve('', XWiki.EntityType.DOCUMENT, function(type) {return 'X'});
      expect('X').toEqual(reference.extractReference(XWiki.EntityType.WIKI).name);
      expect('X').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('X').toEqual(reference.name);

      reference = XWiki.Model.resolve('.', XWiki.EntityType.DOCUMENT);
      expect(reference).toBeUndefined();

      reference = XWiki.Model.resolve(null, XWiki.EntityType.DOCUMENT);
      expect(reference).toBeUndefined();

      reference = XWiki.Model.resolve('', XWiki.EntityType.DOCUMENT);
      expect(reference).toBeUndefined();

      reference = XWiki.Model.resolve('wiki1.wiki2:wiki3:space1.sp\\.ace2.sp\\:ace3.page', XWiki.EntityType.DOCUMENT);
      expect('wiki1.wiki2:wiki3').toEqual(reference.extractReference(XWiki.EntityType.WIKI).name);
      expect('sp:ace3').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('page').toEqual(reference.name);

      reference = XWiki.Model.resolve('one.two.page', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      var spaceReference = reference.extractReference(XWiki.EntityType.SPACE);
      expect('one').toEqual(spaceReference.parent.extractReference(XWiki.EntityType.SPACE).name);
      expect('two').toEqual(spaceReference.name);
      expect('page').toEqual(reference.name);

      reference = XWiki.Model.resolve('wiki:page', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect(reference.extractReference(XWiki.EntityType.SPACE)).toBeUndefined();
      expect('wiki:page').toEqual(reference.name);

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
    function execute(reference, baseReference, expected) {
      expect(XWiki.Model.serialize(reference.relativeTo(baseReference))).toEqual(expected);
    }
    it('relativeTo', function() {
      var reference = new XWiki.DocumentReference('wiki', 'space', 'page');
      execute(reference, reference, '');
      execute(reference, reference.parent, 'page');
      execute(reference, reference.parent.parent, 'space.page');
      execute(reference, null, 'wiki:space.page');
      execute(reference, new XWiki.WikiReference('xwiki'), 'wiki:space.page');
      execute(reference, new XWiki.SpaceReference('wiki', 'Space'), 'space.page');
      execute(reference, new XWiki.DocumentReference('wiki', 'space', 'Page'), 'page');
      execute(reference, new XWiki.DocumentReference('wiki', 'Space', 'page'), 'space.page');
      execute(reference.parent, reference, '');
      execute(reference.parent.parent, reference, '');
      execute(reference.parent, new XWiki.DocumentReference('wiki', 'space', 'Page'), '');
      execute(reference.parent, new XWiki.DocumentReference('wiki', 'Space', 'page'), 'space');
      execute(reference.parent, new XWiki.DocumentReference('xwiki', 'space', 'page'), 'wiki:space');
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
  });
});
