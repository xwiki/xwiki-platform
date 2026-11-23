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
import { XWikiCodeBlock } from "./codeBlockSpec";
import { syntaxHighlighter } from "@blocknote/code-block";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
} from "../editorFeature";

/**
 * Replaces BlockNote's code block with one that doesn't fail on the languages that its language selector doesn't know,
 * saves the languages under the names known server side, and has the CSS classes set by
 * `features.codeBlocks.className`, and highlights the code blocks based on their language. Nothing is contributed when the syntax of the edited content
 * doesn't support code blocks.
 *
 * @since 18.10.0RC1
 * @beta
 */
const codeBlocksFeature: EditorFeature = {
  id: "codeBlocks",
  contribute({
    options,
    syntax,
  }: FeatureContext): FeatureContribution | undefined {
    if (!syntax.features.blocks.code.basicCodeBlocks) {
      return undefined;
    }
    // Not called when the code blocks are disabled.
    const className = (options.features?.codeBlocks || undefined)?.className;
    return {
      schema: { blockSpecs: { codeBlock: XWikiCodeBlock({ className }) } },
      blockNoteExtensions: [syntaxHighlighter],
    };
  },
};

export { codeBlocksFeature };
