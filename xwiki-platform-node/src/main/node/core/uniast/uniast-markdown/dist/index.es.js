import { gfmStrikethroughFromMarkdown as F } from "mdast-util-gfm-strikethrough";
import { gfmTableFromMarkdown as A } from "mdast-util-gfm-table";
import { gfmTaskListItemFromMarkdown as R } from "mdast-util-gfm-task-list-item";
import { gfmStrikethrough as $ } from "micromark-extension-gfm-strikethrough";
import { gfmTable as z } from "micromark-extension-gfm-table";
import { gfmTaskListItem as S } from "micromark-extension-gfm-task-list-item";
import { injectable as g, inject as d } from "inversify";
import { assertUnreachable as v, assertInArray as _, tryFallibleOrError as O } from "@xwiki/cristal-fn-utils";
import { EntityType as w } from "@xwiki/cristal-model-api";
import j from "remark-parse";
import { unified as E } from "unified";
function x(e, t) {
  let r = null;
  for (const { name: n, match: a } of t) {
    const i = e.indexOf(a);
    i !== -1 && (r === null || r.offset > i) && (r = { name: n, match: a, offset: i });
  }
  return r;
}
function D() {
  const e = this.data();
  e.micromarkExtensions ??= [], e.fromMarkdownExtensions ??= [], e.micromarkExtensions.push(
    $(),
    z(),
    S()
  ), e.fromMarkdownExtensions.push(
    F(),
    A(),
    R()
  );
}
var N = Object.getOwnPropertyDescriptor, B = (e, t, r, n) => {
  for (var a = n > 1 ? void 0 : n ? N(t, r) : t, i = e.length - 1, o; i >= 0; i--)
    (o = e[i]) && (a = o(a) || a);
  return a;
}, U = (e, t) => (r, n) => t(r, n, e);
let b = class {
  constructor(e) {
    this.cristalApp = e;
  }
  get() {
    const e = this.cristalApp.getWikiConfig().getType();
    try {
      return this.cristalApp.getContainer().get("Factory<MarkdownParserConfiguration>", { name: e })();
    } catch {
      return {
        supportFlexmarkInternalLinks: !1
      };
    }
  }
};
b = B([
  g(),
  U(0, d("CristalApp"))
], b);
var H = Object.getOwnPropertyDescriptor, W = (e, t, r, n) => {
  for (var a = n > 1 ? void 0 : n ? H(t, r) : t, i = e.length - 1, o; i >= 0; i--)
    (o = e[i]) && (a = o(a) || a);
  return a;
}, k = (e, t) => (r, n) => t(r, n, e);
let P = class {
  constructor(e, t, r) {
    this.modelReferenceParserProvider = e, this.modelReferenceHandlerProvider = t, this.parserConfigurationResolver = r;
  }
  async parseMarkdown(e) {
    const t = E().use(j).use(D).parse(e);
    try {
      return { blocks: await Promise.all(
        t.children.map((n) => this.convertBlock(n))
      ) };
    } catch (r) {
      return r instanceof Error ? r : new Error(String(r));
    }
  }
  async convertBlock(e) {
    switch (e.type) {
      case "paragraph": {
        const t = await this.collectInlineContent(e.children, {});
        return t.length === 1 && t[0].type === "inlineMacro" ? {
          type: "macroBlock",
          name: t[0].name,
          params: t[0].params
        } : {
          type: "paragraph",
          content: t,
          styles: {}
        };
      }
      case "heading":
        return {
          type: "heading",
          level: _(
            e.depth,
            [1, 2, 3, 4, 5, 6],
            "Invalid heading depth in markdown parser"
          ),
          content: await this.collectInlineContent(e.children, {}),
          styles: {}
        };
      case "blockquote":
        return {
          type: "quote",
          content: await Promise.all(
            e.children.map((t) => this.convertBlock(t))
          ),
          styles: {}
        };
      case "list":
        return {
          type: "list",
          items: await Promise.all(
            e.children.map(async (t, r) => ({
              number: e.ordered ? (e.start ?? 1) + r : void 0,
              checked: t.checked ?? void 0,
              content: await Promise.all(
                t.children.map((n) => this.convertBlock(n))
              ),
              styles: {}
            }))
          ),
          styles: {}
        };
      case "code":
        return {
          type: "code",
          content: e.value,
          language: e.lang ?? void 0
        };
      case "table": {
        const [t, ...r] = e.children, n = await Promise.all(
          t?.children.map(
            async (i) => ({
              headerCell: {
                content: await this.collectInlineContent(i.children, {}),
                styles: {}
              }
            })
          )
        ), a = await Promise.all(
          r.map(async (i) => {
            const o = i.children.map(
              async (s) => ({
                content: await this.collectInlineContent(s.children, {}),
                styles: {}
              })
            );
            return await Promise.all(o);
          })
        );
        return {
          type: "table",
          columns: n,
          rows: a,
          styles: {}
        };
      }
      case "image":
        return {
          type: "image",
          ...await this.convertImage(e)
        };
      case "break":
      case "thematicBreak":
        return { type: "break" };
      case "imageReference":
      case "linkReference":
      case "definition":
      case "footnoteDefinition":
      case "footnoteReference":
      case "html":
        throw new Error("TODO: handle blocks of type " + e.type);
      // NOTE: These are handled in the `convertInline` function below
      case "text":
      case "delete":
      case "strong":
      case "emphasis":
      case "inlineCode":
      case "link":
      case "tableCell":
      case "tableRow":
      case "yaml":
      case "listItem":
        throw new Error(
          "Unexpected block type in markdown parser: " + e.type
        );
      default:
        v(e);
    }
  }
  async convertInline(e, t) {
    switch (e.type) {
      case "image":
        return [
          {
            type: "image",
            ...await this.convertImage(e)
          }
        ];
      case "strong":
        return this.collectInlineContent(e.children, {
          ...t,
          bold: !0
        });
      case "emphasis":
        return this.collectInlineContent(e.children, {
          ...t,
          italic: !0
        });
      case "delete":
        return this.collectInlineContent(e.children, {
          ...t,
          strikethrough: !0
        });
      case "inlineCode":
        return [
          {
            type: "text",
            content: e.value,
            styles: {}
          }
        ];
      case "text":
        return this.convertText(e.value, t);
      case "html":
      case "footnoteReference":
      case "linkReference":
      case "imageReference":
      case "break":
        throw new Error("TODO: handle inlines of type " + e.type);
      case "link":
        return await this.convertLink(e, t);
      default:
        v(e);
    }
  }
  async convertLink(e, t) {
    let r;
    if (this.supportFlexmark())
      r = { type: "external", url: e.url };
    else
      try {
        r = {
          type: "internal",
          parsedReference: await this.modelReferenceParserProvider.get().parseAsync(e.url),
          rawReference: e.url
        };
      } catch (a) {
        console.debug("Error parsing reference: ", a), r = { type: "external", url: e.url };
      }
    return [
      {
        type: "link",
        content: (await this.collectInlineContent(e.children, t)).map((a) => {
          if (a.type !== "text")
            throw new Error("Unexpected link inside link in markdown parser");
          return a;
        }),
        target: r
      }
    ];
  }
  async convertImage(e) {
    let t;
    const r = e.url;
    try {
      t = {
        type: "internal",
        parsedReference: await this.modelReferenceParserProvider.get().parseAsync(r, { type: w.ATTACHMENT }),
        rawReference: r
      };
    } catch {
      t = { type: "external", url: r };
    }
    return {
      target: t,
      caption: void 0,
      alt: e.alt ?? void 0,
      styles: {}
    };
  }
  // eslint-disable-next-line max-statements
  convertText(e, t) {
    const r = [];
    let n = 0;
    for (; ; ) {
      const i = [...this.supportFlexmark() ? [
        { name: "image", match: "![[" },
        { name: "link", match: "[[" }
      ] : [], { name: "macro", match: "{{" }], o = x(e.substring(n), i);
      if (!o)
        break;
      const s = n + o.offset, l = e.substring(0, s).match(/\\+/);
      if (!(l && l[0].length % 2 !== 0))
        switch (e.substring(n, s).length > 0 && r.push({
          type: "text",
          content: e.substring(n, s),
          styles: t
        }), o.name) {
          case "image":
          case "link": {
            n = this.handleLinkOrImage(
              o,
              s,
              e,
              n,
              t,
              r
            );
            break;
          }
          case "macro": {
            n = this.handleMacro(o, s, e, n, r);
            break;
          }
          default:
            v(o.name);
        }
    }
    return e.substring(n).length > 0 && r.push({
      type: "text",
      content: e.substring(n),
      styles: t
    }), r;
  }
  // eslint-disable-next-line max-statements
  handleLinkOrImage(e, t, r, n, a, i) {
    const o = e.name === "image";
    let s, l = !1, c = !1, p = !1;
    for (s = t + e.match.length; s < r.length; s++) {
      if (l) {
        l = !1;
        continue;
      }
      const M = r.charAt(s);
      if (M === "\\") {
        l = !0;
        continue;
      }
      if (M === "]") {
        if (c) {
          p = !0;
          break;
        }
        c = !0;
      }
    }
    if (!p)
      return n;
    n = s + 1;
    const m = r.substring(t + e.match.length, s - 1);
    let u, h;
    const y = m.indexOf("|");
    y !== -1 ? (u = m.substring(0, y), h = m.substring(y + 1)) : (u = null, h = m);
    let f;
    try {
      f = this.modelReferenceParserProvider.get().parse(h, {
        type: o ? w.ATTACHMENT : w.DOCUMENT
      });
    } catch {
      f = null;
    }
    const L = {
      type: "internal",
      rawReference: h,
      parsedReference: f
    };
    u ??= f ? this.modelReferenceHandlerProvider.get().getTitle(f) : "<invalid reference>";
    const T = o ? {
      type: "image",
      target: L,
      styles: { alignment: "left" },
      alt: u
    } : {
      type: "link",
      target: L,
      content: [{ type: "text", content: u, styles: a }]
    };
    return i.push(T), n;
  }
  // eslint-disable-next-line max-statements
  handleMacro(e, t, r, n, a) {
    const i = r.substring(t + e.match.length).match(
      // This weird group matches valid accentuated Unicode letters
      /\s*([A-Za-zÀ-ÖØ-öø-ÿ\d]+)(\s+(?=[A-Za-zÀ-ÖØ-öø-ÿ\d/])|(?=\/))/
    );
    if (!i)
      return n = t + e.match.length, a.push({ type: "text", content: e.match, styles: {} }), n;
    const o = i[1];
    let s, l = !1, c = null;
    const p = {};
    let m = !1;
    for (s = t + e.match.length + i[0].length; s < r.length; s++) {
      if (l) {
        if (!c || c.value === null)
          throw new Error("Unexpected");
        l = !1, c.value += r[s];
        continue;
      }
      if (!c) {
        if (r[s] === " ")
          continue;
        if (r[s].match(/[A-Za-zÀ-ÖØ-öø-ÿ_\d]/)) {
          c = { name: r[s], value: null };
          continue;
        }
        if (r[s] === "/") {
          m = !0;
          break;
        }
        break;
      }
      if (c.value === null) {
        if (r[s].match(/[A-Za-zÀ-ÖØ-öø-ÿ_\d]/)) {
          c.name += r[s];
          continue;
        }
        if (r[s] === "=") {
          if (r[s + 1] === '"') {
            s += 1, c.value = "";
            continue;
          }
          const h = r.substring(s + 1).match(/\d+(?=[^A-Za-zÀ-ÖØ-öø-ÿ\d])/);
          if (!h)
            break;
          p[c.name] = h[0], c = null, s += h[0].length;
          continue;
        }
        break;
      }
      r[s] === "\\" ? l = !0 : r[s] === '"' ? (p[c.name] = c.value, c = null) : c.value += r[s];
    }
    const u = r.substring(s).match(/\s*}}/);
    return !m || !u ? (n = t + e.match.length, a.push({ type: "text", content: e.match, styles: {} })) : (n = s + 1 + u[0].length, a.push({
      type: "inlineMacro",
      name: o,
      params: p
    })), n;
  }
  supportFlexmark() {
    return this.parserConfigurationResolver.get().supportFlexmarkInternalLinks;
  }
  async collectInlineContent(e, t = {}) {
    return (await Promise.all(
      e.map((r) => this.convertInline(r, t))
    )).flat();
  }
};
P = W([
  g(),
  k(0, d("ModelReferenceParserProvider")),
  k(1, d("ModelReferenceHandlerProvider")),
  k(2, d("ParserConfigurationResolver"))
], P);
var G = Object.getOwnPropertyDescriptor, X = (e, t, r, n) => {
  for (var a = n > 1 ? void 0 : n ? G(t, r) : t, i = e.length - 1, o; i >= 0; i--)
    (o = e[i]) && (a = o(a) || a);
  return a;
}, Z = (e, t) => (r, n) => t(r, n, e);
let I = class {
  constructor(e) {
    this.cristalApp = e;
  }
  async get() {
    const e = this.cristalApp.getWikiConfig().getType();
    try {
      return (await this.cristalApp.getContainer().getAsync("Factory<InternalLinksSerializer>", { name: e }))();
    } catch (t) {
      throw console.debug(t), new Error(`Could not resolve serializer for type ${e}`);
    }
  }
};
I = X([
  g(),
  Z(0, d("CristalApp"))
], I);
var q = Object.getOwnPropertyDescriptor, J = (e, t, r, n) => {
  for (var a = n > 1 ? void 0 : n ? q(t, r) : t, i = e.length - 1, o; i >= 0; i--)
    (o = e[i]) && (a = o(a) || a);
  return a;
}, K = (e, t) => (r, n) => t(r, n, e);
let C = class {
  constructor(e) {
    this.internalLinksSerializerResolver = e;
  }
  /**
   * Converts the provided AST to Markdown.
   *
   * @param uniAst - the AST to convert to markdown
   *
   * understand the impacts
   */
  async toMarkdown(e) {
    const { blocks: t } = e, r = [];
    for (const n of t) {
      const a = O(() => this.blockToMarkdown(n));
      if (a instanceof Error)
        return a;
      r.push(a);
    }
    return (await Promise.all(r)).join(`

`);
  }
  async blockToMarkdown(e) {
    switch (e.type) {
      case "paragraph":
        return this.convertInlineContents(e.content);
      case "heading":
        return `${"#".repeat(e.level)} ${await this.convertInlineContents(e.content)}`;
      case "list":
        return (await Promise.all(
          e.items.map((t) => this.convertListItem(t))
        )).join(`
`);
      case "quote": {
        const t = e.content.map((r) => this.blockToMarkdown(r)).flatMap(async (r) => (await r).split(`
`)).flatMap(async (r) => (await r).map((a) => `> ${a}`).join(`
`));
        return (await Promise.all(t)).join(`
`);
      }
      case "code":
        return `\`\`\`${e.language ?? ""}
${e.content}
\`\`\``;
      case "table":
        return this.convertTable(e);
      case "image":
        return this.convertImage(e);
      case "break":
        return "---";
      case "macroBlock":
        return this.convertMacro(e.name, e.params);
    }
  }
  async convertListItem(e) {
    let t = e.number !== void 0 ? `${e.number}. ` : "* ";
    e.checked !== void 0 && (t += `[${e.checked ? "x" : " "}] `);
    const r = [];
    for (const n of e.content) {
      const i = (await this.blockToMarkdown(n)).split(`
`);
      r.push(
        i.map((o, s) => (s > 0 ? " ".repeat(t.length) : "") + o).join(`
`)
      );
    }
    return `${t}${r.join(`
`)}`;
  }
  async convertImage(e) {
    return e.target.type === "external" ? `![${e.alt ?? ""}](${e.target.url})` : await (await this.internalLinksSerializerResolver.get()).serializeImage(e.target, e.alt);
  }
  async convertTable(e) {
    const { columns: t, rows: r } = e, n = [
      (await Promise.all(
        t.map(
          (a) => a.headerCell ? this.convertTableCell(a.headerCell) : ""
        )
      )).join(" | "),
      t.map(() => " - ").join(" | ")
    ];
    for (const a of r)
      n.push(
        (await Promise.all(a.map((i) => this.convertTableCell(i)))).join(" | ")
      );
    return n.map((a) => `| ${a} |`).join(`
`);
  }
  convertTableCell(e) {
    return this.convertInlineContents(e.content);
  }
  async convertInlineContents(e) {
    return (await Promise.all(
      e.map((t) => this.convertInlineContent(t))
    )).join("");
  }
  async convertInlineContent(e) {
    switch (e.type) {
      case "text":
        return this.convertText(e);
      case "image":
        return this.convertImage(e);
      case "link":
        return this.convertLink(e);
      case "inlineMacro":
        return this.convertMacro(e.name, e.params);
    }
  }
  async convertLink(e) {
    switch (e.target.type) {
      case "external":
        return `[${await this.convertInlineContents(e.content)}](${e.target.url})`;
      case "internal":
        return (await this.internalLinksSerializerResolver.get()).serialize(
          e.content,
          e.target,
          this
        );
    }
  }
  convertMacro(e, t) {
    return `{{${e}${Object.entries(t).map(
      ([r, n]) => ` ${r}="${n.toString().replace(/\\/g, "\\\\\\").replace(/"/g, '\\\\"')}"`
    ).join("")} /}}`;
  }
  // eslint-disable-next-line max-statements
  convertText(e) {
    const { content: t, styles: r } = e, { bold: n, italic: a, strikethrough: i, code: o } = r, s = [];
    return o && s.push("`"), i && s.push("~~"), a && s.push("_"), n && s.push("**"), `${s.join("")}${t}${s.reverse().join("")}`;
  }
};
C = J([
  g(),
  K(0, d("InternalLinksSerializerResolver"))
], C);
const Q = "MarkdownToUniAstConverter", V = "UniAstToMarkdownConverter";
class ue {
  constructor(t) {
    t.bind(Q).to(P).whenDefault(), t.bind(V).to(C).whenDefault(), t.bind("InternalLinksSerializerResolver").to(I), this.initXWikiFactory(t), this.initNextcloudFactory(t), this.initGitHubFactory(t), this.initFileSystemFactory(t), t.bind("ParserConfigurationResolver").to(b).whenDefault(), t.bind(
      "Factory<MarkdownParserConfiguration>"
    ).toFactory(() => () => ({
      supportFlexmarkInternalLinks: !0
    })).whenNamed("XWiki");
  }
  initXWikiFactory(t) {
    const r = "XWiki";
    t.bind(
      "Factory<InternalLinksSerializer>"
    ).toFactory((n) => async () => {
      const a = (await import("./xwiki-internal-link-serializer-Cd8xorru.js")).XWikiInternalLinkSerializer;
      return this.bindAndLoad(t, r, a, n);
    }).whenNamed(r);
  }
  initNextcloudFactory(t) {
    const r = "Nextcloud";
    t.bind(
      "Factory<InternalLinksSerializer>"
    ).toFactory((n) => async () => {
      const a = (await import("./nextcloud-internal-link-serializer-fxgRNT4h.js")).NextcloudInternalLinkSerializer;
      return this.bindAndLoad(t, r, a, n);
    }).whenNamed(r);
  }
  initGitHubFactory(t) {
    const r = "GitHub";
    t.bind(
      "Factory<InternalLinksSerializer>"
    ).toFactory((n) => async () => {
      const a = (await import("./github-internal-link-serializer-Cr_EgYzo.js")).GitHubInternalLinkSerializer;
      return this.bindAndLoad(t, r, a, n);
    }).whenNamed(r);
  }
  initFileSystemFactory(t) {
    const r = "FileSystem";
    t.bind(
      "Factory<InternalLinksSerializer>"
    ).toFactory((n) => async () => {
      const a = (await import("./filesystem-internal-link-serializer-De0Zjdfl.js")).FilesystemInternalLinkSerializer;
      return this.bindAndLoad(t, r, a, n);
    }).whenNamed(r);
  }
  /**
   * Registed the component in the container on demand.
   *
   * @param container - the container
   * @param name - the name of the component interface
   * @param component - the actual component to register
   * @param context - the context
   */
  bindAndLoad(t, r, n, a) {
    return t.isBound("InternalLinksSerializer", { name: r }) || t.bind("InternalLinksSerializer").to(n).whenNamed(r), a.get("InternalLinksSerializer", {
      name: r
    });
  }
}
export {
  ue as ComponentInit,
  Q as markdownToUniAstConverterName,
  V as uniAstToMarkdownConverterName
};
//# sourceMappingURL=index.es.js.map
