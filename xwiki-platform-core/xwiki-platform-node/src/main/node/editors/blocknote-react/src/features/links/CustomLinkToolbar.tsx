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
import { useLinkEditing } from "./linkEditing";
import { EditorInstanceContext } from "../../contexts";
import { stripLinkMetadata } from "../../misc/contentMetadata";
import { getEditedLink, updateLink } from "../../services/linkEditor";
import { LinkToolbarExtension } from "@blocknote/core/extensions";
import { useComponentsContext, useExtension } from "@blocknote/react";
import { useContext } from "react";
import { useTranslation } from "react-i18next";
import {
  RiDeleteBin6Line,
  RiExternalLinkLine,
  RiPencilLine,
} from "react-icons/ri";
import type { LinkEditionData } from "../../services/linkEditor";
import type { LinkToolbarProps } from "@blocknote/react";

export type CustomLinkToolbarProps = {
  linkToolbarProps: LinkToolbarProps;
};

export const CustomLinkToolbar: React.FC<CustomLinkToolbarProps> = ({
  linkToolbarProps,
}) => {
  const Components = useComponentsContext()!;
  const instance = useContext(EditorInstanceContext)!;
  const linkEditing = useLinkEditing();
  const { t } = useTranslation();

  const { deleteLink } = useExtension(LinkToolbarExtension);

  return (
    <>
      {/* Hidden when there's no link editor. */}
      {linkEditing && (
        <Components.FormattingToolbar.Button
          className="bn-button"
          data-test="editLink"
          label={t("blocknote.linkToolbar.buttons.edit")}
          icon={<RiPencilLine />}
          onClick={async () => {
            // The link data exactly as stored in the content. The link metadata handler may transform what the link
            // editor is pre-filled with (e.g. hide the metadata carried by the URL), but it needs the untransformed
            // data before the update to be able to recover what the link editor doesn't edit, so keep it aside.
            const previous: LinkEditionData = {
              url: linkToolbarProps.url,
              title: linkToolbarProps.text,
            };
            await linkEditing.edit(getEditedLink(instance, previous), (link) =>
              updateLink(instance, link, previous, linkToolbarProps.range.from),
            );
            instance.editor.focus();
          }}
        />
      )}

      <Components.FormattingToolbar.Button
        className="bn-button"
        data-test="openLink"
        label={t("blocknote.linkToolbar.buttons.open")}
        icon={<RiExternalLinkLine />}
        // The metadata carried by the URL isn't for the link target.
        onClick={() => window.open(stripLinkMetadata(linkToolbarProps.url))}
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
