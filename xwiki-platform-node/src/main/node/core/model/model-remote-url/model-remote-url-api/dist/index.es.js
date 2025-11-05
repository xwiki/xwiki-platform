import { injectable as n, inject as c } from "inversify";
var g = Object.getOwnPropertyDescriptor, v = (e, r, t, o) => {
  for (var i = o > 1 ? void 0 : o ? g(r, t) : r, a = e.length - 1, s; a >= 0; a--)
    (s = e[a]) && (i = s(i) || i);
  return i;
}, m = (e, r) => (t, o) => r(t, o, e);
let p = class {
  constructor(e) {
    this.cristalApp = e;
  }
  get(e) {
    const r = e || this.cristalApp.getWikiConfig().getType();
    try {
      return this.cristalApp.getContainer().get("RemoteURLParser", { name: r });
    } catch (t) {
      this.cristalApp.getLogger("remote-url.api").warn(`Couldn't resolve RemoteURLParser for type=[${r}]`, t);
      return;
    }
  }
};
p = v([
  n(),
  m(0, c("CristalApp"))
], p);
var u = Object.getOwnPropertyDescriptor, R = (e, r, t, o) => {
  for (var i = o > 1 ? void 0 : o ? u(r, t) : r, a = e.length - 1, s; a >= 0; a--)
    (s = e[a]) && (i = s(i) || i);
  return i;
}, P = (e, r) => (t, o) => r(t, o, e);
let l = class {
  constructor(e) {
    this.cristalApp = e;
  }
  get(e) {
    const r = e || this.cristalApp.getWikiConfig().getType();
    try {
      return this.cristalApp.getContainer().get("RemoteURLSerializer", { name: r });
    } catch (t) {
      this.cristalApp.getLogger("remote-url.api").warn(
        `Couldn't resolve RemoteURLSerializer for type=[${r}]`,
        t
      );
      return;
    }
  }
};
l = R([
  n(),
  P(0, c("CristalApp"))
], l);
class h {
  constructor(r) {
    r.bind("RemoteURLParserProvider").to(p).inSingletonScope(), r.bind("RemoteURLSerializerProvider").to(l).inSingletonScope();
  }
}
export {
  h as ComponentInit
};
//# sourceMappingURL=index.es.js.map
