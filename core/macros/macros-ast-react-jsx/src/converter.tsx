/**
 * See the LICENSE file distributed with this work for additional
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
import { assertUnreachable, tryFallibleOrError } from "@xwiki/cristal-fn-utils";
import React from "react";
import type {
  MacroBlock,
  MacroBlockStyles,
  MacroInlineContent,
  MacroLinkTarget,
} from "@xwiki/cristal-macros-api";
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/cristal-model-remote-url-api";
import type {
  CSSProperties,
  HTMLAttributes,
  JSX,
  Ref,
  TdHTMLAttributes,
} from "react";

/**
 * @since 0.23
 * @beta
 */
export type MacroEditableZoneRef =
  | { type: "block"; ref: Ref<HTMLDivElement> }
  | { type: "inline"; ref: Ref<HTMLSpanElement> };

/**
 * Converter that transforms a macro's returned AST to React JSX
 *
 * @since 0.23
 * @beta
 */
export class MacrosAstToReactJsxConverter {
  constructor(
    private readonly remoteURLParser: RemoteURLParser,
    private readonly remoteURLSerializer: RemoteURLSerializer,
  ) {}

  /**
   * Render a macro's AST blocks to JSX elements
   *
   * Will force re-render every time, even if the AST is exactly the same
   * (see the private `generateId` function for more informations)
   *
   * @param blocks - The blocks to render
   * @param editableZoneRef - The macro's editable zone reference
   *
   * @returns The JSX elements
   */
  blocksToReactJSX(
    blocks: MacroBlock[],
    editableZoneRef: MacroEditableZoneRef,
  ): JSX.Element[] | Error {
    return tryFallibleOrError(() =>
      this.convertBlocks(blocks, editableZoneRef, this.generateId()),
    );
  }

  /**
   * Render a macro's AST inline contents to JSX elements
   *
   * Will force re-render every time, even if the AST is exactly the same
   * (see the private `generateId` function for more informations)
   *
   * @param inlineContents - The inline contents to render
   * @param editableZoneRef - The macro's editable zone reference
   *
   * @returns The JSX elements
   */
  inlineContentsToReactJSX(
    inlineContents: MacroInlineContent[],
    editableZoneRef: MacroEditableZoneRef,
  ): JSX.Element[] | Error {
    return tryFallibleOrError(() =>
      this.convertInlineContents(
        inlineContents,
        editableZoneRef,
        this.generateId(),
      ),
    );
  }

  private generateId(): string {
    // So, this is a tough one
    //
    // When mapping an array to React elements, each child needs a `key` identifying it based on its content.
    // This allows React to quickly check whether the child changed or not, and update / reorder the DOM accordingly.
    //
    // Because we are iterating over generic structures without identifiers (`MacroBlock` and `MacroInlineContent` don't have an ID),
    // and adding an ID would pollute the object without any real benefit (it wouldn't be linked to the actual content), we have several options:
    //
    // # Option 1. Using array indexes as keys
    //
    // This is not a good idea as keys need to reflect the child's content. If the macro re-renders a new AST and inverses two children's positions,
    // the keys would not reflect it and it could lead to bugs.
    //
    // # Option 2. Hashing each child's object and using it as a key
    //
    // This is not viable as it would be pretty costly. Performing a hash on an AST that may contain hundred of objects, all synchronously, would
    // possibly block the main thread for too long, especially on weaker devices like entry-level smartphones or older computers.
    //
    // # Option 3. Hashing the root AST object and suffixing the result with children's indexes
    //
    // This would be a viable option *if* we didn't also have the editable zone React reference to account for. This may change between renders,
    // and we have no means to derive a key from it, as it's a class instance without any identifiable property.
    //
    // # Option 4. Generating a random ID and suffixing the result with children's indexes
    //
    // This is the solution we use here and it's a big compromise. We are *not* considering the root AST object, simply generating a completely random
    // value that we use throughout the converter. This means even providing the exact same root AST object will force a complete re-render of the entire
    // React tree. This is desirable because again, the editable zone reference may change between two renders, and we have no mean to check that.
    //
    // So we use this trick which, while pretty costly for similar trees, ensures we have no bug in the render, and is an acceptable trade-off given that
    // macros are not supposed to change their rendered output very often.

    return Math.random().toString().substring(2);
  }

