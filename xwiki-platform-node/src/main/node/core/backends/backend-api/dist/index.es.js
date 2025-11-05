import { injectable as l, unmanaged as c, inject as p } from "inversify";
var v = Object.getOwnPropertyDescriptor, u = (t, r, o, e) => {
  for (var i = e > 1 ? void 0 : e ? v(r, o) : r, s = t.length - 1, a; s >= 0; s--)
    (a = t[s]) && (i = a(i) || i);
  return i;
}, f = (t, r) => (o, e) => r(o, e, t);
let n = class {
  logger;
  // @ts-expect-error wikiConfig is temporarily undefined during class
  // initialization
  wikiConfig;
  constructor(t, r) {
    this.logger = t, this.logger.setModule(r);
  }
  setWikiConfig(t) {
    this.logger.debug("Setting wiki Config: ", t), this.wikiConfig = t;
  }
  getWikiConfig() {
    return this.wikiConfig;
  }
};
n = u([
  l(),
  f(0, c())
], n);
var _ = Object.getOwnPropertyDescriptor, C = (t, r, o, e) => {
  for (var i = e > 1 ? void 0 : e ? _(r, o) : r, s = t.length - 1, a; s >= 0; s--)
    (a = t[s]) && (i = a(i) || i);
  return i;
}, h = (t, r) => (o, e) => r(o, e, t);
let g = class {
  constructor(t) {
    this.cristalApp = t;
  }
  get() {
    return this.cristalApp.getWikiConfig().storage;
  }
};
g = C([
  l(),
  h(0, p("CristalApp"))
], g);
class w {
  constructor(r) {
    r.bind("StorageProvider").to(g).inSingletonScope();
  }
}
export {
  n as AbstractStorage,
  w as ComponentInit
};
//# sourceMappingURL=index.es.js.map
