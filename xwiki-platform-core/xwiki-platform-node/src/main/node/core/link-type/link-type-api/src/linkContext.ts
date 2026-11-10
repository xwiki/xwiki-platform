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
import type { DocumentReference } from "@xwiki/platform-model-api";

/**
 * Where a link is created or edited. The link target types take it into account when converting between a link target
 * and the reference of the linked resource (e.g. a page reference is resolved and saved relative to the document
 * holding the link), and the link target fields when displaying and parsing the reference typed by the user.
 *
 * @since 18.10.0RC1
 * @beta
 */
type LinkContext = {
  /**
   * The document holding the link, when it's known. The references of the linked resources (e.g. pages and
   * attachments) are relative to it, so that the links keep working when the document is renamed or copied.
   */
  sourceDocument?: DocumentReference;
};

export type { LinkContext };
