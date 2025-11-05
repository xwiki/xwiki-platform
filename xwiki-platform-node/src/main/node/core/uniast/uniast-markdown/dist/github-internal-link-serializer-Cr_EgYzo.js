import { EntityType as o } from "@xwiki/cristal-model-api";
import { injectable as p, inject as m } from "inversify";
var l = Object.getOwnPropertyDescriptor, u = (e, r, n, t) => {
  for (var c = t > 1 ? void 0 : t ? l(r, n) : r, a = e.length - 1, s; a >= 0; a--)
    (s = e[a]) && (c = s(c) || c);
  return c;
}, f = (e, r) => (n, t) => r(n, t, e);
let i = class {
  constructor(e) {
    this.documentService = e;
  }
  async serialize(e, r, n) {
    const t = await n.convertInlineContents(e), c = this.computeRef(r);
    return `[${t}](${c})`;
  }
  async serializeImage(e, r) {
    const n = this.computeRef(e);
    return `![${r ?? ""}](${n})`;
  }
  // eslint-disable-next-line max-statements
  computeRef(e) {
    if (e.parsedReference) {
      const r = this.documentService.getCurrentDocumentReference().value;
      let n = e.parsedReference;
      const t = n.type === o.ATTACHMENT;
      if (t)
        n = n.document;
      else if (n.type !== o.DOCUMENT)
        throw new Error(
          `Unexpected type ${n.type} for link serialization`
        );
      const c = [
        ...r.space && r.space.names.length > 0 ? r.space.names.map(() => "..") : ["."]
      ].join("/"), a = (t ? "." : "") + n.name, s = [...n.space?.names ?? [], a].map(encodeURI).join("/");
      return t ? `${c}/${s}/attachments/${encodeURI(e.parsedReference.name)}`.replace(
        /^\.\//,
        ""
      ) : `${c}/${s}.md`.replace(/^\.\//, "");
    } else
      return e.rawReference;
  }
};
i = u([
  p(),
  f(0, m("DocumentService"))
], i);
export {
  i as GitHubInternalLinkSerializer
};
//# sourceMappingURL=github-internal-link-serializer-Cr_EgYzo.js.map
