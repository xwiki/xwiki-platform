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
import { EmailLinkTargetType } from "../EmailLinkTargetType";
import { describe, expect, it } from "vitest";

describe("EmailLinkTargetType", () => {
  const extension = new EmailLinkTargetType();

  it("has the expected identity", () => {
    expect(extension.type).toBe("email");
    expect(extension.getLabel("en")).toBe("E-mail");
  });

  it("parses a mailto: URL, including its subject and body", () => {
    const config = extension.tryParseUrl(
      "mailto:jdoe@example.org?subject=Hello&body=Hi%20there",
    );

    expect(config).toEqual({
      address: "jdoe@example.org",
      messageSubject: "Hello",
      messageBody: "Hi there",
    });
  });

  it("parses a mailto: URL with no subject/body as undefined, not empty string", () => {
    const config = extension.tryParseUrl("mailto:jdoe@example.org");

    expect(config).toEqual({
      address: "jdoe@example.org",
      messageSubject: undefined,
      messageBody: undefined,
    });
  });

  it("does not match a non-mailto: URL", () => {
    expect(extension.tryParseUrl("https://example.org")).toBeNull();
  });

  it("round-trips address, subject and body when serializing", () => {
    const url = extension.serializeUrl({
      address: "jdoe@example.org",
      messageSubject: "Hello",
      messageBody: "Hi there",
    });

    expect(url).toBe("mailto:jdoe@example.org?subject=Hello&body=Hi+there");
  });

  it("omits subject/body from the URL when unset", () => {
    const url = extension.serializeUrl({ address: "jdoe@example.org" });

    expect(url).toBe("mailto:jdoe@example.org");
  });

  it("parses a mailto reference", () => {
    expect(
      extension.tryParseReference?.({
        type: "mailto",
        typed: true,
        reference: "jdoe@example.org",
        parameters: {},
      }),
    ).toEqual({ address: "jdoe@example.org" });
  });

  it("does not match a non-mailto reference", () => {
    expect(
      extension.tryParseReference?.({
        type: "url",
        typed: false,
        reference: "https://example.org",
        parameters: {},
      }),
    ).toBeNull();
  });

  it("converts an e-mail config back into a typed mailto reference", () => {
    expect(
      extension.configToReference?.({ address: "jdoe@example.org" }),
    ).toEqual({
      type: "mailto",
      typed: true,
      reference: "jdoe@example.org",
      parameters: {},
    });
  });

  it("returns undefined when converting to a reference with no address set", () => {
    expect(extension.configToReference?.({ address: "" })).toBeUndefined();
  });
});
