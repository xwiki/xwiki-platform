/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
import { applyXWikiParameters } from "./parameters";
import { Fragment, createElement } from "react";
import type { MacroCall } from "./utils";
import type { CSSProperties, ReactNode } from "react";

/**
 * A node in a server-rendered macro output tree: a BlockNote-shaped block/inline node, or a bare inline string.
 */
type OutputNode = RawNode | string;

/** A raw (JSON) BlockNote node, kept loosely typed since macro output freely mixes block and inline shapes. */
type RawNode = {
  type?: string;
  props?: Record<string, unknown>;
  children?: OutputNode[];
  content?: unknown;
  text?: string;
  styles?: Record<string, unknown>;
  href?: string;
  name?: string;
};

/** Guards against unbounded recursion on pathological data (a macro output referencing itself, corrupt fragments). */
const MAX_NESTING = 100;

/** BlockNote list-item block types, mapped to their HTML list container tag. */
const LIST_TAGS: Record<string, "ul" | "ol"> = {
  bulletListItem: "ul",
  numberedListItem: "ol",
  checkListItem: "ul",
  toggleListItem: "ul",
};

/**
 * Renders a server-rendered macro output (an array of BlockNote-shaped nodes) as React elements. Unlike a plain
 * BlockNote export, the output may freely mix inline content and block-level nodes under a single parent (XWiki
 * groups/DIVs allow this, the BlockNote schema does not) and may embed nested macro calls; React composition handles
 * both naturally. `xwikiEditable` markers are replaced with the matching value from the `call` (a parameter value or
 * the content); a nested `xwikiMacroBlock` / `xwikiInlineMacro` is rendered by this same component against its own
 * call/output, so nesting resolves to any depth.
 */
function MacroOutput({
  nodes,
  call,
  inline,
  depth = 0,
}: {
  readonly nodes: OutputNode[];
  readonly call: MacroCall;
  readonly inline: boolean;
  readonly depth?: number;
}): ReactNode {
  if (depth > MAX_NESTING) {
    return null;
  }
  return <>{renderSiblings(nodes, call, inline, depth, "")}</>;
}

/**
 * Renders a sibling array, grouping consecutive list-item blocks into their list container (`ul`/`ol`/`dl`) and
 * rendering every other node individually. This mirrors how BlockNote stores lists (flat sibling items with nested
 * `children`) while producing valid nested list markup.
 */
function renderSiblings(
  nodes: OutputNode[],
  call: MacroCall,
  inline: boolean,
  depth: number,
  keyPrefix: string,
): ReactNode[] {
  return segmentSiblings(nodes).map((segment, i) => {
    const key = `${keyPrefix}${i}`;
    return segment.kind === "list" ? (
      renderList(segment.type, segment.items, call, inline, depth, key)
    ) : (
      <Fragment key={key}>
        {renderNode(segment.node, call, inline, depth)}
      </Fragment>
    );
  });
}

/** A run of same-type consecutive list items, or a single non-list node. */
type Segment =
  | { kind: "list"; type: string; items: RawNode[] }
  | { kind: "node"; node: OutputNode };

/** Splits a sibling array into segments, merging consecutive same-type list items into a single list segment. */
function segmentSiblings(nodes: OutputNode[]): Segment[] {
  const segments: Segment[] = [];
  for (const node of nodes) {
    const listType = getListType(node);
    const last = segments.at(-1);
    if (listType && last?.kind === "list" && last.type === listType) {
      // Add to the current list segment.
      last.items.push(node as RawNode);
    } else if (listType) {
      // Start a new list segment.
      segments.push({ kind: "list", type: listType, items: [node as RawNode] });
    } else {
      // Single node segment (not a list).
      segments.push({ kind: "node", node });
    }
  }
  return segments;
}

/** @returns the list-item type of the node if it is one (so a run can be grouped), else undefined. */
function getListType(node: OutputNode): string | undefined {
  if (typeof node === "object" && node.type) {
    if (node.type in LIST_TAGS || node.type === "xwikiDefinitionListItem") {
      return node.type;
    }
  }
  return undefined;
}

