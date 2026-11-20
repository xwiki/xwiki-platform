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
  insertMacroInvocation,
  mentionInserterName,
} from "@xwiki/platform-editors-blocknote-react";
import { injectable } from "inversify";
import type {
  EditorInstance,
  MentionInserter,
  MentionInsertionContext,
  MentionableUser,
} from "@xwiki/platform-editors-blocknote-react";
import type { Container } from "inversify";

/**
 * Computes the anchor identifying a mention, so that the notification sent for it can link back to it. Same scheme as
 * the CKEditor mentions: the mentioned reference followed by 6 random alphanumeric characters, which makes a
 * collision between two mentions of the same user on the same page unlikely enough.
 *
 * @param reference - the reference of the mentioned user
 * @returns the anchor of the mention
 */
function getMentionAnchor(reference: string): string {
  const random = Math.random().toString(36).substring(2, 8).padEnd(6, "0");
  return `${reference.replaceAll(/[.:]/g, "-")}-${random}`;
}

/**
 * Writes a mention as a call to the XWiki mention macro, inserted as an inline macro so that it round-trips through
 * the XWiki syntax like any other server-rendered macro.
 *
 * @since 19.0.0RC1
 */
@injectable("Singleton")
class DefaultMentionInserter implements MentionInserter {
  public static bind(container: Container): void {
    container
      .bind<MentionInserter>(mentionInserterName)
      .to(DefaultMentionInserter)
      .inSingletonScope();
  }

  public canInsert({ options, syntax }: MentionInsertionContext): boolean {
    // A mention is a macro call, so it needs both the macros of the editor (which give the schema its inline macro
    // type) and a syntax that can save an inline macro.
    return (
      options.features?.macros !== false &&
      syntax.features.inlineContents.macros
    );
  }

  public insert(instance: EditorInstance, user: MentionableUser): void {
    insertMacroInvocation(instance.editor, {
      kind: "inline",
      id: "mention",
      params: {
        reference: user.reference,
        style: "FULL_NAME",
        anchor: getMentionAnchor(user.reference),
      },
      body: { type: "none" },
    });
    // A rendering macro is never inline-only, so a call alone in a paragraph is saved as a block macro. Same as the
    // CKEditor mentions: a space after the call keeps the mention inline, and lets the user keep typing after it.
    instance.editor.insertInlineContent(" ", { updateSelection: true });
  }
}

export { DefaultMentionInserter, getMentionAnchor };