  private convertBlocks(
    blocks: MacroBlock[],
    editableZoneRef: MacroEditableZoneRef,
    key: string,
  ): JSX.Element[] {
    return blocks.map((block, i) => {
      const childKey = `${key}.${i}`;

      return (
        <React.Fragment key={childKey}>
          {this.convertBlock(block, editableZoneRef, childKey)}
        </React.Fragment>
      );
    });
  }

  private convertInlineContents(
    inlineContents: MacroInlineContent[],
    editableZoneRef: MacroEditableZoneRef,
    key: string,
  ): JSX.Element[] {
    return inlineContents.map((inlineContent, i) => {
      const childKey = `${key}.${i}`;

      return (
        <React.Fragment key={childKey}>
          {this.convertInlineContent(inlineContent, editableZoneRef, key)}
        </React.Fragment>
      );
    });
  }

  private convertBlock(
    block: MacroBlock,
    editableZoneRef: MacroEditableZoneRef,
    key: string,
  ): JSX.Element {
    switch (block.type) {
      case "paragraph":
        return (
          <p {...this.convertBlockStyles(block.styles)}>
            {this.convertInlineContents(block.content, editableZoneRef, key)}
          </p>
        );

      case "heading":
        const TagName = `h${block.level}`;

        return (
          <TagName {...this.convertBlockStyles(block.styles)}>
            {this.convertInlineContents(block.content, editableZoneRef, key)}
          </TagName>
        );

      case "list":
        const ListTag = block.numbered ? "ol" : "ul";

        return (
          <ListTag {...this.convertBlockStyles(block.styles)}>
            {block.items.map((item, i) => {
              const childKey = `${key}.${i}`;

              return (
                <li key={childKey}>
                  {item.checked !== undefined && (
                    <input type="checkbox" checked={item.checked} readOnly />
                  )}
                  {this.convertInlineContents(
                    item.content,
                    editableZoneRef,
                    childKey,
                  )}
                </li>
              );
            })}
          </ListTag>
        );

      case "quote":
        return (
          <blockquote {...this.convertBlockStyles(block.styles)}>
            {this.convertBlocks(block.content, editableZoneRef, key)}
          </blockquote>
        );

      case "code":
        // TODO: syntax highlighting?
        return <code>{block.content}</code>;

      case "table":
        return (
          <table {...this.convertBlockStyles(block.styles)}>
            <colgroup>
              {block.columns.map((col, i) => {
                const attrs: HTMLAttributes<unknown> = {};

                if (col.widthPx) {
                  attrs.style = { width: `${col.widthPx}px` };
                }

                const childKey = `${key}.${i}`;

                return <col key={childKey} {...attrs} />;
              })}
            </colgroup>

            {block.columns.find((col) => col.headerCell) && (
              <thead>
                <tr>
                  {block.columns.map((col, i) => {
                    const childKey = `${key}.${i}`;

                    return (
                      <th
                        key={childKey}
                        {...(col.headerCell
                          ? this.convertBlockStyles(col.headerCell.styles)
                          : {})}
                      >
                        {col.headerCell &&
                          this.convertInlineContents(
                            col.headerCell.content,
                            editableZoneRef,
                            key,
                          )}
                      </th>
                    );
                  })}
                </tr>
              </thead>
            )}

            <tbody>
              {block.rows.map((row, i) => {
                const rowId = `${key}.${i}`;

                return (
                  <tr key={rowId}>
                    {row.map((cell, i) => {
                      const attrs: TdHTMLAttributes<HTMLTableCellElement> =
                        this.convertBlockStyles(cell.styles);

                      if (cell.colSpan) {
                        attrs.colSpan = cell.colSpan;
                      }

                      if (cell.rowSpan) {
                        attrs.rowSpan = cell.rowSpan;
                      }

                      const childKey = `${rowId}.${i}`;

                      return (
                        <td key={childKey} {...attrs}>
                          {this.convertInlineContents(
                            cell.content,
                            editableZoneRef,
                            childKey,
                          )}
                        </td>
                      );
                    })}
                  </tr>
                );
              })}
            </tbody>
          </table>
        );

      case "image":
        return (
          <img
            src={this.getTargetUrl(block.target)}
            alt={block.alt}
            width={block.widthPx}
            height={block.heightPx}
          />
        );

      case "rawHtml":
        return <div dangerouslySetInnerHTML={{ __html: block.html }} />;

      case "macroBlock":
        throw new Error("Nested macros are not supported yet");

      case "macroBlockEditableArea":
        if (editableZoneRef.type === "inline") {
          throw new Error(
            'Provided editable zone React ref is of type "inline", but macro requests type "block"',
          );
        }

        return <div ref={editableZoneRef.ref} />;

      default:
        assertUnreachable(block);
    }
  }

