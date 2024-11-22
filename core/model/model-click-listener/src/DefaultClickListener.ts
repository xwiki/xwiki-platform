/*
 * See the LICENSE file distributed with this work for additional
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

import { ClickListener } from "./clickListener";
import { AttachmentReference, EntityType } from "@xwiki/cristal-model-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type { AttachmentPreview } from "@xwiki/cristal-attachments-api";
import type { ModelReferenceSerializerProvider } from "@xwiki/cristal-model-reference-api";
import type { RemoteURLParserProvider } from "@xwiki/cristal-model-remote-url-api";

/**
 * @since 0.12
 */
@injectable()
class DefaultClickListener implements ClickListener {
  constructor(
    @inject("RemoteURLParserProvider")
    private readonly remoteURLParserProvider: RemoteURLParserProvider,
    @inject("ModelReferenceSerializerProvider")
    private readonly modelReferenceSerializerProvider: ModelReferenceSerializerProvider,
    @inject("CristalApp") private readonly cristal: CristalApp,
    @inject("AttachmentPreview")
    private readonly attachmentPreview: AttachmentPreview,
  ) {}

  handleHTMLElement(element: HTMLElement): void {
    const handleURL = this.handleURL.bind(this);
    element.addEventListener(
      "click",
      function handleClick(event: MouseEvent) {
        // If no parser is found, we let the click event go through.
        // TODO: generalize to be able to inject click handlers for various element kinds.
        if ((event.target as HTMLElement).tagName === "IMG") {
          const url =
            (event.target as HTMLImageElement)?.getAttribute("src") || "";
          event.preventDefault();
          handleURL(url);
        } else if ((event.target as HTMLElement).tagName === "A") {
          const link = event.target as HTMLLinkElement;
          const url = link.getAttribute("href") || "";
          const href = link.href;
          event.preventDefault();
          handleURL(url, href);
        }
      },
      true,
    );
  }

  handleURL(url: string, href?: string): void {
    const remoteURLParser = this.remoteURLParserProvider.get()!;
    const modelReferenceSerializer =
      this.modelReferenceSerializerProvider.get()!;
    try {
      const entityReference = remoteURLParser.parse(url);
      if (entityReference) {
        if (entityReference.type == EntityType.DOCUMENT) {
          this.cristal.setCurrentPage(
            modelReferenceSerializer.serialize(entityReference) || "",
          );
        } else if (entityReference.type == EntityType.ATTACHMENT) {
          this.attachmentPreview.preview(
            entityReference as AttachmentReference,
          );
        }
      } else {
        console.debug(`[${url}] is not a valid xwiki entity link`);
      }
    } catch (e) {
      console.log(
        `Failed to parse [${url}], falling back to legacy link handling.`,
        e,
      );
      // We keep the legacy support for now in case of bug in the link parser.
      if (href) {
        this.legacyHandleURL(href);
      }
    }
  }

  private legacyHandleURL(href: string) {
    // We cannot use `closest()` because of possible shadow roots.
    if (href) {
      // Case 1: the link is relative and/or points to the current host.
      if (href.startsWith(location.origin)) {
        const page = href.replace(location.origin + location.pathname, "");
        if (!page.startsWith("#")) {
          this.cristal.setCurrentPage(page, "view");
          this.cristal.loadPage().then();
        } else {
          window.location.href = page;
          return;
        }
      } else {
        // Case 2: the link points to an external server, in this case we try to resolve it to a known page.
        // Otherwise, the link is considered as external.
        this.cristal.loadPageFromURL(href).then();
      }
    }
  }
}

export { DefaultClickListener };
