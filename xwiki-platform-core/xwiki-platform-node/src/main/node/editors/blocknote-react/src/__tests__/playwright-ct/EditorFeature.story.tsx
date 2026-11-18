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
import { BlockNoteForTest } from "./BlockNote.story";
import { FULL_SYNTAX } from "./syntax.mock";
import { editorFeatureName } from "../../features/editorFeature";
import type { BlockType } from "../../blocknote";
import type { EditorFeatures } from "../../editorOptions";
import type { EditorFeature } from "../../features/editorFeature";

/**
 * Renders the editor with an editing feature bound in the service container, contributing one of each kind of
 * contribution. Defined natively (i.e. running in the browser), since a function passed from the test runs outside of
 * the browser.
 */
export const BlockNoteWithFeature: React.FC<{
  content: BlockType[];
  // When set, the feature is disabled through its options.
  disabled?: boolean;
}> = ({ content, disabled }) => (
  <BlockNoteForTest
    content={content}
    options={{
      syntax: FULL_SYNTAX,
      features: (disabled ? { testFeature: false } : {}) as EditorFeatures,
    }}
    bind={(container) =>
      container.bind<EditorFeature>(editorFeatureName).toConstantValue({
        id: "testFeature",
        contribute: ({ getInstance }) => ({
          slashMenuItems: () => [
            {
              title: "Insert greeting",
              onItemClick: () =>
                getInstance().editor.insertInlineContent("Hello"),
            },
          ],
          formattingToolbar: {
            items: [
              <button key="testFeatureButton" data-test="testFeatureButton">
                Test
              </button>,
            ],
            blockToolbars: {
              quote: () => <div data-test="quoteToolbar">Quote toolbar</div>,
            },
          },
          components: (
            <output data-test="testFeatureComponent">Rendered</output>
          ),
        }),
      })
    }
  />
);
