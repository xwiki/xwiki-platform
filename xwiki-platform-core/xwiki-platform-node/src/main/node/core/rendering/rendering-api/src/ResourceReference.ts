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
/**
 * Client-side equivalent of `ResourceType` from `xwiki-rendering-api`.
 *
 * @since 18.6.0RC1
 * @beta
 */
const ResourceType = {
  SPACE: "space",

  DOCUMENT: "doc",
  PAGE: "page",

  ATTACHMENT: "attach",
  PAGE_ATTACHMENT: "pageAttach",

  INTERWIKI: "interwiki",

  URL: "url",
  MAILTO: "mailto",

  PATH: "path",
  UNC: "unc",

  ICON: "icon",
  DATA: "data",

  USER: "user",

  UNKNOWN: "unknown",
};

/**
 * Client-side equivalent of `ResourceReference` from `xwiki-rendering-api`.
 *
 * @since 18.6.0RC1
 * @beta
 */
type ResourceReference = {
  type: string;
  typed: boolean;
  reference: string;
  parameters: Record<string, string>;
  baseReferences?: string[];
};

export { type ResourceReference, ResourceType };
