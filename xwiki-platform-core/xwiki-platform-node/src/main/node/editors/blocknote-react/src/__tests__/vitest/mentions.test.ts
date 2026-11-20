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
// @vitest-environment jsdom

import {
  shouldOpenMentionMenu,
  toSuggestions,
} from "../../features/mentions/mentionMenu";
import { mentionsFeature } from "../../features/mentions/mentions";
import {
  mentionInserterName,
  userSuggesterName,
} from "../../services/mentions";
import { FULL_SYNTAX } from "../playwright-ct/syntax.mock";
import { Container } from "inversify";
import { describe, expect, test, vi } from "vitest";
import type { EditorInstance } from "../../editorInstance";
import type { FeatureContext } from "../../features/editorFeature";
import type {
  MentionInserter,
  MentionableUser,
  UserSuggester,
} from "../../services/mentions";

const user: MentionableUser = {
  reference: "XWiki.Admin",
  label: "Administrator",
  hint: "admin@example.com",
};

const userSuggester: UserSuggester = {
  suggestUsers: async () => [user],
};

/**
 * @param suggester - whether a user suggester is bound, as a host application that can look users up does
 * @param inserter - whether a mention inserter is bound, as a host application that can write mentions does
 * @param canInsert - what the mention inserter answers when asked whether the edited content can hold a mention
 * @returns the context the mentions feature contributes to
 */
function context({
  suggester = true,
  inserter = true,
  canInsert = true,
}: { suggester?: boolean; inserter?: boolean; canInsert?: boolean } = {}) {
  const serviceContainer = new Container();
  if (suggester) {
    serviceContainer
      .bind<UserSuggester>(userSuggesterName)
      .toConstantValue(userSuggester);
  }
  if (inserter) {
    serviceContainer
      .bind<MentionInserter>(mentionInserterName)
      .toConstantValue({ canInsert: () => canInsert, insert: vi.fn() });
  }
  return {
    serviceContainer,
    options: { features: {} },
    syntax: FULL_SYNTAX,
    t: (key: string) => key,
  } as FeatureContext;
}

/**
 * @param textBefore - the text the mention trigger is about to be typed after
 * @returns a transaction whose selection sits right after the given text
 */
function transactionAfter(textBefore: string) {
  return {
    selection: {
      $from: {
        parent: {
          textBetween: (from: number, to: number) => textBefore.slice(from, to),
        },
        parentOffset: textBefore.length,
      },
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } as any;
}

describe("mentionsFeature", () => {
  test("shows the mention menu when both mention services are bound", () => {
    expect(mentionsFeature.contribute(context())?.components).toBeDefined();
  });

  test("contributes nothing without a user suggester", () => {
    expect(
      mentionsFeature.contribute(context({ suggester: false })),
    ).toBeUndefined();
  });

  test("contributes nothing without a mention inserter", () => {
    expect(
      mentionsFeature.contribute(context({ inserter: false })),
    ).toBeUndefined();
  });

  test("contributes nothing when the edited content can't hold a mention", () => {
    expect(
      mentionsFeature.contribute(context({ canInsert: false })),
    ).toBeUndefined();
  });
});

describe("shouldOpenMentionMenu", () => {
  test.each([
    ["at the start of a block", ""],
    ["after a space", "Hello "],
    ["after an opening bracket", "Hello ("],
  ])("opens %s", (_label, textBefore) => {
    expect(shouldOpenMentionMenu(transactionAfter(textBefore))).toBe(true);
  });

  test.each([
    ["in the middle of a word", "alice"],
    ["in an email address", "john.doe"],
    ["after a digit", "user2"],
    ["after an accented letter", "chloé"],
  ])("doesn't open %s", (_label, textBefore) => {
    expect(shouldOpenMentionMenu(transactionAfter(textBefore))).toBe(false);
  });
});

describe("toSuggestions", () => {
  const inserter: MentionInserter = { canInsert: () => true, insert: vi.fn() };
  const instance = {} as EditorInstance;

  test("labels the item with the user name and its hint", () => {
    const items = toSuggestions(instance, inserter, [user], "Users");

    expect(items[0].title).toBe("Administrator");
    expect(items[0].subtext).toBe("admin@example.com");
    expect(items[0].group).toBe("Users");
  });

  test("hands the picked user to the mention inserter", () => {
    toSuggestions(instance, inserter, [user], "Users")[0].onItemClick!();

    expect(inserter.insert).toHaveBeenCalledWith(instance, user);
  });
});
