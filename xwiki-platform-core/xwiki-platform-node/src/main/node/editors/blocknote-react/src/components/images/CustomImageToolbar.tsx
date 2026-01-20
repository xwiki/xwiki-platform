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
import { ImageFilePanel } from "./ImageFilePanel";
import { useBlockNoteEditor, useComponentsContext } from "@blocknote/react";
import { assertUnreachable } from "@xwiki/platform-fn-utils";
import { useCallback, useState } from "react";
import { useTranslation } from "react-i18next";
import { RiExternalLinkLine, RiPencilLine } from "react-icons/ri";
import type { BlockOfType } from "../../blocknote";
import type { LinkEditionContext } from "../../misc/linkSuggest";

type CustomImageToolbarProps = {
  currentBlock: BlockOfType<"image">;
  linkEditionCtx: LinkEditionContext;
  imageEditionOverrideFn?: ImageEditionOverrideFn;
};

/**
 * Interceptor for the image edition mechanism
 *
 * @since 0.26
 * @beta
 */
type ImageEditionOverrideFn = (
  image: BlockOfType<"image">["props"],
  update: (updateResult: ImageUpdateResult) => void,
) => void;

/**
 * Result of an image update process, from `ImageEditionOverrideFn`
 *
 * @since 18.0.0RC1
 * @beta
 */
type ImageUpdateResult =
  | { type: "update"; updatedProps: Partial<BlockOfType<"image">["props"]> }
  | { type: "aborted" };

export const CustomImageToolbar: React.FC<CustomImageToolbarProps> = ({
  currentBlock,
  linkEditionCtx,
  imageEditionOverrideFn,
}) => {
  const Components = useComponentsContext()!;
  const { t } = useTranslation();
  const editor = useBlockNoteEditor();

  const [showImageEditor, setShowImageEditor] = useState(false);

  const updateImageProps = useCallback(
    (updateResult: ImageUpdateResult) => {
      switch (updateResult.type) {
        case "update":
          editor.updateBlock(currentBlock, {
            props: updateResult.updatedProps,
          });
          break;

        case "aborted":
          break;

        default:
          assertUnreachable(updateResult);
      }
    },
    [currentBlock, editor],
  );

  const openEditor = useCallback(() => {
    if (imageEditionOverrideFn) {
      imageEditionOverrideFn(currentBlock.props, updateImageProps);
    } else {
      setShowImageEditor(true);
    }
  }, [imageEditionOverrideFn, currentBlock, editor]);

  return (
    <>
      <Components.Generic.Popover.Root opened={showImageEditor}>
        <Components.Generic.Popover.Trigger>
          {/* TODO: hide tooltip on click
              (note: this comment is from BlockNote's source code but may remain relevant here) */}
          <Components.FormattingToolbar.Button
            className="bn-button"
            label={t("blocknote.imageToolbar.buttons.edit")}
            icon={<RiPencilLine />}
            onClick={openEditor}
          />
        </Components.Generic.Popover.Trigger>
        <Components.Generic.Popover.Content
          className="bn-popover-content bn-form-popover"
          variant="form-popover"
        >
          <ImageFilePanel
            linkEditionCtx={linkEditionCtx}
            currentBlock={currentBlock}
          />
        </Components.Generic.Popover.Content>
      </Components.Generic.Popover.Root>

      <Components.FormattingToolbar.Button
        className="bn-button"
        label={t("blocknote.imageToolbar.buttons.open")}
        icon={<RiExternalLinkLine />}
        onClick={() => window.open(currentBlock.props.url)}
      />
    </>
  );
};

export type {
  CustomImageToolbarProps,
  ImageEditionOverrideFn,
  ImageUpdateResult,
};
