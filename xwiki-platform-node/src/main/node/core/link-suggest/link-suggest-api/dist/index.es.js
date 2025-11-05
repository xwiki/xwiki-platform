import { injectable as g, inject as a } from "inversify";
var l = Object.getOwnPropertyDescriptor, p = (e, t, r, i) => {
  for (var s = i > 1 ? void 0 : i ? l(t, r) : t, n = e.length - 1, c; n >= 0; n--)
    (c = e[n]) && (s = c(s) || s);
  return s;
}, v = (e, t) => (r, i) => t(r, i, e);
let o = class {
  constructor(e) {
    this.cristalApp = e;
  }
  get(e) {
    const t = e || this.cristalApp.getWikiConfig().getType();
    try {
      return this.cristalApp.getContainer().get("LinkSuggestService", { name: t });
    } catch (r) {
      this.cristalApp.getLogger("link.suggest.api").warn(
        `Couldn't resolve LinkSuggestService for type=[${t}]`,
        r
      );
      return;
    }
  }
};
o = p([
  g(),
  v(0, a("CristalApp"))
], o);
class A {
  constructor(t) {
    t.bind("LinkSuggestServiceProvider").to(o).inSingletonScope();
  }
}
var u = /* @__PURE__ */ ((e) => (e[e.PAGE = 0] = "PAGE", e[e.ATTACHMENT = 1] = "ATTACHMENT", e))(u || {});
const C = "LinkSuggestService";
export {
  A as ComponentInit,
  u as LinkType,
  C as name
};
//# sourceMappingURL=index.es.js.map
