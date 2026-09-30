/**
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

/**
 * The JSON representation of an entity reference, as returned by the XWiki REST API.
 *
 * @since 18.8.0RC1
 * @beta
 */
interface EntityReferenceJSON {
  name: string;
  /**
   * The upper case name of the entity type, e.g. `DOCUMENT`.
   */
  type: string;
  parent?: EntityReferenceJSON | null;
  locale?: string;
}

/**
 * The JSON representation of a node of an entity reference tree, as returned by the XWiki REST API.
 *
 * @since 18.8.0RC1
 * @beta
 */
interface EntityReferenceTreeNodeJSON {
  reference?: EntityReferenceJSON | null;
  children: EntityReferenceTreeNodeJSON[];
  locales: EntityReferenceJSON[];
}

/**
 * A default entity name, used to complete a partial reference. A list is used when several nested entities of the same
 * type are needed, e.g. for nested spaces.
 *
 * @since 18.8.0RC1
 * @beta
 */
type EntityReferenceDefaultValue = string | string[] | null | undefined;

/**
 * Provides the names to use for the entity types that are missing from the reference being resolved. It can be an
 * entity reference to take the default values from, a value indexed by entity type, or a function computing the value
 * for a given entity type.
 *
 * @since 18.8.0RC1
 * @beta
 */
type DefaultValueProvider =
  | { extractReference(type: number): EntityReference | null | undefined }
  | EntityReferenceDefaultValue[]
  | Record<number, EntityReferenceDefaultValue>
  | ((type: number) => EntityReferenceDefaultValue)
  | null
  | undefined;

/**
 * The supported entity types, as ordinals, along with the utility methods to convert between ordinals and names.
 *
 * @since 18.8.0RC1
 * @beta
 */
interface EntityTypeApi {
  readonly WIKI: 0;
  readonly SPACE: 1;
  readonly DOCUMENT: 2;
  readonly ATTACHMENT: 3;
  readonly OBJECT: 4;
  readonly OBJECT_PROPERTY: 5;
  readonly CLASS_PROPERTY: 6;

  /**
   * @param entityType - the ordinal of an entity type
   * @returns the camel case name of the given entity type, e.g. `objectProperty`, or `undefined` when the given
   *   ordinal doesn't match any entity type
   */
  getName(entityType: number): string | undefined;

  /**
   * @param name - the camel case name of an entity type, compared without taking the case into account
   * @returns the ordinal of the entity type with the given name, or `-1` when there is no such entity type
   */
  byName(name: string): number;
}

/**
 * The entry point to serialize and resolve entity references.
 *
 * @since 18.8.0RC1
 * @beta
 */
interface ModelApi {
  /**
   * @param entityReference - the entity reference to serialize
   * @returns the string representation of the given entity reference, the empty string when no reference is given
   */
  serialize(entityReference?: EntityReference | null): string;

  /**
   * @param representation - the string representation of an entity reference
   * @param entityType - the type of the entity the representation points to; when not specified it is read from the
   *   `entityType:` prefix of the representation, if there is one
   * @param defaultValueProvider - provides the names to use for the entity types missing from the representation
   * @returns the resolved entity reference
   */
  resolve(
    representation?: string | null,
    entityType?: number | string | null,
    defaultValueProvider?: DefaultValueProvider,
  ): EntityReference | null | undefined;
}

const entityTypeOrdinals = {
  WIKI: 0,
  SPACE: 1,
  DOCUMENT: 2,
  ATTACHMENT: 3,
  OBJECT: 4,
  OBJECT_PROPERTY: 5,
  CLASS_PROPERTY: 6,
} as const;

// The camel case name of each entity type, indexed by its ordinal. Derived from the keys above so that declaring a new
// entity type is enough to get its name.
const entityTypeNames: string[] = [];
for (const [key, ordinal] of Object.entries(entityTypeOrdinals)) {
  const parts = key.toLowerCase().split("_");
  entityTypeNames[ordinal] = [parts[0]]
    // Capitalize all parts except for the first one.
    .concat(
      parts
        .slice(1)
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1)),
    )
    .join("");
}

/**
 * The supported entity types.
 *
 * @since 18.8.0RC1
 * @beta
 */
const EntityType: EntityTypeApi = {
  ...entityTypeOrdinals,

  getName(entityType: number): string | undefined {
    return entityTypeNames[entityType];
  },

  byName(name: string): number {
    const lowerName = name.toLowerCase();
    return entityTypeNames.findIndex(
      (entityTypeName) => entityTypeName.toLowerCase() === lowerName,
    );
  },
};

