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
 * The name under which the {@link UploadListener} of an editor instance is bound in its service container.
 */
const uploadListenerName = "XWikiUploadListener";

/**
 * Told about the uploads of an editor instance, by the {@link XWikiFileUploader}. Bound by the editor component in the
 * service container of the editor instance, since what it does is specific to that editor instance (e.g. the form
 * field submitting its temporary uploads).
 */
interface UploadListener {
  /**
   * Called for each file uploaded to the temporary store of the user session, from where the save request attaches it
   * to the edited document, as long as it asks for it.
   *
   * @param name - the name under which the save request has to ask for the uploaded file
   */
  onTemporaryUpload(name: string): void;

  /**
   * Called when a file can't be uploaded, to tell the user.
   *
   * @param name - the name of the file that couldn't be uploaded
   * @param error - why the upload failed
   */
  onUploadFailed(name: string, error: unknown): void;
}

/**
 * @param instance - an editor instance
 * @returns the upload listener of the given editor instance, if any
 */
function getUploadListener(
  instance: EditorInstance,
): UploadListener | undefined {
  return instance.serviceContainer.isBound(uploadListenerName)
    ? instance.serviceContainer.get<UploadListener>(uploadListenerName)
    : undefined;
}

export { getUploadListener, uploadListenerName };
export type { UploadListener };