  private convertBlockStyles(
    styles: MacroBlockStyles,
  ): HTMLAttributes<unknown> {
    const out: HTMLAttributes<unknown> = {};

    if (styles.backgroundColor) {
      (out["style"] ??= {})["backgroundColor"] = styles.backgroundColor;
    }

    if (styles.textColor) {
      (out["style"] ??= {})["color"] = styles.textColor;
    }

    if (styles.textAlignment) {
      (out["style"] ??= {})["textAlign"] = styles.textAlignment;
    }

    if (styles.cssClasses) {
      out["className"] = styles.cssClasses.join(" ");
    }

    return out;
  }

  private convertInlineContent(
    inlineContent: MacroInlineContent,
    editableZoneRef: MacroEditableZoneRef,
    key: string,
  ): JSX.Element {
    switch (inlineContent.type) {
      case "text":
        const style: CSSProperties = {};

        if (inlineContent.styles.backgroundColor) {
          style.backgroundColor = inlineContent.styles.backgroundColor;
        }

        if (inlineContent.styles.textColor) {
          style.color = inlineContent.styles.textColor;
        }

        if (inlineContent.styles.bold) {
          style.fontWeight = "bold";
        }

        if (inlineContent.styles.italic) {
          style.fontStyle = "italic";
        }

        if (inlineContent.styles.underline) {
          style.textDecoration = "underline";
        }

        if (inlineContent.styles.strikethrough) {
          style.textDecoration = "line-through";
        }

        const attr = Object.values(style).length > 0 ? { style } : {};

        return <span {...attr}>{inlineContent.content}</span>;

      case "link":
        return (
          <a href={this.getTargetUrl(inlineContent.target)}>
            {this.convertInlineContents(
              inlineContent.content,
              editableZoneRef,
              key,
            )}
          </a>
        );

      case "rawHtml":
        return (
          <span dangerouslySetInnerHTML={{ __html: inlineContent.html }} />
        );

      case "inlineMacro":
        throw new Error("Nested macros are not supported yet");

      case "inlineMacroEditableArea":
        if (editableZoneRef.type === "block") {
          throw new Error(
            'Provided editable zone React ref is of type "block", but macro requests type "inline"',
          );
        }

        return <span ref={editableZoneRef.ref} />;

      default:
        assertUnreachable(inlineContent);
    }
  }

  private getTargetUrl(target: MacroLinkTarget): string {
    if (target.type === "external") {
      return target.url;
    }

    const { rawReference } = target;

    const parsedRef = tryFallibleOrError(() =>
      this.remoteURLParser.parse(rawReference),
    );

    if (parsedRef instanceof Error) {
      throw new Error(
        `Failed to parse reference "${rawReference}": ${parsedRef.message}`,
      );
    }

    const url = this.remoteURLSerializer.serialize(parsedRef);

    // TODO: when could this even happen?
    if (!url) {
      throw new Error(`Failed to serialize reference "${rawReference}"`);
    }

    return url;
  }
}
