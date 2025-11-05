import { defineAsyncComponent as m } from "vue";
import { injectable as o, inject as d, optional as p } from "inversify";
function S(e, t, s) {
  e.component(t, m(s));
}
class a {
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  jsonld;
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  constructor(t) {
    this.jsonld = t;
  }
  getIdentifier() {
    return this.jsonld.identifier;
  }
  setIdentifier(t) {
    this.jsonld.identifier = t;
  }
  getName() {
    return this.jsonld.name;
  }
  setName(t) {
    this.jsonld.name = t;
  }
  getText() {
    return this.jsonld.text;
  }
  setText(t) {
    this.jsonld.text = t;
  }
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  get(t) {
    return this.jsonld[t];
  }
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  set(t, s) {
    this.jsonld[t] = s;
  }
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  getSource() {
    return this.jsonld;
  }
}
class _ {
  id;
  name;
  mode = "";
  source;
  syntax;
  html;
  document;
  css;
  js;
  version;
  headline = "";
  headlineRaw = "";
  lastModificationDate;
  lastAuthor;
  canEdit = !0;
  constructor(t = "", s = "", i = "", n = "") {
    this.document = new a({}), this.source = i, this.syntax = n, this.html = "", this.name = s, this.id = t, this.css = [], this.js = [], this.version = "";
  }
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  toObject() {
    return {
      id: this.id,
      name: this.name,
      source: this.source,
      syntax: this.syntax,
      html: this.html,
      document: this.document.getSource(),
      css: this.css,
      js: this.js,
      version: this.version
    };
  }
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  fromObject(t) {
    this.id = t.id, this.name = t.name, this.source = t.source, this.syntax = t.syntax, this.html = t.html, this.document = new a(t.document), this.css = t.css, this.js = t.js, this.version = t.version;
  }
}
class x {
  constructor() {
  }
}
var L = Object.getOwnPropertyDescriptor, C = (e, t, s, i) => {
  for (var n = i > 1 ? void 0 : i ? L(t, s) : t, l = e.length - 1, r; l >= 0; l--)
    (r = e[l]) && (n = r(n) || n);
  return n;
}, g = (e, t) => (s, i) => t(s, i, e);
let f = class {
  constructor(e) {
    this.loggerConfig = e;
  }
  // @ts-expect-error module is temporarily undefined during class
  // initialization
  module;
  setModule(e) {
    this.module = e;
  }
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  debug(...e) {
    (!this.loggerConfig || this.loggerConfig.hasLevelId(this.module, 4)) && e.unshift(this.module + ":"), console.debug.apply(null, e);
  }
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  info(...e) {
    e.unshift(this.module + ":"), console.info.apply(null, e);
  }
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  warn(...e) {
    e.unshift(this.module + ":"), console.warn.apply(null, e);
  }
  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  error(...e) {
    e.unshift(this.module + ":"), console.error.apply(null, e);
  }
};
f = C([
  o(),
  g(0, d("LoggerConfig")),
  g(0, p())
], f);
var D = Object.getOwnPropertyDescriptor, w = (e, t, s, i) => {
  for (var n = i > 1 ? void 0 : i ? D(t, s) : t, l = e.length - 1, r; l >= 0; l--)
    (r = e[l]) && (n = r(n) || n);
  return n;
};
let h = class {
  config;
  computedConfig;
  defaultLevel;
  // @ts-expect-error defaultLevelId is temporarily undefined during class
  // initialization
  defaultLevelId;
  levels = /* @__PURE__ */ new Map();
  constructor(e = "error") {
    this.config = /* @__PURE__ */ new Map(), this.defaultLevel = e, this.computedConfig = /* @__PURE__ */ new Map(), this.levels.set("error", 1), this.levels.set("warn", 2), this.levels.set("info", 3), this.levels.set("debug", 4);
  }
  addLevel(e, t) {
    const s = this.levels.get(t);
    s != null && this.config.forEach((i) => {
      if (i.startsWith(e)) {
        const n = this.computedConfig.get(i);
        (n == null || s && s > n) && s && this.computedConfig.set(i, s);
      }
    });
  }
  getLevels() {
    return this.config;
  }
  getLevel(e) {
    let t = this.config.get(e);
    return t || (t = this.defaultLevel), t;
  }
  getLevelId(e) {
    const t = this.levels.get(e);
    return t || 10;
  }
  setDefaultLevel(e) {
    this.defaultLevel = e, this.defaultLevelId = this.getLevelId(this.defaultLevel);
  }
  getDefaultLevel() {
    return this.defaultLevel;
  }
  getDefaultLevelId() {
    return this.defaultLevelId ? this.defaultLevelId : (this.defaultLevelId = this.getLevelId(this.defaultLevel), this.defaultLevelId);
  }
  hasLevel(e, t) {
    let s = this.computedConfig.get(e);
    s || (s = this.getDefaultLevelId());
    const i = this.getLevelId(t);
    return !!(i && s >= i);
  }
  hasLevelId(e, t) {
    let s = this.computedConfig.get(e);
    s || (s = this.getDefaultLevelId());
    const i = t;
    return !!(i && s >= i);
  }
};
h = w([
  o()
], h);
var O = Object.getOwnPropertyDescriptor, j = (e, t, s, i) => {
  for (var n = i > 1 ? void 0 : i ? O(t, s) : t, l = e.length - 1, r; l >= 0; l--)
    (r = e[l]) && (n = r(n) || n);
  return n;
}, I = (e, t) => (s, i) => t(s, i, e);
let u = class {
  // @ts-expect-error name is temporarily undefined during class
  // initialization
  name;
  // @ts-expect-error baseURL is temporarily undefined during class
  // initialization
  baseURL;
  // @ts-expect-error baseRestURL is temporarily undefined during class
  // initialization
  baseRestURL;
  /**
   * Realtime endpoint URL.
   * @since 0.11
   * @beta
   */
  realtimeURL;
  /**
   * Authentication server base URL.
   * @since 0.15
   * @beta
   */
  authenticationBaseURL;
  /**
   * Authentication Manager component to use.
   * By default, resolves to configuration type.
   * @since 0.16
   * @beta
   */
  authenticationManager;
  // @ts-expect-error homePage is temporarily undefined during class
  // initialization
  homePage;
  // @ts-expect-error storage is temporarily undefined during class
  // initialization
  storage;
  // @ts-expect-error serverRendering is temporarily undefined during class
  // initialization
  serverRendering;
  // @ts-expect-error designSystem is temporarily undefined during class
  // initialization
  designSystem;
  // @ts-expect-error editor is temporarily undefined during class
  // initialization
  editor;
  // @ts-expect-error offline is temporarily undefined during class
  // initialization
  offline;
  offlineSetup;
  /**
   * Root location to store pages.
   * @since 0.16
   * @beta
   */
  storageRoot;
  // @ts-expect-error cristal is temporarily undefined during class
  // initialization
  cristal;
  logger;
  constructor(e) {
    this.logger = e, this.logger.setModule("storage.components.defaultWikiStorage"), this.offlineSetup = !1;
  }
  setConfig(e, t, s, i, n, l, r, c, v) {
    Object.assign(this, {
      name: e,
      baseURL: t,
      baseRestURL: s,
      homePage: i,
      serverRendering: n,
      designSystem: l,
      offline: r,
      editor: c,
      ...v
    });
  }
  setConfigFromObject(e) {
    Object.assign(this, e);
  }
  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  setupOfflineStorage() {
    if (this.logger.debug("Checking offline storage"), this.offline && this.cristal) {
      this.logger.debug("Looking for wrapping offline storage");
      const e = this.cristal.getContainer().get("WrappingStorage");
      e ? (this.logger.debug("Offline local storage is ready"), e.setStorage(this.storage), e.setWikiConfig(this.storage.getWikiConfig()), this.storage = e) : this.logger.debug("Failed Looking for wrapping offline storage");
    } else
      this.cristal || this.logger.debug("Cristal not initialized"), this.offline || this.logger.debug("Offline mode not activated");
  }
  isSupported(e) {
    return e == "html";
  }
  initialize() {
    this.offline && !this.offlineSetup && (this.setupOfflineStorage(), this.offlineSetup = !0, this.storage.isStorageReady());
  }
  defaultPageName() {
    return "index";
  }
  getType() {
    return "Default";
  }
  getNewPageDefaultName() {
    return "newpage";
  }
};
u = j([
  o(),
  I(0, d("Logger"))
], u);
export {
  x as ComponentInit,
  f as DefaultLogger,
  h as DefaultLoggerConfig,
  _ as DefaultPageData,
  u as DefaultWikiConfig,
  a as JSONLDDocument,
  S as registerAsyncComponent
};
//# sourceMappingURL=index.es.js.map
