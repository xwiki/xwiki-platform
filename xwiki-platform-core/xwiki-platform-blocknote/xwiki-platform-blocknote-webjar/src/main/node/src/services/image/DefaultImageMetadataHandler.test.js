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
import { DefaultImageMetadataHandler } from "./DefaultImageMetadataHandler";
import { createBlockMetadataStore } from "@xwiki/platform-editors-blocknote-react";
import { toCristalEntityReference } from "@xwiki/platform-model-xwiki";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { beforeEach, describe, expect, it } from "vitest";

// eslint-disable-next-line max-statements
describe("DefaultImageMetadataHandler", () => {
  const handler = new DefaultImageMetadataHandler();
  const block = {
    id: "image",
    type: "image",
    props: { url: "catURL", previewWidth: 100 },
  };
  let instance;

  beforeEach(() => {
    instance = {
      options: {
        sourceDocumentReference: toCristalEntityReference(
          Model.resolve("wiki:Space.Page", EntityType.DOCUMENT),
        ),
      },
      metadata: createBlockMetadataStore(),
    };
  });

  it("edits the image with its stored reference", () => {
    const reference = { type: "attach", typed: false, reference: "cat.png" };
    instance.metadata.set("image", "xwikiReference", reference);

    expect(handler.beforeEdit(instance, block)).toEqual({
      url: "catURL",
      previewWidth: 100,
      reference,
    });
  });

  it("edits an image known only by its URL without reference", () => {
    expect(handler.beforeEdit(instance, block).reference).toBeUndefined();
  });

  it("stores the reference of an attached image relative to the source document, and untyped", () => {
    handler.afterUpdate(instance, block, {
      type: "attach",
      typed: true,
      reference: "wiki:Space.Page@cat.png",
    });

    expect(instance.metadata.get("image").xwikiReference).toEqual({
      type: "attach",
      typed: false,
      reference: "cat.png",
    });
  });

  it("keeps the reference of an image attached to another document absolute", () => {
    handler.afterUpdate(instance, block, {
      type: "attach",
      typed: true,
      reference: "wiki:Other.Page@cat.png",
    });

    expect(instance.metadata.get("image").xwikiReference).toEqual({
      type: "attach",
      typed: false,
      reference: "Other.Page@cat.png",
    });
  });

  it("stores the reference of an attached image as is without source document, but untyped", () => {
    instance.options.sourceDocumentReference = undefined;

    handler.afterUpdate(instance, block, {
      type: "attach",
      typed: true,
      reference: "wiki:Space.Page@cat.png",
    });

    expect(instance.metadata.get("image").xwikiReference).toEqual({
      type: "attach",
      typed: false,
      reference: "wiki:Space.Page@cat.png",
    });
  });

  it("stores the other references as they are", () => {
    const reference = { type: "icon", typed: true, reference: "home" };

    handler.afterUpdate(instance, block, reference);

    expect(instance.metadata.get("image").xwikiReference).toBe(reference);
  });

  it("drops the reference of the previous image when the new one is known only by its URL", () => {
    instance.metadata.set("image", "xwikiReference", {
      type: "attach",
      reference: "cat.png",
    });

    handler.afterUpdate(instance, block);

    expect(instance.metadata.get("image")).toBeUndefined();
  });
});
