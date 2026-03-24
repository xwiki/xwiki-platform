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

import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";

/**
 * Configuration for the `markdown/1.2` syntax
 *
 * @since 18.2.0RC1
 * @beta
 */
export const markdownSyntaxConfig: SyntaxConfig = {
  id: "markdown/1.2",
  features: {
    blocks: {
      headings: {
        levels1To3: true,
        levels4To6: true,
      },
      images: {
        basicImages: true,
        altText: true,
        caption: true,
        customBorder: false,
        customDimensions: false,
        insideLinks: true,
      },
      lists: {
        bulletLists: true,
        blockInListItems: true,
        checkableLists: true,
        contiguousNumberedLists: true,
        contiguousNumberedListsAnyStartIndex: true,
        mixableCheckableListItems: false,
        multipleBlocksInListItems: true,
        unorderedNumberedLists: false,
        listsNesting: true,
      },
      quotes: true,
      code: {
        basicCodeBlocks: true,
        language: true,
      },
      dividers: true,
      macros: true,
      nesting: true,
      styling: {
        justifyAlignment: false,
        lcrAlignment: false,
      },
      tables: {
        basicTables: true,
        blockInTableCells: false,
        colRows: false,
        colSpan: false,
        headerColumns: false,
        multipleBlocksInTableCells: false,
        multipleFooterRows: false,
        multipleHeaderRows: false,
        noHeaderRowTable: false,
        singleFooterRow: false,
        singleHeaderRow: true,
      },
    },
    inlineContents: {
      images: true,
      links: {
        basicLinks: true,
        customText: true,
        customTextStyling: true,
        descriptiveTooltip: true,
        metadata: false,
      },
      code: {
        basicInlineCode: true,
        language: false,
      },
      macros: true,
      rawHtml: true,
      textStyles: {
        bold: true,
        italic: true,
        strikethrough: true,
        underline: true,
        nesting: true,
        fontFamily: false,
        fontSize: false,
        subscript: true,
        superscript: true,
      },
    },
  },
};
