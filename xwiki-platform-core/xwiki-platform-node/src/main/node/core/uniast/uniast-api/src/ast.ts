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

import type { EntityReference } from "@xwiki/platform-model-api";

/**
 * @since 18.0.0RC1
 * @beta
 */
type UniAst = { blocks: Block[] };

/**
 * @since 18.0.0RC1
 * @beta
 */
type Block =
  | { type: "paragraph"; styles: BlockStyles; content: InlineContent[] }
  | {
      type: "heading";
      level: 1 | 2 | 3 | 4 | 5 | 6;
      content: InlineContent[];
      styles: BlockStyles;
    }
  | {
      type: "list";
      items: ListItem[];
      styles: BlockStyles;
    }
  | { type: "quote"; content: Block[]; styles: BlockStyles }
  | { type: "code"; language?: string; content: string }
  | {
      type: "table";
      columns: TableColumn[];
      rows: TableCell[][];
      styles: BlockStyles;
    }
  | ({
      type: "image";
    } & Image)
  | { type: "break" }
  | {
      /**
       * @since 18.0.0RC1
       * @beta
       */
      type: "macroBlock";

      /**
       * @since 18.0.0RC1
       * @beta
       */
      call: MacroInvocation;
    };

/**
 * @since 18.0.0RC1
 * @beta
 */
type BlockStyles = {
  textColor?: string;
  backgroundColor?: string;
  textAlignment?: Alignment;
};

/**
 * @since 18.0.0RC1
 * @beta
 */
type Alignment = "left" | "center" | "right" | "justify";

/**
 * @since 18.0.0RC1
 * @beta
 */
type ListItem = {
  number?: number;
  checked?: boolean;
  content: Block[];
  styles: BlockStyles;
};

/**
 * @since 18.0.0RC1
 * @beta
 */
type Image = {
  target: LinkTarget;
  caption?: string;
  alt?: string;
  widthPx?: number;
  heightPx?: number;
  styles: { alignment?: Alignment };
};

/**
 * @since 18.0.0RC1
 * @beta
 */
type Link = {
  target: LinkTarget;
  /**
   * @since 18.0.0RC1
   * @beta
   */
  content: Exclude<InlineContent, { type: "link" }>[];
};

/**
 * @since 18.0.0RC1
 * @beta
 */
type TableColumn = { headerCell?: TableCell; widthPx?: number };

/**
 * @since 18.0.0RC1
 * @beta
 */
type TableCell = {
  content: InlineContent[];
  styles: BlockStyles;
  rowSpan?: number;
  colSpan?: number;
};

/**
 * @since 18.0.0RC1
 * @beta
 */
type MacroInvocation = {
  id: string;
  params: Record<string, boolean | number | string>;
  body:
    | { type: "none" }
    | { type: "raw"; content: string }
    // NOTE: This one is for blocks, it should be changed to { type: "blocks", blocks: Block[] } when BlockNote supports nesting
    // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
    | { type: "inlineContents"; inlineContents: InlineContent[] }
    // NOTE: This one is for inline contents, it should be changed to { type: "inlineContents", inlineContents: InlineContent[] } when BlockNote supports nesting
    // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
    | { type: "inlineContent"; inlineContent: InlineContent };
};

/**
 * @since 18.0.0RC1
 * @beta
 */
type InlineContent =
  | ({ type: "text" } & Text)
  | ({ type: "image" } & Image)
  | ({ type: "link" } & Link)
  /**
   * @since 18.0.0RC1
   * @beta
   */
  | {
      type: "inlineMacro";
      call: MacroInvocation;
    }
  /**
   * @since 18.0.0RC1
   * @beta
   */
  | {
      type: "subscript";
      content: string;
      styles: TextStyles;
    }
  /**
   * @since 18.0.0RC1
   * @beta
   */
  | {
      type: "superscript";
      content: string;
      styles: TextStyles;
    };

/**
 * @since 18.0.0RC1
 * @beta
 */
type Text = {
  content: string;
  styles: TextStyles;
};

/**
 * @since 18.0.0RC1
 * @beta
 */
type TextStyles = {
  bold?: boolean;
  italic?: boolean;
  strikethrough?: boolean;
  underline?: boolean;
  code?: boolean;
  textColor?: string;
  backgroundColor?: string;
};

/**
 * @since 18.0.0RC1
 * @beta
 */
type LinkTarget =
  | {
      type: "internal";

      /**
       * @since 18.0.0RC1
       * @beta
       */

      rawReference: string;

      /**
       * Will be `null` if the raw reference is invalid and can't be parsed
       *
       * @since 18.0.0RC1
       * @beta
       */
      parsedReference: EntityReference | null;
    }
  | { type: "external"; url: string };

export type {
  Alignment,
  Block,
  BlockStyles,
  Image,
  InlineContent,
  Link,
  LinkTarget,
  ListItem,
  MacroInvocation,
  TableCell,
  TableColumn,
  Text,
  TextStyles,
  UniAst,
};
