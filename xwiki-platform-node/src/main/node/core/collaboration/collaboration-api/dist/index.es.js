import { injectable as s, inject as g } from "inversify";
const i = "collaborationManager", p = "collaborationManagerProvider";
var v = Object.getOwnPropertyDescriptor, C = (e, n, t, r) => {
  for (var o = r > 1 ? void 0 : r ? v(n, t) : n, a = e.length - 1, l; a >= 0; a--)
    (l = e[a]) && (o = l(o) || o);
  return o;
}, d = (e, n) => (t, r) => n(t, r, e);
let c = class {
  constructor(e) {
    this.cristalApp = e;
  }
  get() {
    const e = this.cristalApp.getWikiConfig().realtimeHint, n = this.cristalApp.getContainer();
    return e && n.isBound(i, { name: e }) ? n.get(i, { name: e }) : n.get(i);
  }
};
c = C([
  s(),
  d(0, g("CristalApp"))
], c);
class f {
  constructor(n) {
    n.bind(p).to(c).inSingletonScope();
  }
}
var m = /* @__PURE__ */ ((e) => (e[e.Disconnected = 0] = "Disconnected", e[e.Connecting = 1] = "Connecting", e[e.Connected = 2] = "Connected", e))(m || {});
export {
  f as ComponentInit,
  m as Status,
  i as collaborationManagerName,
  p as collaborationManagerProviderName
};
//# sourceMappingURL=index.es.js.map
