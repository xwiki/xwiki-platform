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
import type {
  AttachmentReference,
  DocumentReference,
} from "@xwiki/platform-model-api";

/**
 * Configuration for a page link
 *
 * @since 18.10.0RC1
 * @beta
 */
type PageLinkTargetConfig = {
  ref: DocumentReference | null;
  queryString?: string;
  anchor?: string;
};

/**
 * Configuration for an attachment link
 *
 * @since 18.10.0RC1
 * @beta
 */
type AttachmentLinkTargetConfig = {
  ref: AttachmentReference | null;
  queryString?: string;
};

/**
 * Configuration for a link to a resource designated by a plain text location: a URL, a path or a UNC path.
 *
 * @since 18.10.0RC1
 * @beta
 */
type PlainTextLinkTargetConfig = {
  location: string;
};

/**
 * Configuration for an e-mail link
 *
 * @since 18.10.0RC1
 * @beta
 */
type EmailLinkTargetConfig = {
  address: string;
  messageSubject?: string;
  messageBody?: string;
};

export type {
  AttachmentLinkTargetConfig,
  EmailLinkTargetConfig,
  PageLinkTargetConfig,
  PlainTextLinkTargetConfig,
};
