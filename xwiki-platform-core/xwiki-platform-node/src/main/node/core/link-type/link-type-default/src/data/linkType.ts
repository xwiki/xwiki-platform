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
 * @since 18.7.0RC1
 * @beta
 */
type LinkPageConfig = {
  ref: DocumentReference | null;
  queryString?: string;
  anchor?: string;
};

/**
 * Configuration for an attachment link
 *
 * @since 18.7.0RC1
 * @beta
 */
type LinkAttachmentConfig = {
  ref: AttachmentReference | null;
  queryString?: string;
};

/**
 * Configuration for an URL link
 *
 * @since 18.7.0RC1
 * @beta
 */
type LinkUrlConfig = {
  url: string;
};

/**
 * Configuration for an e-mail link
 *
 * @since 18.7.0RC1
 * @beta
 */
type LinkEmailConfig = {
  address: string;
  messageSubject?: string;
  messageBody?: string;
};

export type {
  LinkAttachmentConfig,
  LinkEmailConfig,
  LinkPageConfig,
  LinkUrlConfig,
};
