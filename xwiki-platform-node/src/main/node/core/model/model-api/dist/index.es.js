var c = /* @__PURE__ */ ((e) => (e[e.WIKI = 0] = "WIKI", e[e.SPACE = 1] = "SPACE", e[e.DOCUMENT = 2] = "DOCUMENT", e[e.ATTACHMENT = 3] = "ATTACHMENT", e))(c || {});
class r {
  type = 0;
  name;
  constructor(a) {
    this.name = a;
  }
}
class m {
  type = 1;
  wiki;
  names;
  constructor(a, ...t) {
    this.wiki = a, this.names = t;
  }
}
class n {
  type = 2;
  space;
  name;
  /**
   * Indicates whether the current document reference is terminal.
   * @since 0.13
   * @beta
   */
  terminal;
  constructor(a, t, s) {
    this.space = t, this.name = a, this.terminal = s ?? !1;
  }
}
class o {
  type = 3;
  name;
  document;
  /**
   * @since 0.22
   */
  _metadata;
  /**
   *
   * @param name - the name of the attachment
   * @param document - the document reference of the attachment
   * @param metadata - (since 0.22) the metadata of the attachment, can be used to store additional information
   *  about the attachment (e.g., additional information for a given backend)
   */
  constructor(a, t, s) {
    this.name = a, this.document = t, this._metadata = s ?? {};
  }
  get metadata() {
    return this._metadata;
  }
}
export {
  o as AttachmentReference,
  n as DocumentReference,
  c as EntityType,
  m as SpaceReference,
  r as WikiReference
};
//# sourceMappingURL=index.es.js.map
