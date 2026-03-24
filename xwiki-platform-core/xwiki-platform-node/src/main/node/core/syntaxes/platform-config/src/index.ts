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
 * Configuration for a specific syntax
 *
 * @since 18.2.0RC1
 * @beta
 */
type SyntaxConfig = {
  /**
   * ID of the syntax (must be unique across all syntaxes)
   */
  id: string;

  /**
   * List of supported features
   */
  features: SyntaxFeaturesWhitelist;
};

/**
 * List of features supported by a syntax
 *
 * All features disabled or absent from the configuration will be considered as unsupported
 *
 * @since 18.2.0RC1
 * @beta
 */
type SyntaxFeaturesWhitelist = {
  /**
   * Support for various blocks
   */
  blocks: {
    /**
     * Support for heading
     */
    headings: {
      /**
       * Support for heading levels 1 to 3
       */
      levels1To3: boolean;

      /**
       * Support for heading levels 4 to 6
       */
      levels4To6: boolean;
    };

    /**
     * Support for lists
     */
    lists: {
      /**
       * Support for unordered lists
       */
      bulletLists: boolean;

      /**
       * Support for contiguous numbered lists (starting from index 1)
       */
      contiguousNumberedLists: boolean;

      /**
       * Support for contiguous numbered lists (with any start index)
       */
      contiguousNumberedListsAnyStartIndex: boolean;

      /**
       * Support for unordered numbered lists
       */
      unorderedNumberedLists: boolean;

      /**
       * Support for checkable lists
       */
      checkableLists: boolean;

      /**
       * Support for mixing checkable items in other list types
       */
      mixableCheckableListItems: boolean;

      /**
       * Supports nesting lists inside list items
       */
      listsNesting: boolean;

      /**
       * Support for using blocks inside list items (e.g. tables)
       */
      blockInListItems: boolean;

      /**
       * Support for using multiple blocks inside list items (e.g. a table followed by another list)
       */
      multipleBlocksInListItems: boolean;
    };

    /**
     * Support for tables
     */
    tables: {
      /**
       * Support for basic tables (unstyled cells with inline contents only)
       */
      basicTables: boolean;

      /**
       * Support for a single header row
       */
      singleHeaderRow: boolean;

      /**
       * Support for multiple header rows
       */
      multipleHeaderRows: boolean;

      /**
       * Support for a single footer row
       */
      singleFooterRow: boolean;

      /**
       * Support for multiple footer rows
       */
      multipleFooterRows: boolean;

      /**
       * Support for header columns
       */
      headerColumns: boolean;

      /**
       * Support for using blocks inside table cells (e.g. lists)
       */
      blockInTableCells: boolean;

      /**
       * Support fo rusing multiple blocks inside list items (e.g. two consecutive lists)
       */
      multipleBlocksInTableCells: boolean;

      /**
       * Support for extending a single cell across multiple column
       */
      colSpan: boolean;

      /**
       * Support for extending a single cell across multiple rows
       */
      colRows: boolean;

      /**
       * Support for tables WITHOUT a header row
       */
      noHeaderRowTable: boolean;
    };

    /**
     * Support for images
     */
    images: {
      /**
       * Support for basic images (only URL)
       */
      basicImages: boolean;

      /**
       * Support for custom width and height
       */
      customDimensions: boolean;

      /**
       * Support for caption
       */
      caption: boolean;

      /**
       * Support for alternative text (if the image fails to render)
       */
      altText: boolean;

      /**
       * Support for embedding images inside links
       */
      insideLinks: boolean;

      /**
       * Support for showing an optional border with custom width and color
       */
      customBorder: boolean;
    };

    /**
     * Support for quote blocks
     */
    quotes: boolean;

    /**
     * Support for code blocks
     */
    code: {
      /**
       * Support for basic code blocks (no property)
       */
      basicCodeBlocks: boolean;

      /**
       * Support for specifying the syntax language
       */
      language: boolean;
    };

    /**
     * Support for dividers
     */
    dividers: boolean;

    /**
     * Support for macros
     */
    macros: boolean;

    /**
     * Support for nesting blocks
     */
    nesting: boolean;

    /**
     * Support for blocks styling
     */
    styling: {
      /** Support for 'left', 'center' and 'right' alignments */
      lcrAlignment: boolean;

      /**
       * Support for 'justify' alignment
       */
      justifyAlignment: boolean;
    };
  };

  /**
   * Support for inline contents
   */
  inlineContents: {
    /**
     * Support for links
     */
    links: {
      /**
       * Support for basic links (URL only)
       */
      basicLinks: boolean;

      /**
       * Support for displaying custom text instead of the URL
       */
      customText: boolean;

      /**
       * Support fo rstyling the custom text
       */
      customTextStyling: boolean;

      /**
       * Support for storing metadata inside the link (e.g. document reference)
       */
      metadata: boolean;

      /**
       * Support for showing a custom tooltip on hover (e.g. HTML's `title` attribute)
       */
      descriptiveTooltip: boolean;
    };

    /**
     * Support for inline images, with the same feature set as described in the 'blocks' section
     */
    images: boolean;

    /**
     * Support for inline code
     */
    code: {
      /**
       * Support for basic inline codes (no property)
       */
      basicInlineCode: boolean;

      /**
       * Support for specifying the syntax language
       */
      language: boolean;
    };

    /**
     * Support for inline macros
     */
    macros: boolean;

    /**
     * Support for raw HTML
     */
    rawHtml: boolean;

    /**
     * Support for various text styles
     */
    textStyles: {
      bold: boolean;
      italic: boolean;
      underline: boolean;
      strikethrough: boolean;
      superscript: boolean;
      subscript: boolean;

      fontSize: boolean;
      fontFamily: boolean;

      /**
       * Support for nesting text styles (e.g. italic segment inside bold text)
       */
      nesting: boolean;
    };
  };
};

export type { SyntaxConfig, SyntaxFeaturesWhitelist };
