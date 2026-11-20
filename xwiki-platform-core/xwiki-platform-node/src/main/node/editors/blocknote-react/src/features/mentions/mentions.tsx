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
 * Mentions a user in the edited content. The mention trigger opens a menu that suggests the users returned by the
 * `UserSuggester` bound in the service container. Picking one hands it to the `MentionInserter` bound there, which
 * writes it into the content. Looking a user up and representing a mention are each backend specific, so this feature
 * contributes nothing unless the host application binds both services and the inserter says it can write a mention in
 * the edited content.
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