/**
 * Walks up the parent chain until a reference of the given type is found. The given reference is returned when it
 * already has the expected type.
 *
 * @param reference - the reference to start from
 * @param type - the type of the entity to look for
 * @returns the first reference of the given type, if any
 */
function extractFrom(
  reference: EntityReference | null | undefined,
  type: number,
): EntityReference | null | undefined {
  let current = reference;
  // Loose comparison on purpose: the entity type has historically been accepted as a string too.
  while (current && current.type != type) {
    current = current.parent;
  }
  return current;
}

/**
 * @param reference - the reference to start from
 * @returns the reference chain, from the root entity down to the given reference
 */
function reversedChainOf(reference: EntityReference): EntityReference[] {
  const components: EntityReference[] = [];
  let current: EntityReference | null | undefined = reference;
  while (current) {
    components.push(current);
    current = current.parent;
  }
  return components.reverse();
}

/**
 * @param reference - the reference to start from
 * @returns the root of the reference chain the given reference belongs to
 */
function rootOf(reference: EntityReference): EntityReference {
  let root = reference;
  while (root.parent) {
    root = root.parent;
  }
  return root;
}

/**
 * Skips the leading components whose entity types don't match.
 *
 * @param components - the reference chain to make relative
 * @param baseComponents - the reference chain to be relative to
 * @returns the positions reached in both chains
 */
function skipDifferentTypes(
  components: EntityReference[],
  baseComponents: EntityReference[],
): [number, number] {
  let i = 0;
  let j = 0;
  while (
    i < components.length &&
    j < baseComponents.length &&
    components[i].type !== baseComponents[j].type
  ) {
    if (components[i].type > baseComponents[j].type) {
      j++;
    } else {
      i++;
    }
  }
  return [i, j];
}

/**
 * Skips the components that both chains have in common.
 *
 * @param components - the reference chain to make relative
 * @param baseComponents - the reference chain to be relative to
 * @param start - the positions to start from in both chains
 * @returns the positions reached in both chains
 */
function skipCommonComponents(
  components: EntityReference[],
  baseComponents: EntityReference[],
  start: [number, number],
): [number, number] {
  let [i, j] = start;
  while (
    i < components.length &&
    j < baseComponents.length &&
    components[i].type === baseComponents[j].type &&
    components[i].name === baseComponents[j].name
  ) {
    i++;
    j++;
  }
  return [i, j];
}

/**
 * If the current base entity type has not been fully matched then we need to add back the previously matched entity.
 *
 * @param components - the reference chain to make relative
 * @param baseComponents - the reference chain to be relative to
 * @param start - the positions reached in both chains
 * @returns the position to start the relative reference from
 */
function addBackPreviousComponent(
  components: EntityReference[],
  baseComponents: EntityReference[],
  start: [number, number],
): number {
  const [start_i, j] = start;
  let i = start_i;
  if (
    j < baseComponents.length &&
    j > 0 &&
    baseComponents[j].type === baseComponents[j - 1].type &&
    i > 0
  ) {
    i--;
    while (i > 0 && components[i].type === components[i - 1].type) {
      i--;
    }
  }
  return i;
}

/**
 * A reference to an entity of the XWiki model (a wiki, a space, a document, an attachment, an object, an object
 * property or a class property).
 *
 * @since 18.8.0RC1
 * @beta
 */
class EntityReference {
  public name: string;
  public type: number;
  public parent?: EntityReference | null;
  public locale?: string;

  public constructor(
    name: string,
    type: number,
    parent?: EntityReference | null,
    locale?: string,
  ) {
    this.name = name;
    this.type = type;
    this.parent = parent;
    this.locale = locale;
  }

  /**
   * Extract the entity of the given type from this one. This entity may be returned if it has the type requested.
   *
   * @param type - the type of the entity to be extracted
   * @returns the entity of the given type
   */
  public extractReference(type: number): EntityReference | null | undefined {
    return extractFrom(this, type);
  }

  /**
   * Extract the value identifying the entity with the given type from this reference. The name of this entity may be
   * returned if it has the type requested.
   *
   * @param type - the type of the entity to be extracted
   * @returns the value corresponding to the entity of the given type
   */
  public extractReferenceValue(type: number): string | null {
    const reference = this.extractReference(type);
    return reference ? reference.name : null;
  }

