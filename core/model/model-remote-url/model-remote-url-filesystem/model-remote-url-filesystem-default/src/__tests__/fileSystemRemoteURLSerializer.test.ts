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
import { FileSystemRemoteURLSerializer } from "../filesystemRemoteURLSerializer";
import {
  AttachmentReference,
  DocumentReference,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { describe, expect, it } from "vitest";

describe("FileSystemRemoteURLSerializer", () => {
  const fileSystemRemoteURLSerializer = new FileSystemRemoteURLSerializer();
  it("serialize a simple document reference", () => {
    const serialize = fileSystemRemoteURLSerializer.serialize(
      new DocumentReference("c", new SpaceReference(undefined, "a", "b")),
    );
    expect(serialize).toEqual("cristalfs://a/b/c");
  });

  it("serialize a document reference with special chars", () => {
    const serialize = fileSystemRemoteURLSerializer.serialize(
      new DocumentReference("c/d", new SpaceReference(undefined, "a", "b")),
    );
    expect(serialize).toEqual("cristalfs://a/b/c%2Fd");
  });

  it("serialize an attachment reference ", () => {
    const serialize = fileSystemRemoteURLSerializer.serialize(
      new AttachmentReference(
        "file.pdf",
        new DocumentReference("c", new SpaceReference(undefined, "a", "b")),
      ),
    );
    expect(serialize).toEqual("cristalfs://a/b/c/attachments/file.pdf");
  });
});
