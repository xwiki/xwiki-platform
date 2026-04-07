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
// @ts-expect-error this is a JavaScript file, it is expected to not have types.
import { loadById } from "./services/require.js";

async function flavorchoicestepCallback(): Promise<boolean> {
  const form = document.querySelector("#flavorchoicestep");
  if (form) {
    // @ts-expect-error expected type
    const params = new FormData(form);
    const restURL = `${XWiki.contextPath}/rest/distributionWizard/${encodeURIComponent(
      XWiki.currentWiki,
    )}/step/FlavorChoiceStep`;
    const response = await fetch(restURL, {
      method: "POST",
      headers: {
        "Content-Type": "multipart/form-data",
      },
      body: params,
    });
    return response.status >= 200 && response.status < 300;
  } else {
    return false;
  }
}

async function initFlavorListener(): Promise<void> {
  const jQuery = await loadById("jquery");

  jQuery(document).on(
    "xwiki:flavorpicker:updated",
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    function (event: any, data: any) {
      if (
        jQuery(data.elements).find("input[type='radio']:checked").length > 0
      ) {
        console.log("validating step triggering event");
        jQuery(document).trigger("xwiki:distributionWizard:validateStep");
      } else {
        jQuery(document).trigger("xwiki:distributionWizard:invalidateStep");
      }
    },
  );
}
export { flavorchoicestepCallback, initFlavorListener };
