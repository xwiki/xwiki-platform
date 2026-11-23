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
import type { EditorInstance } from "@xwiki/platform-editors-blocknote-react";

/**
 * The name under which the {@link TemporaryUploads} of an editor instance are bound in its service container.
 */
const temporaryUploadsName = "XWikiTemporaryUploads";

/**
 * The files uploaded by the {@link XWikiFileUploader} to the temporary store of the user session, from where the save
 * request attaches them to the edited document, as long as it asks for them. Bound by the editor component in the
 * service container of the editor instance, since it's the form field of that editor instance that submits them.
 */
interface TemporaryUploads {
  /**
   * Records a file uploaded to the temporary store, otherwise it's never attached to the edited document.
   *
   * @param name - the name under which the save request has to ask for the uploaded file
   */
  add(name: string): void;
}

/**
 * @param instance - an editor instance
 * @returns the temporary uploads of the given editor instance, if any
 */
function getTemporaryUploads(
  instance: EditorInstance,
): TemporaryUploads | undefined {
  return instance.serviceContainer.isBound(temporaryUploadsName)
    ? instance.serviceContainer.get<TemporaryUploads>(temporaryUploadsName)
    : undefined;
}

export { getTemporaryUploads, temporaryUploadsName };
export type { TemporaryUploads };
