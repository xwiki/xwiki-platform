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
  getMentionAnchor,
  queryMentionMenuItems,
} from "../../components/mentions/mentionMenu";
import { describe, expect, test, vi } from "vitest";
import type { MentionableUser } from "../../components/mentions/mentionMenu";

const user: MentionableUser = {
  reference: "XWiki.Admin",
  label: "Administrator",
  hint: "admin@example.com",
};

function getItems(users: MentionableUser[], editor: unknown) {
  return queryMentionMenuItems({
    query: "admin",
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    editor: editor as any,
    userSuggest: () => Promise.resolve(users),
    t: ((key: string) => key) as never,
  });
}

describe("getMentionAnchor", () => {
  test("replaces the reference separators and suffixes a 6 characters random part", () => {
    expect(getMentionAnchor("wiki:XWiki.Admin")).toMatch(
      /^wiki-XWiki-Admin-[a-z0-9]{6}$/,
    );
  });

  test("gives two mentions of the same user different anchors", () => {
    expect(getMentionAnchor("XWiki.Admin")).not.toBe(
      getMentionAnchor("XWiki.Admin"),
    );
  });
});

describe("queryMentionMenuItems", () => {
  test("inserts a mention macro invocation for the picked user", async () => {
    const editor = { replaceBlocks: vi.fn(), insertInlineContent: vi.fn() };

    const items = await getItems([user], editor);
    items[0].onItemClick!();

    const [content] = editor.insertInlineContent.mock.calls[0];
    const call = JSON.parse(content[0].props.call);
    expect(content[0].type).toBe("xwikiInlineMacro");
    expect(call.name).toBe("mention");
    expect(call.parameters.reference).toBe("XWiki.Admin");
    expect(call.parameters.style).toBe("FULL_NAME");
    expect(call.parameters.anchor).toMatch(/^XWiki-Admin-[a-z0-9]{6}$/);
  });

  test("labels the item with the user name and its hint", async () => {
    const items = await getItems([user], {});

    expect(items[0].title).toBe("Administrator");
    expect(items[0].subtext).toBe("admin@example.com");
  });
});
