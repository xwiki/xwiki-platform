/*
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

import { EntityReference } from "@xwiki/cristal-model-api";

/**
 * @since 0.16
 */
type UniAst = { blocks: Block[] };

/**
 * @since 0.16
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
      type: "bulletListItem";
      content: InlineContent[];
      subItems: ListItem[];
      styles: BlockStyles;
    }
  | {
      type: "numberedListItem";
      number: number;
      content: InlineContent[];
      subItems: ListItem[];
      styles: BlockStyles;
    }
  | {
      type: "checkedListItem";
      checked: boolean;
      content: InlineContent[];
      subItems: ListItem[];
      styles: BlockStyles;
    }
  | { type: "blockQuote"; content: Block[]; styles: BlockStyles }
  | { type: "codeBlock"; language?: string; content: string }
  | {
      type: "table";
      columns: TableColumn[];
      rows: TableCell[][];
      styles: BlockStyles;
    }
  | {
      type: "image";
      caption?: string;
      widthPx?: number;
      heightPx?: number;
      target: LinkTarget;
      styles: { alignment?: Alignment };
    }
  | {
      type: "macro";
      name: string;
      props: Record<string, boolean | number | string>;
    };

/**
 * @since 0.16
 */
type ListItem = Extract<
  Block,
  { type: "bulletListItem" | "numberedListItem" | "checkedListItem" }
>;

/**
 * @since 0.16
 */
type BlockStyles = {
  textColor?: string;
  backgroundColor?: string;
  textAlignment?: Alignment;
};

/**
 * @since 0.16
 */
type Alignment = "left" | "center" | "right" | "justify";

/**
 * @since 0.16
 */
type TableColumn = { headerCell?: TableCell; widthPx?: number };

/**
 * @since 0.16
 */
type TableCell = {
  content: InlineContent[];
  styles: BlockStyles;
  rowSpan?: number;
  colSpan?: number;
};

/**
 * @since 0.16
 */
type InlineContent =
  | { type: "text"; props: Text }
  | { type: "link"; target: LinkTarget; content: Text[] };

/**
 * @since 0.16
 */
type Text = {
  content: string;
  styles: TextStyles;
};

/**
 * @since 0.16
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
 */
type LinkTarget =
  | { type: "internal"; reference: EntityReference }
  | { type: "external"; url: string };

export type {
  Alignment,
  Block,
  BlockStyles,
  InlineContent,
  LinkTarget,
  ListItem,
  TableCell,
  TableColumn,
  Text,
  TextStyles,
  UniAst,
};