/** Renders a run of same-type list items as a `ul`/`ol` (list items) or a `dl` (definition list items). */
function renderList(
  type: string,
  items: RawNode[],
  call: MacroCall,
  inline: boolean,
  depth: number,
  key: string,
): ReactNode {
  if (type === "xwikiDefinitionListItem") {
    return (
      <dl key={key}>
        {items.map((item, i) =>
          renderDefinitionItem(item, call, depth, `${key}.${i}`),
        )}
      </dl>
    );
  }
  const tag = LIST_TAGS[type];
  return createElement(
    tag,
    { key },
    items.map((item, i) => (
      <li key={i}>
        {type === "checkListItem" && (
          <input
            type="checkbox"
            checked={item.props?.checked === true}
            readOnly
          />
        )}
        {renderContent(item.content, call, true, depth, `${key}.${i}`)}
        {item.children?.length
          ? renderSiblings(
              item.children,
              call,
              inline,
              depth + 1,
              `${key}.${i}.`,
            )
          : null}
      </li>
    )),
  );
}

/** Renders a definition-list item as a `dt` (term) or `dd` (definition), with nested items inside the `dd`. */
function renderDefinitionItem(
  item: RawNode,
  call: MacroCall,
  depth: number,
  key: string,
): ReactNode {
  const content = renderContent(item.content, call, true, depth, key);
  if (item.props?.term === true) {
    return <dt key={key}>{content}</dt>;
  }
  return (
    <dd key={key}>
      {content}
      {item.children?.length
        ? renderSiblings(item.children, call, false, depth + 1, `${key}.`)
        : null}
    </dd>
  );
}

/** Renders a single (non-list) node to a React element. */
function renderNode(
  node: OutputNode,
  call: MacroCall,
  inline: boolean,
  depth: number,
): ReactNode {
  if (typeof node === "string") {
    return node;
  }
  switch (node.type) {
    case "xwikiEditable":
      return renderEditable(node, call, inline, depth);
    case "xwikiMacroBlock":
      return renderNestedMacro(node, false, depth);
    case "xwikiInlineMacro":
      return renderNestedMacro(node, true, depth);
    case "xwikiGroup":
      return (
        <div ref={xwikiParamsRef(node.props?.xwikiParameters)}>
          {renderSiblings(node.children ?? [], call, inline, depth + 1, "")}
        </div>
      );
    case "xwikiRaw":
      return renderRaw(node, inline);
    case "paragraph":
      return (
        <p style={blockStyle(node.props)}>
          {renderContent(node.content, call, true, depth, "")}
        </p>
      );
    case "heading":
      return createElement(
        `h${(node.props?.level as number) ?? 1}`,
        { style: blockStyle(node.props) },
        renderContent(node.content, call, true, depth, ""),
      );
    case "codeBlock":
      return (
        <pre>
          <code>
            {typeof node.content === "string"
              ? node.content
              : renderContent(node.content, call, true, depth, "")}
          </code>
        </pre>
      );
    case "quote":
      return (
        <blockquote style={blockStyle(node.props)}>
          {renderContent(node.content, call, true, depth, "")}
          {node.children?.length
            ? renderSiblings(node.children, call, false, depth + 1, "")
            : null}
        </blockquote>
      );
    case "table":
      return renderTable(node, call, depth);
    case "image":
      return (
        <img
          src={node.props?.url as string | undefined}
          alt={(node.props?.alt as string | undefined) ?? ""}
        />
      );
    case "text":
      return renderText(node);
    case "link":
      return (
        <a href={node.href}>
          {renderContent(node.content, call, true, depth, "") ||
            (node.props?.xwikiGeneratedLabel as string | undefined) ||
            node.href}
        </a>
      );
    default:
      // Unknown node: render its children / content best-effort so nothing is silently dropped.
      if (node.children?.length) {
        return (
          <div>
            {renderSiblings(node.children, call, inline, depth + 1, "")}
          </div>
        );
      }
      return renderContent(node.content, call, inline, depth, "");
  }
}

/** Renders a node's `content` (a string, a single inline node, or an inline-content array). */
function renderContent(
  content: unknown,
  call: MacroCall,
  inline: boolean,
  depth: number,
  keyPrefix: string,
): ReactNode {
  if (content === undefined || content === null) {
    return null;
  }
  if (typeof content === "string") {
    return content;
  }
  const nodes = Array.isArray(content)
    ? (content as OutputNode[])
    : [content as OutputNode];
  return renderSiblings(nodes, call, inline, depth, keyPrefix);
}

