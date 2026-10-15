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

import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { loadById } from "@xwiki/platform-xwiki-utils";
import type { ToolbarEnvironment } from "../ui/types";

/**
 * The subset of the jQuery API used to notify the edit form listeners.
 */
type JQuery = (element: Element) => { trigger(event: string): void };

type Moment = {
  format(format: string): string;
  toMomentFormatString(javaDateFormat: string): string;
};

type MomentFactory = (date?: number) => Moment;

/**
 * The configuration of the realtime editing, see `XWiki.Realtime.Configuration`.
 */
type RealtimeConfig = {
  dateFormat?: string;
  editComment?: { enabled?: boolean; mandatory?: boolean };
  minorEdit?: boolean;
  defaultAvatarURL?: string;
  icons?: { check?: string; clock?: string };
};

/**
 * Loads what the realtime toolbar needs from the XWiki edit page (configuration, libraries, etc.) so that realtime
 * toolbars can then be created synchronously.
 *
 * @returns the environment of the realtime toolbar on the XWiki edit page
 * @since 18.9.0RC1
 * @beta
 */
async function createXWikiToolbarEnvironment(): Promise<ToolbarEnvironment> {
  const [$, moment, , config] = await loadById<
    [JQuery, MomentFactory, unknown, RealtimeConfig]
  >("jquery", "moment", "moment-jdateformatparser", "xwiki-realtime-config");
  const dateFormat = moment().toMomentFormatString(
    config.dateFormat || "yyyy/MM/dd HH:mm",
  );

  return {
    editComment: {
      enabled: config.editComment?.enabled ?? false,
      mandatory: config.editComment?.mandatory ?? false,
    },
    minorEdit: config.minorEdit ?? false,
    defaultAvatarURL: config.defaultAvatarURL ?? "",
    icons: {
      check: config.icons?.check ?? "",
      clock: config.icons?.clock ?? "",
    },
    formatDate: (date) => moment(date).format(dateFormat),
    getUserURL: (reference) =>
      new XWiki.Document(
        Model.resolve(reference, EntityType.DOCUMENT)!,
      ).getURL(),
    getVersionURL: (xwikiDocument, version) =>
      xwikiDocument.getURL(
        "view",
        new URLSearchParams({
          rev: version,
          language: xwikiDocument.realLocale ?? "",
        }),
      ),
    getVersionContentURL: (xwikiDocument, version) =>
      xwikiDocument.getURL(
        "view",
        new URLSearchParams({
          xpage: "displaycontent",
          htmlHeaderAndFooter: "true",
          outputTitle: "true",
          language: xwikiDocument.realLocale ?? "",
          rev: version,
        }),
      ),
    async fetchChanges(form, xwikiDocument, previousVersion, toolbar) {
      // Notify the others that the form is about to be submitted, so that they update the form fields. The event is
      // fired from inside the form because some listeners are registered on the form's action buttons.
      $(toolbar).trigger("xwiki:actions:beforePreview");
      const formData = new URLSearchParams(
        new FormData(form as HTMLFormElement) as unknown as string[][],
      );
      formData.set("rev1", previousVersion ?? "");
      formData.set("xpage", "changesinline");
      // We have to use the edit URL in order to compare the previous version with the unsaved version.
      const response = await fetch(xwikiDocument.getURL("edit"), {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "X-Requested-With": "XMLHttpRequest",
        },
        body: formData,
      });
      if (!response.ok) {
        throw new Error(`Response status: ${response.status}`);
      }
      return await response.text();
    },
    setChangeSummary(form, summary, minorEdit) {
      const commentInput = form.querySelector<HTMLInputElement>(
        'input[name="comment"]',
      );
      if (commentInput) {
        commentInput.value = summary;
      }
      const minorEditCheckbox = form.querySelector<HTMLInputElement>(
        'input[name="minorEdit"]',
      );
      if (minorEdit !== undefined && minorEditCheckbox) {
        minorEditCheckbox.checked = minorEdit;
      }
    },
  };
}

export { createXWikiToolbarEnvironment };
