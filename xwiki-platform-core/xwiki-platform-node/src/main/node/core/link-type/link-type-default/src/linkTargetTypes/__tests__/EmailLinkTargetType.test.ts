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
  const targetType = new EmailLinkTargetType();

  it("has the expected identity", async () => {
    expect(targetType.type).toBe("email");
    await expect(targetType.getLabel("en")).resolves.toBe("E-mail");
  });

  it("parses a mailto: URL, including its subject and body", () => {
    const config = targetType.fromURL(
      "mailto:jdoe@example.org?subject=Hello&body=Hi%20there",
    );

    expect(config).toEqual({
      address: "jdoe@example.org",
      messageSubject: "Hello",
      messageBody: "Hi there",
    });
  });

  it("parses a mailto: URL with no subject/body as undefined, not empty string", () => {
    const config = targetType.fromURL("mailto:jdoe@example.org");

    expect(config).toEqual({
      address: "jdoe@example.org",
      messageSubject: undefined,
      messageBody: undefined,
    });
  });

  it("does not match a non-mailto: URL", () => {
    expect(targetType.fromURL("https://example.org")).toBeNull();
  });

  it("round-trips address, subject and body when serializing", () => {
    const url = targetType.toURL({
      address: "jdoe@example.org",
      messageSubject: "Hello",
      messageBody: "Hi there",
    });

    expect(url).toBe("mailto:jdoe@example.org?subject=Hello&body=Hi%20there");
  });

  it("encodes the subject and body as RFC 6068 requires, e.g. spaces as %20 and not +", () => {
    const url = targetType.toURL({
      address: "jdoe@example.org",
      messageSubject: "1 + 1 = 2?",
      messageBody: "a&b#c",
    });

    expect(url).toBe(
      "mailto:jdoe@example.org?subject=1%20%2B%201%20%3D%202%3F&body=a%26b%23c",
    );
  });

  it("omits subject/body from the URL when unset", () => {
    const url = targetType.toURL({ address: "jdoe@example.org" });

    expect(url).toBe("mailto:jdoe@example.org");
  });

  it("labels the link with the e-mail address, without the message subject and body", () => {
    expect(
      targetType.getDefaultLabel({
        address: " jdoe@example.org ",
        messageSubject: "Hello",
        messageBody: "Hi there",
      }),
    ).toBe("jdoe@example.org");
  });
});

describe("EmailLinkTargetType reference conversion", () => {
  const targetType = new EmailLinkTargetType();

  it("parses a mailto reference", () => {
    expect(
      targetType.fromReference?.({
        type: "mailto",
        typed: true,
        reference: "jdoe@example.org",
        parameters: {},
      }),
    ).toEqual({ address: "jdoe@example.org" });
  });

  it("parses the subject and body held by the query string of a mailto reference, as the WYSIWYG editor saves them", () => {
    expect(
      targetType.fromReference?.({
        type: "mailto",
        typed: true,
        reference:
          "jdoe@example.org?subject=1%20%2B%201%20%3D%202%3F&body=Hi+there",
        parameters: {},
      }),
    ).toEqual({
      address: "jdoe@example.org",
      messageSubject: "1 + 1 = 2?",
      messageBody: "Hi there",
    });
  });

  it("does not match a non-mailto reference", () => {
    expect(
      targetType.fromReference?.({
        type: "url",
        typed: false,
        reference: "https://example.org",
        parameters: {},
      }),
    ).toBeNull();
  });

  it("converts an e-mail config back into a typed mailto reference", () => {
    expect(targetType.toReference?.({ address: "jdoe@example.org" })).toEqual({
      type: "mailto",
      typed: true,
      reference: "jdoe@example.org",
      parameters: {},
    });
  });

  it("keeps the subject and body in the query string of the mailto reference", () => {
    expect(
      targetType.toReference?.({
        address: "jdoe@example.org",
        messageSubject: "Hello",
        messageBody: "Hi there",
      }),
    ).toEqual({
      type: "mailto",
      typed: true,
      reference: "jdoe@example.org?subject=Hello&body=Hi%20there",
      parameters: {},
    });
  });

  it("returns undefined when converting to a reference with no address set", () => {
    expect(targetType.toReference?.({ address: "" })).toBeUndefined();
  });
});