/**
 * Replaces an `xwikiEditable` marker with the matching value from the call: a named marker resolves to the parameter
 * value (matched case-insensitively), an unnamed one to the macro content. The value can itself be a fragment holding
 * nested macros / editables, so it is rendered recursively.
 */
function renderEditable(
  node: RawNode,
  call: MacroCall,
  inline: boolean,
  depth: number,
): ReactNode {
  const value = node.name
    ? lookupParameter(call.parameters, node.name)
    : call.content;
  return renderContent(
    editableValue(value, inline),
    call,
    inline,
    depth + 1,
    "",
  );
}

/** Normalizes an editable value into renderable nodes: a string becomes a paragraph (block) or is kept inline. */
function editableValue(value: unknown, inline: boolean): unknown {
  if (typeof value === "string") {
    return inline
      ? value
      : [{ type: "paragraph", props: {}, content: value, children: [] }];
  }
  return value;
}

/**
 * Renders a nested macro call by rendering its own server output against its own call. Only the outermost macro is
 * editable, so nested macros carry no edit affordance or double-click handler; a double-click inside bubbles up to the
 * outermost macro's wrapper. `call` / `output` are read as objects here (nested macro nodes are not JSON-stringified,
 * unlike the top-level macro block props).
 */
function renderNestedMacro(
  node: RawNode,
  inline: boolean,
  depth: number,
): ReactNode {
  const call = (node.props?.call ?? {}) as MacroCall;
  const output = (node.props?.output ?? []) as OutputNode[];
  const tag = inline ? "span" : "div";
  if (!output.length) {
    return createElement(
      tag,
      { "data-macro-name": call.name },
      `macro:${call.name}`,
    );
  }
  return createElement(
    tag,
    { "data-macro-name": call.name },
    <MacroOutput
      nodes={output}
      call={call}
      inline={inline}
      depth={depth + 1}
    />,
  );
}

/**
 * Renders an `xwikiRaw` node. HTML syntaxes are injected verbatim through a transparent (`display: contents`) wrapper
 * so the raw markup keeps its place in the surrounding flow (matching view mode); other syntaxes are rendered as
 * escaped text in a `pre`.
 */
function renderRaw(node: RawNode, inline: boolean): ReactNode {
  const syntax = (node.props?.syntax as string | undefined) ?? "";
  const text = (node.props?.text as string | undefined) ?? "";
  if (syntax.startsWith("html/")) {
    return createElement(inline ? "span" : "div", {
      style: { display: "contents" },
      dangerouslySetInnerHTML: { __html: text },
    });
  }
  return <pre className="xwiki-raw">{text}</pre>;
}

/** Renders a styled text inline node, applying text styles and any `xwikiParameters` (verbatim) as a span. */
function renderText(node: RawNode): ReactNode {
  const styles = node.styles ?? {};
  const style = textStyle(styles);
  const params = styles.xwikiParameters;
  const content = styles.code ? <code>{node.text}</code> : node.text;
  const hasStyle = Object.values(style).some((value) => value !== undefined);
  if (params != null) {
    return (
      <span ref={xwikiParamsRef(params)} style={hasStyle ? style : undefined}>
        {content}
      </span>
    );
  }
  if (hasStyle || styles.code) {
    return <span style={hasStyle ? style : undefined}>{content}</span>;
  }
  // Zero-wrapper: unstyled text is emitted as a bare string, so it stays a direct child of its parent (view mode).
  return node.text;
}

/** Maps BlockNote text styles to a React style object (undefined values are ignored by React and by {@link renderText}). */
function textStyle(styles: Record<string, unknown>): CSSProperties {
  const decoration = [
    styles.underline ? "underline" : "",
    styles.strikethrough ? "line-through" : "",
  ]
    .filter(Boolean)
    .join(" ");
  return {
    fontWeight: styles.bold ? "bold" : undefined,
    fontStyle: styles.italic ? "italic" : undefined,
    textDecoration: decoration || undefined,
    color: isColor(styles.textColor) ? (styles.textColor as string) : undefined,
    backgroundColor: isColor(styles.backgroundColor)
      ? (styles.backgroundColor as string)
      : undefined,
  };
}