  /**
   * @param baseReference - the reference to compute this one relatively to
   * @returns a new reference pointing to the same entity but relative to the given reference
   */
  public relativeTo(baseReference?: EntityReference | null): EntityReference {
    const components = this.getReversedReferenceChain();
    const baseComponents = baseReference
      ? baseReference.getReversedReferenceChain()
      : [];
    const afterDifferentTypes = skipDifferentTypes(components, baseComponents);
    const afterCommonComponents = skipCommonComponents(
      components,
      baseComponents,
      afterDifferentTypes,
    );
    let i = addBackPreviousComponent(
      components,
      baseComponents,
      afterCommonComponents,
    );
    let relativeReference: EntityReference | undefined;
    for (; i < components.length; i++) {
      relativeReference = new EntityReference(
        components[i].name,
        components[i].type,
        relativeReference,
      );
    }
    return relativeReference || new EntityReference("", this.type);
  }

  /**
   * @returns the reference chain, from the root entity down to this reference
   */
  public getReversedReferenceChain(): EntityReference[] {
    return reversedChainOf(this);
  }

  /**
   * @returns the root of the reference chain this reference belongs to
   */
  public getRoot(): EntityReference {
    return rootOf(this);
  }

  /**
   * Attach the given reference to the root of this reference chain.
   *
   * @param parent - the reference to use as the new root parent
   * @returns this reference
   */
  public appendParent(parent: EntityReference): this {
    rootOf(this).parent = parent;
    return this;
  }

  /**
   * @param expectedParent - the reference to look for in the parent chain
   * @returns whether the given reference is one of the ancestors of this reference
   */
  public hasParent(expectedParent?: EntityReference | null): boolean {
    let actualParent = this.parent;
    // Loose comparison on purpose: handles the case when both the expected and the actual parent are missing, be it
    // as null or as undefined.
    if (actualParent == expectedParent) {
      return true;
    }
    while (actualParent && !actualParent.equals(expectedParent)) {
      actualParent = actualParent.parent;
    }
    // Loose comparison on purpose: the parent chain ends either with null or with undefined.
    return actualParent != null;
  }

  /**
   * @param reference - the reference to compare this one with
   * @returns whether both references point to the same entity
   */
  public equals(reference?: EntityReference | null): boolean {
    // Loose comparisons on purpose: a missing parent is either null or undefined and both have to be handled the
    // same way.
    if (reference == null) {
      return false;
    }
    if (
      (this.parent == null && reference.parent != null) ||
      (this.parent != null && reference.parent == null)
    ) {
      return false;
    }
    return (
      this.name === reference.name &&
      this.type === reference.type &&
      (this.parent == null || this.parent.equals(reference.parent))
    );
  }

  /**
   * @returns the string representation of this reference
   */
  public toString(): string {
    return Model.serialize(this);
  }

  /**
   * @returns the name of the entity this reference points to
   */
  public getName(): string {
    return this.name;
  }

  /**
   * @param object - the JSON representation of an entity reference
   * @returns the corresponding entity reference
   */
  public static fromJSONObject(object: EntityReferenceJSON): EntityReference {
    let parent: EntityReference | undefined;
    // Loose comparison on purpose: the JSON representation uses null for a missing parent.
    if (object.parent != null) {
      parent = EntityReference.fromJSONObject(object.parent);
    } else {
      parent = undefined;
    }

    return new EntityReference(
      object.name,
      EntityType.byName(object.type),
      parent,
      object.locale,
    );
  }
}

/**
 * A reference to a wiki.
 *
 * @since 18.8.0RC1
 * @beta
 */
class WikiReference extends EntityReference {
  public constructor(wikiName: string) {
    super(wikiName, EntityType.WIKI);
  }
}

/**
 * Computes the arguments to pass to the {@link EntityReference} constructor for a space reference. This is done
 * outside of the constructor because the parent chain of a nested space has to be built before calling `super()`.
 *
 * @param wikiName - the name of the wiki the space belongs to
 * @param spaceNames - the name of the space, or the names of the nested spaces leading to it
 * @returns the name, the type and the parent of the space reference
 * @throws the error message, as a string, when the given space names are invalid
 */
function spaceReferenceArguments(
  wikiName: string,
  spaceNames?: string | string[],
): [string, number, EntityReference] {
  const wikiReference = new WikiReference(wikiName);
  if (Array.isArray(spaceNames) && spaceNames.length > 0) {
    // Support passing an Array of Spaces (Nested Spaces).
    let reference: EntityReference = wikiReference;
    let i = 0;
    for (; i < spaceNames.length - 1; ++i) {
      reference = new EntityReference(
        spaceNames[i],
        EntityType.SPACE,
        reference,
      );
    }
    return [spaceNames[i], EntityType.SPACE, reference];
  } else if (
    typeof spaceNames === "string" ||
    typeof spaceNames === "undefined"
  ) {
    // Support passing a single space as a String for both backward-compatibility reason but also simplicity.
    return [spaceNames as string, EntityType.SPACE, wikiReference];
  } else {
    // A string is thrown, rather than an Error, because callers have been comparing it as such for a long time.
    throw (
      "Missing mandatory space name or invalid type for: [" + spaceNames + "]"
    );
  }
}

