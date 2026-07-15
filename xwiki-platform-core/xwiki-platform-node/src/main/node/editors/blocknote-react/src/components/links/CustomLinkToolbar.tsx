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
import { LinkEditor } from "./LinkEditor";
import { LinkToolbarExtension } from "@blocknote/core/extensions";
import { useComponentsContext, useExtension } from "@blocknote/react";
import { useCallback, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  RiDeleteBin6Line,
  RiExternalLinkLine,
  RiPencilLine,
} from "react-icons/ri";
import type { LinkData } from "./LinkEditor";
import type { LinkEditionHooks } from "./linkEditionHooks";
import type { LinkToolbarProps } from "@blocknote/react";

export type CustomLinkToolbarProps = {
  linkToolbarProps: LinkToolbarProps;
  linkEditionHooks?: LinkEditionHooks;
};

export const CustomLinkToolbar: React.FC<CustomLinkToolbarProps> = ({
  linkToolbarProps,
  linkEditionHooks,
}) => {
  const Components = useComponentsContext()!;
  const { t } = useTranslation();

  const { editLink, deleteLink } = useExtension(LinkToolbarExtension);

  const [showLinkEditor, setShowLinkEditor] = useState(false);
  // The link data shown in the editor popover. It is set from the current link, giving the integration
  // a chance to transform it (e.g. to resolve an XWiki resource reference) right before the popover
  // opens, through the beforeEdit hook.
  const [editData, setEditData] = useState<LinkData>();

  const openEditor = useCallback(() => {
    const current: LinkData = {
      url: linkToolbarProps.url,
      title: linkToolbarProps.text,
    };
    setEditData(linkEditionHooks?.beforeEdit?.(current) ?? current);
    setShowLinkEditor(true);
  }, [linkToolbarProps.url, linkToolbarProps.text, linkEditionHooks]);

  const updateLink = useCallback(
    (linkData: LinkData) => {
      // Let the integration intercept the link (including its resource reference) before it is written
      // into the content. Throwing from the hook cancels the edition.
      const updatedLinkData =
        linkEditionHooks?.beforeUpdate?.(linkData) ?? linkData;
      editLink(
        updatedLinkData.url,
        updatedLinkData.title,
        linkToolbarProps.range.from,
      );
    },
    [editLink, linkEditionHooks, linkToolbarProps.range.from],
  );

  return (
    <>
      <Components.Generic.Popover.Root open={showLinkEditor}>
        <Components.Generic.Popover.Trigger>
          {/* TODO: hide tooltip on click
              (note: this comment is from BlockNote's source code but may remain relevant here) */}
          <Components.FormattingToolbar.Button
            className="bn-button"
            data-test="editLink"
            label={t("blocknote.linkToolbar.buttons.edit")}
            icon={<RiPencilLine />}
            onClick={openEditor}
          />
        </Components.Generic.Popover.Trigger>
        <Components.Generic.Popover.Content
          className="bn-popover-content bn-form-popover"
          variant="form-popover"
        >
          <LinkEditor
            current={
              editData ?? {
                url: linkToolbarProps.url,
                title: linkToolbarProps.text,
              }
            }
            updateLink={updateLink}
          />
        </Components.Generic.Popover.Content>
      </Components.Generic.Popover.Root>

      <Components.FormattingToolbar.Button
        className="bn-button"
        data-test="openLink"
        label={t("blocknote.linkToolbar.buttons.open")}
        icon={<RiExternalLinkLine />}
        onClick={() => window.open(linkToolbarProps.url)}
      />

      <Components.FormattingToolbar.Button
        className="bn-button"
        data-test="deleteLink"
        label={t("blocknote.linkToolbar.buttons.delete")}
        icon={<RiDeleteBin6Line />}
        onClick={() => deleteLink(linkToolbarProps.range.from)}
      />
    </>
  );
};
