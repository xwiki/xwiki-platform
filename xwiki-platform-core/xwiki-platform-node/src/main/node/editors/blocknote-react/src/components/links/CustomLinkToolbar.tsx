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
import type { LinkEditionHandler } from "./linkEdition";
import type { LinkToolbarProps } from "@blocknote/react";

export type CustomLinkToolbarProps = {
  linkToolbarProps: LinkToolbarProps;
  linkEditionFn: LinkEditionHandler;
};

export const CustomLinkToolbar: React.FC<CustomLinkToolbarProps> = ({
  linkToolbarProps,
  linkEditionFn,
}) => {
  const Components = useComponentsContext()!;
  const { t } = useTranslation();

  const { editLink, deleteLink } = useExtension(LinkToolbarExtension);

  return (
    <>
      <Components.FormattingToolbar.Button
        className="bn-button"
        label={t("blocknote.linkToolbar.buttons.edit")}
        icon={<RiPencilLine />}
        onClick={() =>
          linkEditionFn({
            current: {
              url: linkToolbarProps.url,
              title: linkToolbarProps.text,
            },
            mode: "editExisting",
            onSubmit({ url, title }) {
              editLink(url, title);
            },
          })
        }
      />

      <Components.FormattingToolbar.Button
        className="bn-button"
        label={t("blocknote.linkToolbar.buttons.open")}
        icon={<RiExternalLinkLine />}
        onClick={() => window.open(linkToolbarProps.url)}
      />

      <Components.FormattingToolbar.Button
        className="bn-button"
        label={t("blocknote.linkToolbar.buttons.delete")}
        icon={<RiDeleteBin6Line />}
        onClick={() => deleteLink(linkToolbarProps.range.from)}
      />
    </>
  );
};