/**
 * A reference to a space, possibly nested.
 *
 * @since 18.8.0RC1
 * @beta
 */
class SpaceReference extends EntityReference {
  public constructor(wikiName: string, spaceNames?: string | string[]) {
    super(...spaceReferenceArguments(wikiName, spaceNames));
  }
}

/**
 * A reference to a document.
 *
 * @since 18.8.0RC1
 * @beta
 */
class DocumentReference extends EntityReference {
  public constructor(
    wikiName: string,
    spaceNames: string | string[] | undefined,
    pageName: string,
  ) {
    super(
      pageName,
      EntityType.DOCUMENT,
      new SpaceReference(wikiName, spaceNames),
    );
  }
}

/**
 * A reference to an attachment.
 *
 * @since 18.8.0RC1
 * @beta
 */
class AttachmentReference extends EntityReference {
  public constructor(fileName: string, documentReference?: EntityReference) {
    super(fileName, EntityType.ATTACHMENT, documentReference);
  }
}

/**
 * Walks down the given tree node, following the given reference chain.
 *
 * @param node - the node to start from
 * @param references - the reference chain to follow
 * @returns the node at the end of the chain, or null when there is no such node
 */
function findDescendant(
  node: EntityReferenceTreeNode,
  references: EntityReference[],
): EntityReferenceTreeNode | null {
  let descendant = node;
  for (const element of references) {
    // Get the children with the same reference element name.
    const descendantByType = descendant.children[element.name];
    if (typeof descendantByType === "undefined") {
      return null;
    }

    // Get the child with the same reference element type.
    const child = descendantByType[element.type];
    if (typeof child === "undefined") {
      return null;
    }
    descendant = child;
  }

  return descendant;
}

/**
 * A node of a tree of entity references.
 *
 * @since 18.8.0RC1
 * @beta
 */
class EntityReferenceTreeNode {
  /**
   * The child nodes, indexed first by entity name then by entity type, because the same name can be used by both a
   * space and a document.
   */
  public children: Record<string, Record<number, EntityReferenceTreeNode>>;

  /**
   * The translations of the entity this node points to, indexed by locale.
   */
  public locales: Record<string, EntityReference>;

  // Declared without emitting a class field, so that the property exists only once it has been set.
  declare public reference?: EntityReference;

  public constructor() {
    this.children = {};
    this.locales = {};
  }

  /**
   * @param node - the JSON representation of a tree node
   * @internal
   */
  public _fromJSONObject(node: EntityReferenceTreeNodeJSON): void {
    // Reference
    // Loose comparison on purpose: the JSON representation uses null for a missing reference.
    if (node.reference != null) {
      this.reference = EntityReference.fromJSONObject(node.reference);
    }

    // Children
    // Note that forEach is used rather than Prototype.js' each, which the previous implementation relied on, so that
    // this code doesn't depend on Prototype.js being loaded.
    node.children.forEach((child) => this._childFromJSONObject(child));

    // Locales
    node.locales.forEach((locale) => this._localeFromJSONObject(locale));
  }

  /**
   * @param jsonNode - the JSON representation of a child node
   * @internal
   */
  public _childFromJSONObject(jsonNode: EntityReferenceTreeNodeJSON): void {
    const node = new EntityReferenceTreeNode();
    node._fromJSONObject(jsonNode);

    // For the reference name, you can have both a space and a document as children.
    const reference = node.reference!;
    let childrenByType = this.children[reference.name];
    if (typeof childrenByType === "undefined") {
      childrenByType = {};
      this.children[reference.name] = childrenByType;
    }

    // Add the child node including its type.
    childrenByType[reference.type] = node;
  }

  /**
   * @param jsonReference - the JSON representation of a translation of the entity this node points to
   * @internal
   */
  public _localeFromJSONObject(jsonReference: EntityReferenceJSON): void {
    this.locales[jsonReference.locale!] =
      EntityReference.fromJSONObject(jsonReference);
  }

  /**
   * @returns true if the node contains children
   */
  public hasChildren(): boolean {
    return Object.keys(this.children).length !== 0;
  }

  /**
   * @returns true if the node contains locales
   */
  public hasLocales(): boolean {
    return Object.keys(this.locales).length !== 0;
  }

