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
import { mentionRendererName } from "@xwiki/platform-editors-blocknote-react";
import { injectable } from "inversify";
import type {
  InlineMacroInvocation,
  MentionRenderer,
  MentionableUser,
} from "@xwiki/platform-editors-blocknote-react";
import type { Container } from "inversify";

/**
 * Computes the anchor identifying a mention, so that the notification sent for it can link back to it. Same scheme as
 * the CKEditor mentions, the mentioned reference followed by 6 random alphanumeric characters, so that two mentions
 * of the same user on the same page get different anchors.
 *
 * @param reference - the reference of the mentioned user
 * @returns the anchor of the mention
 */
function getMentionAnchor(reference: string): string {
  const random = Math.random().toString(36).substring(2, 8).padEnd(6, "0");
  return `${reference.replaceAll(/[.:]/g, "-")}-${random}`;
}

/**
 * Renders a mention as a call to the XWiki mention macro, so that it round-trips through the XWiki syntax like any
 * other server-rendered macro.
 *
 * @since 19.0.0RC1
 */
@injectable("Singleton")
class DefaultMentionRenderer implements MentionRenderer {
  public static bind(container: Container): void {
    container
      .bind<MentionRenderer>(mentionRendererName)
      .to(DefaultMentionRenderer)
      .inSingletonScope();
  }

  public render(user: MentionableUser): InlineMacroInvocation {
    return {
      kind: "inline",
      id: "mention",
      params: {
        reference: user.reference,
        style: "FULL_NAME",
        anchor: getMentionAnchor(user.reference),
      },
      body: { type: "none" },
    };
  }
}

export { DefaultMentionRenderer, getMentionAnchor };
