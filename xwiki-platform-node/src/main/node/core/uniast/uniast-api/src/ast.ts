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
 * @since 0.16
 * @beta
 */
type UniAst = { blocks: Block[] };

/**
 * @since 0.16
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
       * @since 0.20
       * @beta
       */
      type: "macroBlock";
      name: string;
      params: Record<string, boolean | number | string>;
    };

/**
 * @since 0.16
 * @beta
 */
type BlockStyles = {
  textColor?: string;
  backgroundColor?: string;
  textAlignment?: Alignment;
};

/**
 * @since 0.16
 * @beta
 */
type Alignment = "left" | "center" | "right" | "justify";

/**
 * @since 0.17
 * @beta
 */
type ListItem = {
  number?: number;
  checked?: boolean;
  content: Block[];
  styles: BlockStyles;
};

/**
 * @since 0.17
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
 * @since 0.22
 * @beta
 */
type Link = {
  target: LinkTarget;
  /**
   * @since 0.23
   * @beta
   */
  content: Exclude<InlineContent, { type: "link" }>[];
};

/**
 * @since 0.16
 * @beta
 */
type TableColumn = { headerCell?: TableCell; widthPx?: number };

/**
 * @since 0.16
 * @beta
 */
type TableCell = {
  content: InlineContent[];
  styles: BlockStyles;
  rowSpan?: number;
  colSpan?: number;
};

/**
 * @since 0.16
 * @beta
 */
type InlineContent =
  | ({ type: "text" } & Text)
  | ({ type: "image" } & Image)
  | ({ type: "link" } & Link)
  | {
      /**
       * @since 0.20
       * @beta
       */
      type: "inlineMacro";
      name: string;
      params: Record<string, boolean | number | string>;
    };

/**
 * @since 0.16
 * @beta
 */
type Text = {
  content: string;
  styles: TextStyles;
};

/**
 * @since 0.16
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
 * @since 0.16
 * @beta
 */
type LinkTarget =
  | {
      type: "internal";

      /**
       * @since 0.20
       * @beta
       */

      rawReference: string;

      /**
       * Will be `null` if the raw reference is invalid and can't be parsed
       *
       * @since 0.20
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
  TableCell,
  TableColumn,
  Text,
  TextStyles,
  UniAst,
};
