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
import { LinkToolbarExtension } from "@blocknote/core/extensions";
import { useComponentsContext, useExtension } from "@blocknote/react";
import { useTranslation } from "react-i18next";
import {
  RiDeleteBin6Line,
  RiExternalLinkLine,
  RiPencilLine,
} from "react-icons/ri";
import type { LinkEditionData, LinkEditionHandler } from "./linkEdition";
import type { LinkEditionHooks } from "./linkEditionHooks";
import type { LinkToolbarProps } from "@blocknote/react";

export type CustomLinkToolbarProps = {
  linkToolbarProps: LinkToolbarProps;
  linkEditionHandler: LinkEditionHandler;
  linkEditionHooks?: LinkEditionHooks;
};

export const CustomLinkToolbar: React.FC<CustomLinkToolbarProps> = ({
  linkToolbarProps,
  linkEditionHandler,
  linkEditionHooks,
}) => {
  const Components = useComponentsContext()!;
  const { t } = useTranslation();

  const { editLink, deleteLink } = useExtension(LinkToolbarExtension);

  return (
    <>
      <Components.FormattingToolbar.Button
        className="bn-button"
        data-test="editLink"
        label={t("blocknote.linkToolbar.buttons.edit")}
        icon={<RiPencilLine />}
        onClick={() => {
          // The link data exactly as stored in the content. beforeEdit may transform what the link
          // editor is pre-filled with (e.g. hide the synthetic id carried by the URL), but
          // beforeUpdate needs the untransformed data to be able to recover that id, so keep it
          // aside.
          const previous: LinkEditionData = {
            url: linkToolbarProps.url,
            title: linkToolbarProps.text,
          };

          linkEditionHandler({
            current: linkEditionHooks?.beforeEdit?.(previous) ?? previous,
            mode: "editExisting",
            onSubmit(linkData) {
              const updatedLinkData =
                linkEditionHooks?.beforeUpdate?.(linkData, previous) ??
                linkData;
              editLink(
                updatedLinkData.url,
                updatedLinkData.title,
                linkToolbarProps.range.from,
              );
            },
          });
        }}
      />

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
