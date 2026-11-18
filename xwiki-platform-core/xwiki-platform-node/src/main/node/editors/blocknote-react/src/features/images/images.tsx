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
import { CustomImageToolbar } from "./CustomImageToolbar";
import { FilePanel } from "./FilePanel";
import { ImageSuggestionController } from "./ImageSuggestionController";
import { XWikiImageBlock } from "./imageBlockSpec";
import { openImageSuggestionMenu } from "./imageSuggestionMenu";
import {
  FilePanelController,
  getDefaultReactSlashMenuItems,
} from "@blocknote/react";
import type { BlockOfType, BlockType, EditorType } from "../../blocknote";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
  SlashMenuItem,
} from "../editorFeature";

/**
 * @param editor - the editor where the image is inserted
 * @returns the item replacing BlockNote's "Image" item of the slash menu, which suggests images to insert (in addition
 *   to uploading or browsing images) instead of inserting an empty image block
 */
function imageSlashMenuItem(editor: EditorType): SlashMenuItem[] {
  // The default items keep the key of their BlockNote dictionary entry, even if their type doesn't declare it.
  return (getDefaultReactSlashMenuItems(editor) as SlashMenuItem[])
    .filter((item) => item.key === "image")
    .map((item) => ({
      ...item,
      onItemClick: () => openImageSuggestionMenu(editor),
    }));
}

/**
 * Inserts and edits images: the "Image" item of the slash menu and the image suggestion menu it opens, the toolbar of
 * the image blocks, and the panel to change the image of an image block. It also replaces BlockNote's image block with
 * one showing the file name of the images that can't be displayed. The images are chosen with the `ImageEditor`
 * when one is bound in the service container, and with the image selector of the editor otherwise. Nothing is
 * contributed when the syntax of the edited content doesn't support images.
 *
 * @since 18.10.0RC1
 * @beta
 */
const imagesFeature: EditorFeature = {
  id: "images",
  contribute({
    syntax,
    getInstance,
  }: FeatureContext): FeatureContribution | undefined {
    if (!syntax.features.blocks.images.basicImages) {
      return undefined;
    }
    return {
      schema: { blockSpecs: { image: XWikiImageBlock() } },
      slashMenuItems: () => imageSlashMenuItem(getInstance().editor),
      formattingToolbar: {
        blockToolbars: {
          image: (block: BlockType) => (
            <CustomImageToolbar currentBlock={block as BlockOfType<"image">} />
          ),
        },
      },
      components: (
        <>
          <ImageSuggestionController maxSuggestions={5} />
          <FilePanelController
            filePanel={({ blockId }) => <FilePanel blockId={blockId} />}
          />
        </>
      ),
    };
  },
};

export { imagesFeature };
