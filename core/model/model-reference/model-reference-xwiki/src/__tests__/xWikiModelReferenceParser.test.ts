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

import "reflect-metadata";
import { XWikiModelReferenceParser } from "../xWikiModelReferenceParser";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { describe, expect, test } from "vitest";
import { mock } from "vitest-mock-extended";
import { ref } from "vue";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { Ref } from "vue";

describe("xWikiModelReferenceParser", () => {
  const documentService = mock<DocumentService>({
    getCurrentDocumentReference(): Ref<DocumentReference | undefined> {
      return ref(
        new DocumentReference(
          "WebHome",
          new SpaceReference(undefined, "TestSpace"),
        ),
      );
    },
  });

  const parser = new XWikiModelReferenceParser(documentService);
  const currentSpace = new SpaceReference(undefined, "TestSpace");
  test.each([
    ["WebHome", undefined, new DocumentReference("WebHome", currentSpace)],
    [
      "attach:TestSpace.png",
      undefined,
      new AttachmentReference(
        "TestSpace.png",
        new DocumentReference("WebHome", currentSpace),
      ),
    ],
    [
      "TestSpace.png",
      EntityType.ATTACHMENT,
      new AttachmentReference(
        "TestSpace.png",
        new DocumentReference(
          "WebHome",
          new SpaceReference(undefined, "TestSpace"),
        ),
      ),
    ],
    [
      "TestSpace.TestSubSpace.WebHome@TestSubSpace.png",
      EntityType.ATTACHMENT,
      new AttachmentReference(
        "TestSubSpace.png",
        new DocumentReference(
          "WebHome",
          new SpaceReference(undefined, "TestSpace", "TestSubSpace"),
        ),
      ),
    ],
    [
      "attach:TestSpace.TestSubSpace.WebHome@TestSubSpace.png",
      undefined,
      new AttachmentReference(
        "TestSubSpace.png",
        new DocumentReference(
          "WebHome",
          new SpaceReference(undefined, "TestSpace", "TestSubSpace"),
        ),
      ),
    ],
    [
      "TestSpace.TestSubSpace.WebHome",
      undefined,
      new DocumentReference(
        "WebHome",
        new SpaceReference(undefined, "TestSpace", "TestSubSpace"),
      ),
    ],
  ])("%s -> %s", (reference, type, expected) => {
    expect(parser.parse(reference, type)).toEqual(expected);
  });
});
