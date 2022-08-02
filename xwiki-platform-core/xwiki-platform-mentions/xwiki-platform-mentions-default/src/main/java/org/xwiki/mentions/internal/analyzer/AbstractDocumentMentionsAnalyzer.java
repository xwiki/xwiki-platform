/*
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
package org.xwiki.mentions.internal.analyzer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.internal.MentionedActorReference;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.rendering.block.MacroBlock;

/**
 * Common class for the mention analyzers.
 *
 * @version $Id$
 * @since 12.10
 */
public abstract class AbstractDocumentMentionsAnalyzer
{
    protected void addNewMention(MentionNotificationParameters mentionNotificationParameters, String type,
        MentionNotificationParameter mentionedActorReference)
    {
        mentionNotificationParameters.addNewMention(type, mentionedActorReference);
    }

    protected void addAllMentions(MentionNotificationParameters ret, Map<MentionedActorReference, List<String>> count,
        List<MacroBlock> mentionsMacros)
    {
        for (Map.Entry<MentionedActorReference, List<String>> e : count.entrySet()) {
            MentionedActorReference mentionReference = e.getKey();
            for (String anchorId : e.getValue()) {
                String reference = mentionReference.getReference();
                DisplayStyle displayStyle = findDisplayStyle(mentionsMacros, reference, anchorId);
                ret.addMention(mentionReference.getType(),
                    new MentionNotificationParameter(reference, anchorId, displayStyle));
            }
        }
    }

    protected Optional<MentionNotificationParameters> wrapResult(
        MentionNotificationParameters mentionNotificationParameters)
    {
        Optional<MentionNotificationParameters> ret;
        if (mentionNotificationParameters.getNewMentions().isEmpty()) {
            ret = Optional.empty();
        } else {
            ret = Optional.of(mentionNotificationParameters);
        }
        return ret;
    }

    protected DisplayStyle findDisplayStyle(List<MacroBlock> mentionsMacros, String reference, String anchor)
    {
        return mentionsMacros.stream()
            .filter(it -> Objects.equals(it.getParameter("reference"), reference)
                && Objects.equals(it.getParameter("anchor"), anchor))
            .findAny()
            .map(it -> DisplayStyle.getOrDefault(it.getParameter("style")))
            .orElse(DisplayStyle.FULL_NAME);
    }
}
