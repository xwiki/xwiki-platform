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
import {
  DefaultMentionFactory,
  getMentionAnchor,
} from "./DefaultMentionFactory";
import { describe, expect, it } from "vitest";

const user = { reference: "XWiki.Alice", label: "Alice" };

describe("getMentionAnchor", () => {
  it("replaces the reference separators and suffixes a 6 characters random part", () => {
    expect(getMentionAnchor("wiki:XWiki.Admin")).toMatch(
      /^wiki-XWiki-Admin-[a-z0-9]{6}$/,
    );
  });

  it("gives two mentions of the same user different anchors", () => {
    expect(getMentionAnchor("XWiki.Admin")).not.toBe(
      getMentionAnchor("XWiki.Admin"),
    );
  });
});

describe("DefaultMentionFactory", () => {
  it("builds a call to the mention macro for the mentioned user", () => {
    const mention = new DefaultMentionFactory().createMention(user);

    expect(mention.type).toBe("xwikiInlineMacro");
    const call = JSON.parse(mention.props.call);
    expect(call.name).toBe("mention");
    expect(call.parameters.reference).toBe("XWiki.Alice");
    expect(call.parameters.style).toBe("FULL_NAME");
    expect(call.parameters.anchor).toMatch(/^XWiki-Alice-[a-z0-9]{6}$/);
  });
});
