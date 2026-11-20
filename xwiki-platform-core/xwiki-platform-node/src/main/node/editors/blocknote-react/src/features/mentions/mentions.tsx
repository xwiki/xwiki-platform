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
import { MentionSuggestionController } from "./MentionSuggestionController";
import {
  mentionInserterName,
  userSuggesterName,
} from "../../services/mentions";
import type { MentionInserter } from "../../services/mentions";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
} from "../editorFeature";

/**
 * Mentions a user in the edited content: the menu opened by the mention trigger, which suggests the users provided by
 * the `UserSuggester` bound in the service container and hands the picked one to the `MentionInserter` bound there,
 * which writes it into the content. How a mention is looked up and how it is written are both backend specific, so
 * nothing is contributed unless the host application binds the two of them, and the inserter says it can write a
 * mention in the edited content.
 *
 * @since 19.0.0RC1
 * @beta
 */
const mentionsFeature: EditorFeature = {
  id: "mentions",
  contribute(context: FeatureContext): FeatureContribution | undefined {
    const { serviceContainer } = context;
    if (
      !serviceContainer.isBound(userSuggesterName) ||
      !serviceContainer.isBound(mentionInserterName)
    ) {
      return undefined;
    }
    const inserter = serviceContainer.get<MentionInserter>(mentionInserterName);
    return inserter.canInsert(context)
      ? { components: <MentionSuggestionController /> }
      : undefined;
  },
};

export { mentionsFeature };