  /**
   * @param referencePath - a path in the tree starting from this node, specified as an EntityReference
   * @returns the node associated to the specified path
   */
  public getChildByReference(
    referencePath?: EntityReference,
  ): EntityReferenceTreeNode | null {
    // Only undefined is checked on purpose: passing null has always failed with a type error further down.
    if (typeof referencePath === "undefined") {
      return null;
    }

    return findDescendant(this, referencePath.getReversedReferenceChain());
  }
}

/**
 * A tree of entity references.
 *
 * @since 18.8.0RC1
 * @beta
 */
class EntityReferenceTree extends EntityReferenceTreeNode {
  /**
   * @param object - the JSON representation of an entity reference tree
   * @returns the corresponding entity reference tree
   */
  public static fromJSONObject(
    object: EntityReferenceTreeNodeJSON,
  ): EntityReferenceTree {
    const tree = new EntityReferenceTree();
    tree._fromJSONObject(object);
    return tree;
  }
}

const ESCAPE = "\\";
const DBLESCAPE = ESCAPE + ESCAPE;
const WIKISEP = ":";
const SPACESEP = ".";
const ATTACHMENTSEP = "@";
const OBJECTSEP = "^";
const PROPERTYSEP = SPACESEP;
const CLASSPROPSEP = OBJECTSEP;

const ESCAPES: string[][] = [
  /* WIKI */ [],
  /* SPACE */ [SPACESEP, WIKISEP, ESCAPE],
  /* DOCUMENT */ [SPACESEP, ESCAPE],
  /* ATTACHMENT */ [ATTACHMENTSEP, ESCAPE],
  /* OBJECT */ [OBJECTSEP, ESCAPE],
  /* OBJECT_PROPERTY */ [PROPERTYSEP, ESCAPE],
  /* CLASS_PROPERTY */ [CLASSPROPSEP, SPACESEP, ESCAPE],
];

const REPLACEMENTS: string[][] = [
  /* WIKI */ [],
  /* SPACE */ [ESCAPE + SPACESEP, ESCAPE + WIKISEP, DBLESCAPE],
  /* DOCUMENT */ [ESCAPE + SPACESEP, DBLESCAPE],
  /* ATTACHMENT */ [ESCAPE + ATTACHMENTSEP, DBLESCAPE],
  /* OBJECT */ [ESCAPE + OBJECTSEP, DBLESCAPE],
  /* OBJECT_PROPERTY */ [ESCAPE + PROPERTYSEP, DBLESCAPE],
  /* CLASS_PROPERTY */ [ESCAPE + CLASSPROPSEP, ESCAPE + SPACESEP, DBLESCAPE],
];

const SEPARATORS: string[][] = [
  /* WIKI */ [],
  /* SPACE */ [SPACESEP, WIKISEP],
  /* DOCUMENT */ [SPACESEP, WIKISEP],
  /* ATTACHMENT */ [ATTACHMENTSEP, SPACESEP, WIKISEP],
  /* OBJECT */ [OBJECTSEP, SPACESEP, WIKISEP],
  /* OBJECT_PROPERTY */ [PROPERTYSEP, OBJECTSEP, SPACESEP, WIKISEP],
  /* CLASS_PROPERTY */ [CLASSPROPSEP, SPACESEP, WIKISEP],
];

const DEFAULT_PARENT: (number | null)[] = [
  /* WIKI */ null,
  /* SPACE */ EntityType.WIKI,
  /* DOCUMENT */ EntityType.SPACE,
  /* ATTACHMENT */ EntityType.DOCUMENT,
  /* OBJECT */ EntityType.DOCUMENT,
  /* OBJECT_PROPERTY */ EntityType.OBJECT,
  /* CLASS_PROPERTY */ EntityType.DOCUMENT,
];

// Left sparse on purpose: the missing WIKI entry is what ends the resolution loop.
const REFERENCE_SETUP: (Record<string, number> | undefined)[] = [];
// Skip the WIKI entity type because it doesn't require special handling.
for (let i = 1; i < SEPARATORS.length; i++) {
  REFERENCE_SETUP[i] = {};
}
REFERENCE_SETUP[EntityType.SPACE]![WIKISEP] = EntityType.WIKI;
REFERENCE_SETUP[EntityType.SPACE]![SPACESEP] = EntityType.SPACE;
REFERENCE_SETUP[EntityType.DOCUMENT]![SPACESEP] = EntityType.SPACE;
REFERENCE_SETUP[EntityType.ATTACHMENT]![ATTACHMENTSEP] = EntityType.DOCUMENT;
REFERENCE_SETUP[EntityType.OBJECT]![OBJECTSEP] = EntityType.DOCUMENT;
REFERENCE_SETUP[EntityType.OBJECT_PROPERTY]![PROPERTYSEP] = EntityType.OBJECT;
REFERENCE_SETUP[EntityType.CLASS_PROPERTY]![CLASSPROPSEP] = EntityType.DOCUMENT;

