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
import { BlockNoteForTest } from "./BlockNote.story";
import { FULL_SYNTAX } from "./syntax.mock";
import { fileUploaderName } from "../../files/fileUploader";
import { useState } from "react";
import type { BlockType } from "../../blocknote";
import type { FileUploader } from "../../files/fileUploader";

/**
 * Renders the editor with a file uploader defined natively (i.e. running in the browser), since a function passed from
 * the test runs outside of the browser. The uploader stores the files under their name, and the names of the files it
 * gets are rendered, so that the tests can check them.
 */
export const BlockNoteWithFileUploader: React.FC<{
  content: BlockType[];
  // The upload option of the editor.
  upload?: boolean;
  // When set, the uploads fail.
  fail?: boolean;
}> = ({ content, upload, fail }) => {
  const [uploadedFiles, setUploadedFiles] = useState<string[]>([]);

  return (
    <>
      <BlockNoteForTest
        content={content}
        options={{ syntax: FULL_SYNTAX, upload }}
        bind={(container) =>
          container.bind<FileUploader>(fileUploaderName).toConstantValue({
            async upload(_instance, file) {
              setUploadedFiles((names) => [...names, file.name]);
              if (fail) {
                throw new Error("Failed to upload the file");
              }
              return { url: `/download/${file.name}`, name: file.name };
            },
          })
        }
      />
      <output data-test="uploadedFiles">{JSON.stringify(uploadedFiles)}</output>
    </>
  );
};
