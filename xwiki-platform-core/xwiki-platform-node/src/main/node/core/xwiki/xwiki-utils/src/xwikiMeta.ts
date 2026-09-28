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

import type { EntityReference } from "@xwiki/platform-xwiki-model-api";

/**
 * The metadata of the document displayed by the current page, exposed by the `xwiki-meta` RequireJS module.
 *
 * @since 18.8.0RC1
 * @beta
 */
type XWikiMeta = {
  documentReference: EntityReference;
  version: string;
  restURL: string;
  form_token: string;
  userReference?: EntityReference | null;
  isNew: boolean;

  /**
   * The raw locale of the document displayed by the current page. It is empty for the original translation, whose
   * actual locale is given by {@link XWikiMeta.realLocale}.
   */
  locale: string;

  /**
   * The actual locale of the document displayed by the current page. It differs from {@link XWikiMeta.locale} only
   * for the original translation, whose raw locale is empty. Note that it is the empty string for a technical
   * document, whose locale is the root locale.
   */
  realLocale: string;

  action: string;

  setVersion: (version: string) => void;

  /**
   * Refresh the version of a document from a REST endpoint. It fires a xwiki:document:changeVersion event. In case of
   * 404 this certainly means that the document is new.
   *
   * @param handle404 - function to choose how to handle when the document is new
   */
  refreshVersion: (handle404: (response: Response) => void) => Promise<void>;
};

export type { XWikiMeta };
