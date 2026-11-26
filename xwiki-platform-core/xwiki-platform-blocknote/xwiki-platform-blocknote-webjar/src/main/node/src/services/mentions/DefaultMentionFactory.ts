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
  createInlineMacro,
  mentionFactoryName,
} from "@xwiki/platform-editors-blocknote-react";
import { injectable } from "inversify";
import type {
  InlineContentType,
  MentionFactory,
} from "@xwiki/platform-editors-blocknote-react";
import type { SuggestedUser } from "@xwiki/platform-user-suggest-api";
import type { Container } from "inversify";

/**
 * Computes the anchor identifying a mention, so that the notification sent for it can link back to it. Same scheme as
 * the CKEditor mentions, the mentioned reference followed by 6 random characters, so that two mentions of the same
 * user on the same page get different anchors.
 *
 * @param reference - the reference of the mentioned user
 * @returns the anchor of the mention
 */
function getMentionAnchor(reference: string): string {
  const random = Array.from(crypto.getRandomValues(new Uint8Array(3)), (byte) =>
    byte.toString(16).padStart(2, "0"),
  ).join("");
  return `${reference.replaceAll(/[.:]/g, "-")}-${random}`;
}

/**
 * Builds a mention as a call to the XWiki mention macro, so that it round-trips through the XWiki syntax like any
 * other server-rendered macro.
 *
 * @since 19.0.0RC1
 */
@injectable("Singleton")
class DefaultMentionFactory implements MentionFactory {
  public static bind(container: Container): void {
    container
      .bind<MentionFactory>(mentionFactoryName)
      .to(DefaultMentionFactory)
      .inSingletonScope();
  }

  public createMention(user: SuggestedUser): InlineContentType {
    return createInlineMacro({
      id: "mention",
      params: {
        reference: user.reference,
        style: "FULL_NAME",
        anchor: getMentionAnchor(user.reference),
      },
      body: { type: "none" },
    });
  }
}

export { DefaultMentionFactory, getMentionAnchor };
