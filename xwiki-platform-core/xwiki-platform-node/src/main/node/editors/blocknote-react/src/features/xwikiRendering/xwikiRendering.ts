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
import { DefinitionListItemBlock } from "./definitionListItemBlockSpec";
import { XWikiGroupBlock } from "./groupBlockSpec";
import { XWikiRawBlock } from "./rawBlockSpec";
import { VerbatimStyle, XWikiParametersStyle } from "./xwikiStyleSpecs";
import type { EditorFeature, FeatureContribution } from "../editorFeature";

/**
 * The block types and text styles that the XWiki rendering produces when converting content to BlockNote's format
 * (definition list items, groups, raw content, verbatim text and the XWiki parameters kept on the text), so that the
 * editor can load such content. None of them can be inserted from the editor UI. The syntax configuration has no
 * capability for them, so the feature always contributes them; the integrations whose content comes from the XWiki
 * rendering bind it (it isn't bound by the `ComponentInit` of this package).
 *
 * @since 18.10.0RC1
 * @beta
 */
const xwikiRenderingFeature: EditorFeature = {
  id: "xwikiRendering",
  contribute(): FeatureContribution {
    return {
      schema: {
        blockSpecs: {
          xwikiDefinitionListItem: DefinitionListItemBlock(),
          xwikiGroup: XWikiGroupBlock(),
          xwikiRaw: XWikiRawBlock(),
        },
        styleSpecs: {
          xwikiVerbatim: VerbatimStyle,
          xwikiParameters: XWikiParametersStyle,
        },
      },
    };
  },
};

export { xwikiRenderingFeature };
