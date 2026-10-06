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
 * Who created a document version.
 *
 * @since 18.8.0RC1
 * @beta
 */
type DocumentVersionAuthor = {
  /**
   * The absolute reference of the user document, when it is known.
   */
  reference?: string;

  /**
   * The display name of the author.
   */
  name?: string;
};

/**
 * A document version, as reported to the code that displays the edit history.
 *
 * @since 18.8.0RC1
 * @beta
 */
type DocumentVersion = {
  /**
   * The version number, e.g. `1.1`.
   */
  number: string;

  /**
   * When the version was created, as a timestamp.
   */
  date: number;

  /**
   * The author, either the identifier of the client that saved, which the caller is left to resolve, or the user
   * details when this target could look them up.
   */
  author?: string | DocumentVersionAuthor;
};

/**
 * The save context of {@link XWikiFormSaveTarget}.
 *
 * @since 18.8.0RC1
 * @beta
 */
type XWikiFormSaveContext = {
  /**
   * The save button the user clicked, left unset by an auto-save, which uses the save and continue button.
   */
  button?: HTMLInputElement;
};

export type { DocumentVersion, DocumentVersionAuthor, XWikiFormSaveContext };
