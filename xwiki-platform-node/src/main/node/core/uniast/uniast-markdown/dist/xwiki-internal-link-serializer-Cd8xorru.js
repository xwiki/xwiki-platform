import { injectable as c } from "inversify";
var o = Object.getOwnPropertyDescriptor, v = (r, e, a, i) => {
  for (var n = i > 1 ? void 0 : i ? o(e, a) : e, t = r.length - 1, s; t >= 0; t--)
    (s = r[t]) && (n = s(n) || n);
  return n;
};
let l = class {
  async serialize(r, e, a) {
    return `[[${await a.convertInlineContents(
      r
    )}|${e.rawReference}]]`;
  }
  async serializeImage(r, e) {
    return `![[${e ?? ""}${e ? "|" : ""}${r.rawReference}]]`;
  }
};
l = v([
  c()
], l);
export {
  l as XWikiInternalLinkSerializer
};
//# sourceMappingURL=xwiki-internal-link-serializer-Cd8xorru.js.map
