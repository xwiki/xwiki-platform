import { EntityType as l } from "@xwiki/cristal-model-api";
import { XMLParser as f } from "fast-xml-parser";
import { injectable as u, inject as c } from "inversify";
var R = Object.getOwnPropertyDescriptor, h = (e, o, r, t) => {
  for (var n = t > 1 ? void 0 : t ? R(o, r) : o, s = e.length - 1, i; s >= 0; s--)
    (i = e[s]) && (n = i(n) || n);
  return n;
}, p = (e, o) => (r, t) => o(r, t, e);
let m = class {
  constructor(e, o, r) {
    this.remoteURLSerializerProvider = e, this.cristalApp = o, this.documentService = r;
  }
  async serialize(e, o, r) {
    const t = await r.convertInlineContents(e), n = this.remoteURLSerializerProvider.get().serialize(o.parsedReference ?? void 0), s = await fetch(n, {
      method: "PROPFIND",
      body: `<?xml version="1.0" encoding="UTF-8"?>
 <d:propfind xmlns:d="DAV:">
   <d:prop xmlns:oc="http://owncloud.org/ns">
    <oc:fileid/>
   </d:prop>
 </d:propfind>`,
      headers: {
        Authorization: `Basic ${btoa("admin:admin")}`
      }
    }), a = new f().parse(await s.text())["d:multistatus"]["d:response"]["d:propstat"]["d:prop"]["oc:fileid"], d = `${this.cristalApp.getWikiConfig().baseURL}/f/${a}`;
    return `[${t}](${d})`;
  }
  // eslint-disable-next-line max-statements
  async serializeImage(e, o) {
    let r;
    if (e.parsedReference) {
      const t = this.documentService.getCurrentDocumentReference().value;
      if (e.parsedReference.type == l.ATTACHMENT) {
        const n = e.parsedReference, s = n.document;
        if (t === s)
          r = `.${s.name}/attachments/${n.name}`;
        else {
          const i = [
            ...t.space && t.space.names.length > 0 ? t.space.names.map(() => "..") : ["."]
          ].join("/"), a = [
            ...n.document.space?.names ?? [],
            "." + n.document.name
          ].map(encodeURI).join("/");
          r = `${i}/${a}/attachments/${encodeURI(n.name)}`;
        }
      } else
        throw new Error(
          `Unexpected type ${e.parsedReference.type} for link serialization`
        );
    } else
      r = e.rawReference;
    return `![${o ?? ""}](${r})`;
  }
};
m = h([
  u(),
  p(0, c("RemoteURLSerializerProvider")),
  p(1, c("CristalApp")),
  p(2, c("DocumentService"))
], m);
export {
  m as NextcloudInternalLinkSerializer
};
//# sourceMappingURL=nextcloud-internal-link-serializer-fxgRNT4h.js.map