const ESCAPE_MATCHING = [DBLESCAPE, ESCAPE];
const ESCAPE_MATCHING_REPLACE = [ESCAPE, ""];

function contains(text: string, position: number, subText: string): boolean {
  for (let i = 0; i < subText.length; i++) {
    const j = position + i;
    if (j >= text.length || text.charAt(j) !== subText.charAt(i)) {
      return false;
    }
  }
  return true;
}

function replaceEach(
  text: string,
  matches: string[],
  replacements: string[],
): string {
  let result = text;
  let i = -1;
  while (++i < result.length) {
    for (let j = 0; j < matches.length; j++) {
      if (contains(result, i, matches[j])) {
        result =
          result.slice(0, i) +
          replacements[j] +
          result.slice(i + matches[j].length);
        i += replacements[j].length - 1;
        break;
      }
    }
  }
  return result;
}

/**
 * The outcome of looking at one character of the representation being resolved.
 */
interface SeparatorMatch {
  /**
   * The position to continue the search from.
   */
  position: number;

  /**
   * The type of the parent entity when a non-escaped separator was found, null otherwise.
   */
  parentType: number | null;
}

/**
 * Removes the escaping character that precedes the character at the given position.
 *
 * @param representation - the characters of the representation being resolved
 * @param index - the position of the escaped character
 * @returns the position to continue the search from
 */
function unescapeAt(representation: string[], index: number): SeparatorMatch {
  representation.splice(index - 1, 1);
  return { position: index - 1, parentType: null };
}

/**
 * @param representation - the characters of the representation being resolved
 * @param setup - the separators that are meaningful for the entity type being resolved
 * @param index - the position to look at
 * @returns whether a non-escaped separator was found at the given position
 */
function matchSeparatorAt(
  representation: string[],
  setup: Record<string, number>,
  index: number,
): SeparatorMatch {
  const nextIndex = index - 1;
  const separatorType = setup[representation[index]];
  if (typeof separatorType === "number") {
    const escaped =
      getNumberOfCharsBefore(ESCAPE, representation, nextIndex) % 2 !== 0;
    return escaped
      ? unescapeAt(representation, index)
      : { position: index, parentType: separatorType };
  }
  const nextChar = nextIndex < 0 ? 0 : representation[nextIndex];
  return nextChar === ESCAPE
    ? unescapeAt(representation, index)
    : { position: index, parentType: null };
}

/**
 * Searches all characters for a non escaped separator. If found, then the part after the character is considered as
 * the reference name and the part before the separator is parsed next.
 *
 * @param representation - the characters of the representation being resolved
 * @param setup - the separators that are meaningful for the entity type being resolved
 * @returns the position of the separator, negative when there is none, and the type of the parent entity
 */
function findSeparator(
  representation: string[],
  setup: Record<string, number>,
): SeparatorMatch {
  let parentType: number | null = null;
  let i = representation.length;
  while (--i >= 0) {
    const match = matchSeparatorAt(representation, setup, i);
    if (match.parentType !== null) {
      parentType = match.parentType;
      break;
    }
    i = match.position;
  }
  return { position: i, parentType };
}

/**
 * @param character - the character to count
 * @param representation - the characters of the representation being resolved
 * @param currentPosition - the position to count from, going backwards
 * @returns the number of consecutive occurrences of the given character ending at the given position
 */
function getNumberOfCharsBefore(
  character: string,
  representation: string[],
  currentPosition: number,
): number {
  let position = currentPosition;
  while (position >= 0 && representation[position] === character) {
    --position;
  }
  return currentPosition - position;
}

/**
 * Resolves the entity reference chain of a representation that has separators.
 *
 * @param resolver - the resolver to delegate to, so that overridden methods are taken into account
 * @param representation - the characters of the representation being resolved
 * @param entityType - the type of the entity the representation points to
 * @param defaultValueProvider - provides the names to use for the entity types missing from the representation
 * @returns the resolved entity reference
 */
