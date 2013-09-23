describe('EntityReference', function() {
  describe('Serializer', function() {
    function resolveAndSerialize(representation, entityType) {
      expect(representation).toEqual(XWiki.Model.serialize(XWiki.Model.resolve(representation, entityType)));
    }

    it('Wiki reference', function() {
      resolveAndSerialize('some:wiki', XWiki.EntityType.WIKI);
    });

    it('Space reference', function() {
      var reference = XWiki.Model.resolve('wiki:space1.space2', XWiki.EntityType.SPACE);
      expect('wiki:space1\\.space2').toEqual(XWiki.Model.serialize(reference));
    });

    it('Document reference', function() {
      resolveAndSerialize('wiki:space.page', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('wiki:space.', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('space.', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('page', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('.', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('', XWiki.EntityType.DOCUMENT);

      expect('').toEqual(XWiki.Model.serialize());

      var reference = XWiki.Model.resolve('wiki1.wiki2:wiki3:some.space.page', XWiki.EntityType.DOCUMENT);
      expect('wiki1.wiki2:wiki3:some\\.space.page').toEqual(XWiki.Model.serialize(reference));

      reference = XWiki.Model.resolve('some.space.page', XWiki.EntityType.DOCUMENT);
      expect('some\\.space.page').toEqual(XWiki.Model.serialize(reference));

      resolveAndSerialize('wiki:page', XWiki.EntityType.DOCUMENT);

      resolveAndSerialize('\\.:@\\.', XWiki.EntityType.DOCUMENT);
      resolveAndSerialize('\\\\:\\\\.\\\\', XWiki.EntityType.DOCUMENT);

      // The escaping here is not necessary but we want to test that it works.
      reference = XWiki.Model.resolve('\\wiki:\\space.\\page', XWiki.EntityType.DOCUMENT);
      expect('wiki:space.page').toEqual(XWiki.Model.serialize(reference));
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
      var reference = XWiki.Model.resolve('wiki:space.page', XWiki.EntityType.DOCUMENT);
      expect('wiki').toEqual(reference.extractReference(XWiki.EntityType.WIKI).name);
      expect('space').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('page').toEqual(reference.name);

      reference = XWiki.Model.resolve('wiki:space.', XWiki.EntityType.DOCUMENT);
      expect('wiki').toEqual(reference.extractReference(XWiki.EntityType.WIKI).name);
      expect('space').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('').toEqual(reference.name);

      reference = XWiki.Model.resolve('space.', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect('space').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('').toEqual(reference.name);

      reference = XWiki.Model.resolve('page', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect(reference.extractReference(XWiki.EntityType.SPACE)).toBeUndefined();
      expect('page').toEqual(reference.name);

      reference = XWiki.Model.resolve('.', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect('').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('').toEqual(reference.name);

      reference = XWiki.Model.resolve(null, XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect(reference.extractReference(XWiki.EntityType.SPACE)).toBeUndefined();
      expect('').toEqual(reference.name);

      reference = XWiki.Model.resolve('', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect(reference.extractReference(XWiki.EntityType.SPACE)).toBeUndefined();
      expect('').toEqual(reference.name);

      reference = XWiki.Model.resolve('wiki1.wiki2:wiki3:some.space.page', XWiki.EntityType.DOCUMENT);
      expect('wiki1.wiki2:wiki3').toEqual(reference.extractReference(XWiki.EntityType.WIKI).name);
      expect('some.space').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
      expect('page').toEqual(reference.name);

      reference = XWiki.Model.resolve('some.space.page', XWiki.EntityType.DOCUMENT);
      expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
      expect('some.space').toEqual(reference.extractReference(XWiki.EntityType.SPACE).name);
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
  });
});
