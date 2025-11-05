import { DocumentReference as v, EntityType as s } from "@xwiki/cristal-model-api";
import { injectable as i, inject as f } from "inversify";
var u = Object.getOwnPropertyDescriptor, P = (e, r, t, o) => {
  for (var n = o > 1 ? void 0 : o ? u(r, t) : r, a = e.length - 1, l; a >= 0; a--)
    (l = e[a]) && (n = l(n) || n);
  return n;
};
let c = class {
  createDocumentReference(e, r) {
    return new v(e, r);
  }
  getTitle(e) {
    switch (e.type) {
      case s.WIKI:
        return e.name;
      case s.SPACE:
        return [...e.names].pop();
      case s.DOCUMENT:
        return e.name;
      case s.ATTACHMENT:
        return e.name;
    }
    return "";
  }
};
c = P([
  i()
], c);
var _ = Object.getOwnPropertyDescriptor, m = (e, r, t, o) => {
  for (var n = o > 1 ? void 0 : o ? _(r, t) : r, a = e.length - 1, l; a >= 0; a--)
    (l = e[a]) && (n = l(n) || n);
  return n;
}, h = (e, r) => (t, o) => r(t, o, e);
let p = class {
  constructor(e) {
    this.cristalApp = e;
  }
  get(e) {
    const r = this.cristalApp.getContainer(), t = e || this.cristalApp.getWikiConfig().getType();
    return r.isBound("ModelReferenceHandler", { name: t }) ? r.get("ModelReferenceHandler", { name: t }) : r.get("ModelReferenceHandler");
  }
};
p = m([
  i(),
  h(0, f("CristalApp"))
], p);
var C = Object.getOwnPropertyDescriptor, y = (e, r, t, o) => {
  for (var n = o > 1 ? void 0 : o ? C(r, t) : r, a = e.length - 1, l; a >= 0; a--)
    (l = e[a]) && (n = l(n) || n);
  return n;
}, A = (e, r) => (t, o) => r(t, o, e);
let d = class {
  constructor(e) {
    this.cristalApp = e;
  }
  get(e) {
    const r = e || this.cristalApp.getWikiConfig().getType();
    try {
      return this.cristalApp.getContainer().get("ModelReferenceParser", { name: r });
    } catch (t) {
      this.cristalApp.getLogger("model-reference.api").warn(
        `Couldn't resolve ModelReferenceParser for type=[${r}]`,
        t
      );
      return;
    }
  }
};
d = y([
  i(),
  A(0, f("CristalApp"))
], d);
var M = Object.getOwnPropertyDescriptor, R = (e, r, t, o) => {
  for (var n = o > 1 ? void 0 : o ? M(r, t) : r, a = e.length - 1, l; a >= 0; a--)
    (l = e[a]) && (n = l(n) || n);
  return n;
}, D = (e, r) => (t, o) => r(t, o, e);
let g = class {
  constructor(e) {
    this.cristalApp = e;
  }
  get(e) {
    const r = e || this.cristalApp.getWikiConfig().getType();
    try {
      return this.cristalApp.getContainer().get("ModelReferenceSerializer", { name: r });
    } catch (t) {
      this.cristalApp.getLogger("model-reference.api").warn(
        `Couldn't resolve ModelReferenceSerializer for type=[${r}]`,
        t
      );
      return;
    }
  }
};
g = R([
  i(),
  D(0, f("CristalApp"))
], g);
class S {
  constructor(r) {
    r.bind("ModelReferenceHandler").to(c).inSingletonScope().whenDefault(), r.bind("ModelReferenceHandlerProvider").to(p).inSingletonScope(), r.bind("ModelReferenceParserProvider").to(d).inSingletonScope(), r.bind(
      "ModelReferenceSerializerProvider"
    ).to(g).inSingletonScope();
  }
}
export {
  S as ComponentInit
};
//# sourceMappingURL=index.es.js.map
