import { EntityType as i } from "@xwiki/cristal-model-api";
import { injectable as l, inject as p } from "inversify";
var m = Object.getOwnPropertyDescriptor, u = (e, n, r, t) => {
  for (var a = t > 1 ? void 0 : t ? m(n, r) : n, c = e.length - 1, s; c >= 0; c--)
    (s = e[c]) && (a = s(a) || a);
  return a;
}, f = (e, n) => (r, t) => n(r, t, e);
let o = class {
  constructor(e) {
    this.documentService = e;
  }
  async serialize(e, n, r) {
    return `[${`${await r.convertInlineContents(
      e
    )}`}](${this.serializeTarget(n)})`;
  }
  async serializeImage(e, n) {
    return `![${n ?? ""}](${this.serializeTarget(e)})`;
  }
  // eslint-disable-next-line max-statements
  serializeTarget(e) {
    if (e.parsedReference) {
      const n = this.documentService.getCurrentDocumentReference().value;
      let r = e.parsedReference;
      const t = r.type === i.ATTACHMENT;
      if (t)
        r = r.document;
      else if (r.type !== i.DOCUMENT)
        throw new Error(
          `Unexpected type ${r.type} for link serialization`
        );
      const a = [
        ...n.space && n.space.names.length > 0 ? n.space.names.map(() => "..") : ["."]
      ].join("/"), c = (t ? "." : "") + r.name, s = [...r.space?.names ?? [], c].map(encodeURI).join("/");
      return t ? `${a}/${s}/attachments/${encodeURI(e.parsedReference.name)}`.replace(
        /^\.\//,
        ""
      ) : `${a}/${s}.md`.replace(/^\.\//, "");
    } else
      return e.rawReference;
  }
};
o = u([
  l(),
  f(0, p("DocumentService"))
], o);
export {
  o as FilesystemInternalLinkSerializer
};
//# sourceMappingURL=filesystem-internal-link-serializer-De0Zjdfl.js.map