/** Maps a block's default BlockNote props (alignment, colors) to a React style object, or undefined if none apply. */
function blockStyle(
  props: Record<string, unknown> | undefined,
): CSSProperties | undefined {
  const style: CSSProperties = {};
  const alignment = props?.textAlignment;
  if (typeof alignment === "string" && alignment !== "left") {
    style.textAlign = alignment as CSSProperties["textAlign"];
  }
  if (isColor(props?.textColor)) {
    style.color = props!.textColor as string;
  }
  if (isColor(props?.backgroundColor)) {
    style.backgroundColor = props!.backgroundColor as string;
  }
  return Object.keys(style).length ? style : undefined;
}

/** @returns whether the value is a non-default color string worth applying. */
function isColor(value: unknown): boolean {
  return typeof value === "string" && value !== "" && value !== "default";
}

/** Renders a BlockNote table (its `tableContent`) as an HTML table, honoring header rows/columns and cell spans. */
function renderTable(node: RawNode, call: MacroCall, depth: number): ReactNode {
  const table = node.content as
    | {
        columnWidths?: (number | null)[];
        headerRows?: number;
        headerCols?: number;
        rows?: { cells: unknown[] }[];
      }
    | undefined;
  const rows = table?.rows ?? [];
  const headerRows = table?.headerRows ?? 0;
  const headerCols = table?.headerCols ?? 0;
  const columnWidths = table?.columnWidths ?? [];
  return (
    <table>
      {columnWidths.some(Boolean) && (
        <colgroup>
          {columnWidths.map((width, i) => (
            <col key={i} style={width ? { width: `${width}px` } : undefined} />
          ))}
        </colgroup>
      )}
      <tbody>
        {rows.map((row, rowIndex) => (
          <tr key={rowIndex}>
            {row.cells.map((cell, colIndex) =>
              renderTableCell(
                cell,
                rowIndex < headerRows || colIndex < headerCols,
                call,
                depth,
                `${rowIndex}.${colIndex}`,
              ),
            )}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** Renders a single table cell (a string, an inline-content array, or a `tableCell` object with spans) as th/td. */
function renderTableCell(
  cell: unknown,
  header: boolean,
  call: MacroCall,
  depth: number,
  key: string,
): ReactNode {
  const tag = header ? "th" : "td";
  if (
    cell !== null &&
    typeof cell === "object" &&
    !Array.isArray(cell) &&
    (cell as RawNode).type === "tableCell"
  ) {
    const cellNode = cell as RawNode;
    return createElement(
      tag,
      {
        key,
        colSpan: cellNode.props?.colspan as number | undefined,
        rowSpan: cellNode.props?.rowspan as number | undefined,
      },
      renderContent(cellNode.content, call, true, depth, key),
    );
  }
  return createElement(
    tag,
    { key },
    renderContent(cell, call, true, depth, key),
  );
}

/**
 * @returns a React `ref` callback that copies the given `xwikiParameters` (server-provided `class`, `style` and any
 * other attributes) onto the element as HTML attributes, or undefined when there are none. Applying them imperatively
 * (as the DOM does) is what keeps arbitrary attribute names and `style` strings working, which React props cannot
 * express directly.
 */
function xwikiParamsRef(
  params: unknown,
): ((element: HTMLElement | null) => void) | undefined {
  if (params == null) {
    return undefined;
  }
  return (element: HTMLElement | null) => {
    if (element) {
      applyXWikiParameters(element, params);
    }
  };
}

/**
 * Looks up a macro-call parameter value by name, case-insensitively. The `xwikiEditable` marker carries the macro
 * descriptor's (canonical) parameter name, whereas the macro call stores parameters under the case the user actually
 * wrote, so an exact-case lookup would miss (this mirrors the server's case-insensitive matching).
 */
function lookupParameter(
  parameters: MacroCall["parameters"],
  name: string,
): unknown {
  if (name in parameters) {
    return parameters[name];
  }
  const lowerName = name.toLowerCase();
  const key = Object.keys(parameters).find(
    (k) => k.toLowerCase() === lowerName,
  );
  return key === undefined ? undefined : parameters[key];
}

export { MacroOutput };
export type { OutputNode };
