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

/**
 * Block returned by a macro
 *
 * @since 0.23
 * @beta
 */
type MacroBlock =
  | {
      type: "paragraph";
      styles: MacroBlockStyles;
      content: MacroInlineContent[];
    }
  | {
      type: "heading";
      level: 1 | 2 | 3 | 4 | 5 | 6;
      content: MacroInlineContent[];
      styles: MacroBlockStyles;
    }
  | {
      type: "list";
      numbered?: boolean;
      items: MacroListItem[];
      styles: MacroBlockStyles;
    }
  | { type: "quote"; content: MacroBlock[]; styles: MacroBlockStyles }
  | { type: "code"; language?: string; content: string }
  | {
      type: "table";
      columns: MacroTableColumn[];
      rows: MacroTableCell[][];
      styles: MacroBlockStyles;
    }
  | ({
      type: "image";
    } & MacroImage)
  | {
      type: "macroBlock";
      name: string;
      params: Record<string, boolean | number | string>;
    }
  | { type: "rawHtml"; html: string }
  | {
      type: "macroBlockEditableArea";
      styles: MacroBlockStyles;
    };

/**
 * Styles for a `MacroBlock`
 *
 * @since 0.23
 * @beta
 */
type MacroBlockStyles = {
  cssClasses?: string[];
  // TODO: theme's CSS variables
  textColor?: string;
  // TODO: theme's CSS variables
  backgroundColor?: string;
  textAlignment?: MacroAlignment;
};

/**
 * Alignment for an item
 *
 * @since 0.23
 * @beta
 */
type MacroAlignment = "left" | "center" | "right" | "justify";

/**
 * Item that's part of a `MacroBlock` list
 *
 * @since 0.23
 * @beta
 */
type MacroListItem = {
  checked?: boolean;
  content: MacroInlineContent[];
  styles: MacroBlockStyles;
};

/**
 * Image returned by a macro
 *
 * @since 0.23
 * @beta
 */
type MacroImage = {
  target: MacroLinkTarget;
  alt?: string;
  widthPx?: number;
  heightPx?: number;
};

/**
 * Link returned by a macro
 *
 * @since 0.23
 * @beta
 */
type MacroLink = {
  target: MacroLinkTarget;
  content: Exclude<MacroInlineContent, { type: "link" }>[];
};

/**
 * Column that's part of a `MacroBlock` table
 *
 * @since 0.23
 * @beta
 */
type MacroTableColumn = {
  headerCell?: {
    content: MacroInlineContent[];
    styles: MacroBlockStyles;
  };
  widthPx?: number;
};

/**
 * Cell that's part of a `MacroBlock` table
 *
 * @since 0.23
 * @beta
 */
type MacroTableCell = {
  content: MacroInlineContent[];
  styles: MacroBlockStyles;
  rowSpan?: number;
  colSpan?: number;
};

/**
 * Inline content returned by a macro
 *
 * @since 0.23
 * @beta
 */
type MacroInlineContent =
  | ({ type: "text" } & MacroText)
  | ({ type: "link" } & MacroLink)
  | { type: "rawHtml"; html: string }
  | {
      type: "inlineMacro";
      name: string;
      params: Record<string, boolean | number | string>;
    }
  | { type: "inlineMacroEditableArea" };

/**
 * Text that's part of a `MacroInlineContent`
 *
 * @since 0.23
 * @beta
 */
type MacroText = {
  content: string;
  styles: MacroTextStyles;
};

/**
 * Styles for a `MacroText`
 *
 * @since 0.23
 * @beta
 */
type MacroTextStyles = {
  bold?: boolean;
  italic?: boolean;
  strikethrough?: boolean;
  underline?: boolean;
  code?: boolean;
  textColor?: string;
  backgroundColor?: string;
};

/**
 * Link target returned by a macro
 *
 * @since 0.23
 * @beta
 */
type MacroLinkTarget =
  | {
      type: "internal";
      rawReference: string;
    }
  | { type: "external"; url: string };

export type {
  MacroAlignment,
  MacroBlock,
  MacroBlockStyles,
  MacroImage,
  MacroInlineContent,
  MacroLink,
  MacroLinkTarget,
  MacroListItem,
  MacroTableCell,
  MacroTableColumn,
  MacroText,
  MacroTextStyles,
};
