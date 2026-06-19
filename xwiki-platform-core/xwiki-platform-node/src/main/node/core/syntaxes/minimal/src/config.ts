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
import { injectable } from "inversify";
import type {
  SyntaxAllowedFeatures,
  SyntaxConfig,
} from "@xwiki/platform-syntaxes-config";

/**
 * Name of the minimal syntax
 *
 * @since 18.6.0RC1
 * @beta
 */
const MINIMAL_SYNTAX_NAME = "_minimal";

/**
 * Configuration for the `xwiki/2.1` syntax
 *
 * @since 18.6.0RC1
 * @beta
 */
@injectable()
class MinimalSyntaxConfig implements SyntaxConfig {
  /** NOTE: must *always* match `MINIMAL_SYNTAX_NAME` */
  id = "_minimal";

  features: SyntaxAllowedFeatures = {
    blocks: {
      headings: {
        levels1To3: false,
        levels4To6: false,
      },
      images: {
        basicImages: false,
        altText: false,
        caption: false,
        customBorder: false,
        customDimensions: false,
        insideLinks: false,
      },
      lists: {
        bulletLists: false,
        blockInListItems: false,
        checkableLists: false,
        contiguousNumberedLists: false,
        contiguousNumberedListsAnyStartIndex: false,
        mixableCheckableListItems: false,
        multipleBlocksInListItems: false,
        unorderedNumberedLists: false,
        listsNesting: false,
      },
      quotes: false,
      code: {
        basicCodeBlocks: false,
        language: false,
      },
      dividers: false,
      macros: false,
      nesting: false,
      styling: {
        justifyAlignment: false,
        lcrAlignment: false,
      },
      tables: {
        basicTables: false,
        blockInTableCells: false,
        colRows: false,
        colSpan: false,
        headerColumns: false,
        multipleBlocksInTableCells: false,
        multipleFooterRows: false,
        multipleHeaderRows: false,
        noHeaderRowTable: false,
        singleFooterRow: false,
        singleHeaderRow: false,
      },
    },
    inlineContents: {
      images: false,
      links: {
        basicLinks: false,
        customText: false,
        customTextStyling: false,
        descriptiveTooltip: false,
        metadata: false,
      },
      code: {
        basicInlineCode: false,
        language: false,
      },
      macros: false,
      rawHtml: false,
      textStyles: {
        bold: false,
        italic: false,
        strikethrough: false,
        underline: false,
        nesting: false,
        fontFamily: false,
        fontSize: false,
        subscript: false,
        superscript: false,
      },
    },
  };
}

export { MINIMAL_SYNTAX_NAME, MinimalSyntaxConfig };