function resolveChain(
  resolver: EntityReferenceResolver,
  representation: string[],
  entityType: number,
  defaultValueProvider?: DefaultValueProvider,
): EntityReference | undefined {
  let reference: EntityReference | undefined;
  let currentType = entityType;
  let typeSetup = REFERENCE_SETUP[entityType];
  do {
    const separator = findSeparator(representation, typeSetup!);
    const parent = resolver._getNewReference(
      separator.position,
      representation,
      currentType,
      defaultValueProvider,
    );
    reference = resolver._appendNewReference(reference, parent);
    // The || is on purpose: when the parent type is WIKI (0) the default parent is WIKI as well.
    currentType = separator.parentType || DEFAULT_PARENT[currentType]!;
    typeSetup = REFERENCE_SETUP[currentType];
    // Loose comparison on purpose: the setup is undefined for the entity types that end the resolution.
  } while (typeSetup != null);

  // Handle last entity reference's name.
  return resolver._appendNewReference(
    reference,
    resolver._getEscapedReference(
      representation,
      currentType,
      defaultValueProvider,
    ),
  );
}

/**
 * @param reference - the reference to take the default names from
 * @param type - the type of the entities to collect
 * @returns the names of the leading entities of the given type
 */
function namesFromReference(
  reference: EntityReference | null | undefined,
  type: number,
): string[] {
  const names: string[] = [];
  let current = reference;
  // Extract all the reference components with the specified type.
  while (current && current.type === type) {
    names.push(current.name);
    current = current.parent;
  }
  names.reverse();
  return names;
}

/**
 * @param type - the type of the entity to find a default name for
 * @param defaultValueProvider - the provider to ask
 * @returns the default name, or names, to use for the given entity type
 */
function defaultNameFor(
  type: number,
  defaultValueProvider?: DefaultValueProvider,
): EntityReferenceDefaultValue {
  if (typeof defaultValueProvider === "function") {
    return defaultValueProvider(type);
  }
  // Note that typeof null is "object", so a null provider goes through this branch, like it always did.
  if (typeof defaultValueProvider !== "object") {
    return undefined;
  }
  const provider = defaultValueProvider as
    | (Partial<{
        extractReference(type: number): EntityReference | null | undefined;
      }> &
        Record<number, EntityReferenceDefaultValue>)
    | null;
  if (provider && typeof provider.extractReference === "function") {
    return namesFromReference(provider.extractReference(type), type);
  }
  return provider && provider[type];
}

/**
 * Resolves a string representation into an entity reference.
 *
 * @since 18.8.0RC1
 * @beta
 */
class EntityReferenceResolver {
  /**
   * @param value - the string representation to resolve
   * @param type - the type of the entity the representation points to
   * @param defaultValueProvider - provides the names to use for the entity types missing from the representation
   * @returns the resolved entity reference
   * @throws the error message, as a string, when the given entity type is not supported
   */
  public resolve(
    value?: string | null,
    type?: number | string | null,
    defaultValueProvider?: DefaultValueProvider,
  ): EntityReference | null | undefined {
    // Create a char array from the input string (as an equivalent to Java's StringBuilder).
    const representation = (value || "").split("");
    // No radix on purpose: parseInt already converts its argument to a string, so entity types passed as strings keep
    // being accepted.
    const entityType = Number.parseInt(String(type));

    // First, check if the given entity type is valid.
    if (
      Number.isNaN(entityType) ||
      entityType < 0 ||
      entityType >= SEPARATORS.length
    ) {
      throw "No parsing definition found for Entity Type [" + entityType + "]";
    }

    // Check if the specified entity type requires anything specific.
    if (!REFERENCE_SETUP[entityType]) {
      return this._getEscapedReference(
        representation,
        entityType,
        defaultValueProvider,
      );
    }

    return resolveChain(this, representation, entityType, defaultValueProvider);
  }

  /**
   * @param representation - the remaining characters of the representation being resolved
   * @param type - the type of the entity the remaining characters point to
   * @param defaultValueProvider - provides the name to use when there are no remaining characters
   * @returns the unescaped entity reference
   * @internal
   */
  public _getEscapedReference(
    representation: string[],
    type: number,
    defaultValueProvider?: DefaultValueProvider,
  ): EntityReference | null {
    if (representation.length > 0) {
      const name = replaceEach(
        representation.join(""),
        ESCAPE_MATCHING,
        ESCAPE_MATCHING_REPLACE,
      );
      return new EntityReference(name, type);
    } else {
      return this._resolveDefaultReference(type, defaultValueProvider);
    }
  }

  /**
   * @param index - the position of the separator that was found, negative when there is none
   * @param representation - the remaining characters of the representation being resolved
   * @param type - the type of the entity the characters after the separator point to
   * @param defaultValueProvider - provides the name to use when there is nothing after the separator
   * @returns the entity reference matching the characters after the separator
   * @internal
   */
  public _getNewReference(
    index: number,
    representation: string[],
    type: number,
    defaultValueProvider?: DefaultValueProvider,
  ): EntityReference | null {
    // Found a valid separator (not escaped), separate content on its left from content on its right.
    let reference: EntityReference | null;
    if (index < representation.length - 1) {
      const name = representation.slice(index + 1).join("");
      reference = new EntityReference(name, type);
    } else {
      reference = this._resolveDefaultReference(type, defaultValueProvider);
    }
    representation.splice(Math.max(index, 0), representation.length);
    return reference;
  }

