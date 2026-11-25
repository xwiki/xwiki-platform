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
  createBlockNoteSchema,
  querySuggestionsMenuItems,
} from "../../blocknote";
import { insertMention } from "../../features/mentions/mentionInsertion";
import {
  shouldOpenMentionMenu,
  toSuggestions,
} from "../../features/mentions/mentionMenu";
import { mentionsFeature } from "../../features/mentions/mentions";
import { mentionFactoryName } from "../../services/mentionFactory";
import { FULL_SYNTAX } from "../playwright-ct/syntax.mock";
import { BlockNoteEditor } from "@blocknote/core";
import { userSuggesterProviderName } from "@xwiki/platform-user-suggest-api";
import { Container } from "inversify";
import { beforeAll, describe, expect, test, vi } from "vitest";
import type { InlineContentType } from "../../blocknote";
import type { EditorInstance } from "../../editorInstance";
import type { FeatureContext } from "../../features/editorFeature";
import type { MentionFactory } from "../../services/mentionFactory";
import type {
  SuggestedUser,
  UserSuggester,
  UserSuggesterProvider,
} from "@xwiki/platform-user-suggest-api";

const user: SuggestedUser = {
  reference: "XWiki.Admin",
  label: "Administrator",
  hint: "admin@example.com",
};

const userSuggester: UserSuggester = {
  suggestUsers: async () => [user],
};

// Plain text rather than a macro call, which the schema of these editors doesn't hold: the feature inserts whatever
// the factory builds.
const mention = {
  type: "text",
  text: "@Admin",
  styles: {},
} as unknown as InlineContentType;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyEditor = BlockNoteEditor<any, any, any>;

let editor: AnyEditor;

beforeAll(() => {
  editor = BlockNoteEditor.create({
    schema: createBlockNoteSchema(),
  }) as AnyEditor;
});

/**
 * @param suggester - whether the backend can look users up
 * @param factory - whether a mention factory is bound, as a host application that can represent mentions does
 * @param mentions - whether the syntax of the edited content can save a mention
 * @returns the context the mentions feature contributes to
 */
function context({
  suggester = true,
  factory = true,
  mentions = true,
}: { suggester?: boolean; factory?: boolean; mentions?: boolean } = {}) {
  const serviceContainer = new Container();
  serviceContainer
    .bind<UserSuggesterProvider>(userSuggesterProviderName)
    .toConstantValue({ get: () => (suggester ? userSuggester : undefined) });
  if (factory) {
    serviceContainer
      .bind<MentionFactory>(mentionFactoryName)
      .toConstantValue({ createMention: () => mention });
  }
  const syntax = structuredClone(FULL_SYNTAX);
  syntax.features.inlineContents.mentions = mentions;
  return {
    serviceContainer,
    options: { features: {} },
    syntax,
    // Identity stub: the keys are what the assertions below are tied to.
    t: (key: string) => key,
    getInstance: () => ({ editor }) as EditorInstance,
  } as FeatureContext;
}

/**
 * @param query - what the user typed after the slash
 * @returns the "Mention" item of the slash menu, when the given query keeps it
 */
function findMentionItem(query = "") {
  return querySuggestionsMenuItems({
    editor,
    query,
    syntax: FULL_SYNTAX,
    featureItems: mentionsFeature.contribute(context())!.slashMenuItems!(),
  }).find((item) => item.title === "blocknote.slashMenu.mention.title");
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

  test("contributes nothing without a mention factory", () => {
    expect(
      mentionsFeature.contribute(context({ factory: false })),
    ).toBeUndefined();
  });

  test("contributes nothing when the syntax can't save a mention", () => {
    expect(
      mentionsFeature.contribute(context({ mentions: false })),
    ).toBeUndefined();
  });

  test("adds a Mention entry to the slash menu", () => {
    const mentionItem = findMentionItem();

    expect(mentionItem).toBeDefined();
    expect(mentionItem!.subtext).toBe("blocknote.slashMenu.mention.subtext");
    expect(mentionItem!.aliases).toEqual(["mention", "user"]);
    expect(mentionItem!.group).toBe("blocknote.slashMenu.mention.group");
  });

  test("the Mention entry survives a query matching its alias", () => {
    expect(findMentionItem("user")).toBeDefined();
  });

  test("the Mention entry is filtered out by a query matching neither its title nor its aliases", () => {
    expect(findMentionItem("table")).toBeUndefined();
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
  const factory: MentionFactory = { createMention: vi.fn(() => mention) };

  test("labels the item with the user name and its hint", () => {
    const items = toSuggestions(editor, factory, [user], "Users");

    expect(items[0].title).toBe("Administrator");
    expect(items[0].subtext).toBe("admin@example.com");
    expect(items[0].group).toBe("Users");
  });

  test("hands the picked user to the mention factory", () => {
    toSuggestions(editor, factory, [user], "Users")[0].onItemClick!();

    expect(factory.createMention).toHaveBeenCalledWith(user);
  });
});

describe("insertMention", () => {
  const factory: MentionFactory = { createMention: () => mention };

  /**
   * @param content - the text of the paragraph the mention is inserted in
   * @param caret - the end of that paragraph, or its start, which is the only way to reach a caret followed by a
   *   space without building a selection out of the editor's API
   * @returns the text of the edited paragraph, and the text the caret is left after
   */
  function insertInto(content: string, caret: "start" | "end") {
    const textEditor = BlockNoteEditor.create({
      schema: createBlockNoteSchema(),
      initialContent: [{ type: "paragraph", content }],
    }) as AnyEditor;
    textEditor.setTextCursorPosition(textEditor.document[0], caret);

    insertMention(textEditor, factory, user);

    const { doc, selection } = textEditor.prosemirrorState;
    return {
      text: doc.textContent,
      beforeCaret: doc.textBetween(0, selection.to),
    };
  }

  test("inserts the mention built for the user, followed by a space", () => {
    expect(insertInto("a b", "end")).toEqual({
      text: "a b@Admin ",
      beforeCaret: "a b@Admin ",
    });
  });

  test("takes over the space the mention is inserted before, leaving the caret after it", () => {
    expect(insertInto(" a b", "start")).toEqual({
      text: "@Admin a b",
      beforeCaret: "@Admin ",
    });
  });
});