  /**
   * @param type - the type of the entity to build a default reference for
   * @param defaultValueProvider - provides the name, or the nested names, to use
   * @returns the default entity reference, null when the provider has no value for the given type
   * @internal
   */
  public _resolveDefaultReference(
    type: number,
    defaultValueProvider?: DefaultValueProvider,
  ): EntityReference | null {
    const name = defaultNameFor(type, defaultValueProvider);
    let reference: EntityReference | null = null;
    if (name && name.length > 0) {
      const names = typeof name === "string" ? [name] : name;
      for (let i = 0; i < names.length; i++) {
        reference = new EntityReference(names[i], type, reference);
      }
    }
    return reference;
  }

  /**
   * @param reference - the reference built so far
   * @param parent - the reference to attach to the root of the reference built so far
   * @returns the resulting reference
   * @internal
   */
  public _appendNewReference(
    reference: EntityReference | undefined,
    parent: EntityReference | null | undefined,
  ): EntityReference | undefined {
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

  /**
   * @param character - the character to count
   * @param representation - the characters of the representation being resolved
   * @param currentPosition - the position to count from, going backwards
   * @returns the number of consecutive occurrences of the given character ending at the given position
   * @internal
   */
  public _getNumberOfCharsBefore(
    character: string,
    representation: string[],
    currentPosition: number,
  ): number {
    return getNumberOfCharsBefore(character, representation, currentPosition);
  }
}

/**
 * Serializes an entity reference into its string representation.
 *
 * @since 18.8.0RC1
 * @beta
 */
class EntityReferenceSerializer {
  /**
   * @param entityReference - the entity reference to serialize
   * @returns the string representation of the given entity reference
   */
  public serialize(entityReference?: EntityReference | null): string {
    return entityReference
      ? this.serialize(entityReference.parent) +
          this._serializeComponent(entityReference)
      : "";
  }

  /**
   * @param entityReference - the entity reference whose own name has to be serialized
   * @returns the separator, if any, followed by the escaped name of the given entity reference
   * @internal
   */
  public _serializeComponent(entityReference: EntityReference): string {
    let representation = "";
    const escapes = ESCAPES[entityReference.type];

    // Add the separator if this is not the first component.
    if (entityReference.parent) {
      representation +=
        entityReference.parent.type === EntityType.WIKI ? WIKISEP : escapes[0];
    }

    // The root reference doesn't have to escape its separator because the reference is parsed from the right.
    if (escapes.length > 0) {
      representation += replaceEach(
        entityReference.name,
        escapes,
        REPLACEMENTS[entityReference.type],
      );
    } else {
      representation += entityReference.name.replaceAll(ESCAPE, DBLESCAPE);
    }

    return representation;
  }
}

const resolver = new EntityReferenceResolver();
const serializer = new EntityReferenceSerializer();

/**
 * The entry point to serialize and resolve entity references.
 *
 * @since 18.8.0RC1
 * @beta
 */
const Model: ModelApi = {
  serialize: function (entityReference?: EntityReference | null): string {
    return serializer.serialize(entityReference);
  },

  resolve: function (
    representation?: string | null,
    entityType?: number | string | null,
    defaultValueProvider?: DefaultValueProvider,
  ): EntityReference | null | undefined {
    let value = representation;
    let type = entityType;
    // Loose comparison on purpose: an explicitly null entity type must also trigger the detection below.
    if (type == undefined && typeof value === "string") {
      // Try to extract the entity type from the representation.
      const separatorIndex = value.indexOf(":");
      if (separatorIndex > 0) {
        type = EntityType.byName(value.substring(0, separatorIndex));
        if (type >= 0) {
          value = value.substring(separatorIndex + 1);
        }
      }
    }
    return resolver.resolve(value, type, defaultValueProvider);
  },
};

export {
  AttachmentReference,
  type DefaultValueProvider,
  DocumentReference,
  EntityReference,
  type EntityReferenceDefaultValue,
  type EntityReferenceJSON,
  EntityReferenceResolver,
  EntityReferenceSerializer,
  EntityReferenceTree,
  EntityReferenceTreeNode,
  type EntityReferenceTreeNodeJSON,
  EntityType,
  type EntityTypeApi,
  Model,
  type ModelApi,
  SpaceReference,
  WikiReference,
};
