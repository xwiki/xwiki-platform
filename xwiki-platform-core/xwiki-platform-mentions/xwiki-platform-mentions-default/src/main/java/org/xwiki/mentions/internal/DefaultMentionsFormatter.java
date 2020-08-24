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
package org.xwiki.mentions.internal;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.xwiki.mentions.DisplayStyle.FIRST_NAME;
import static org.xwiki.mentions.DisplayStyle.LOGIN;

/**
 * Default implementation of {@link MentionsFormatter}.
 *
 * @version $Id$
 * @since 12.6
 */
@Singleton
@Component
public class DefaultMentionsFormatter implements MentionsFormatter
{
    @Inject
    private UserPropertiesResolver userPropertiesResolver;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public String formatMention(String userReference, DisplayStyle style)
    {
        UserReference resolve = this.userReferenceResolver.resolve(userReference);
        UserProperties userProperties =
            this.userPropertiesResolver.resolve(resolve);
        String firstName = Objects.toString(userProperties.getFirstName(), "");
        String lastName = Objects.toString(userProperties.getLastName(), "");
        final String content;
        if (Objects.equals(style, LOGIN) || Objects.equals(firstName, "") && Objects.equals(lastName, "")) {
            // if the login is asked explicitly we display it
            // if the user has no first name and no last name, we display the login by default
            content = this.documentReferenceResolver.resolve(userReference).getName();
        } else if (Objects.equals(style, FIRST_NAME)) {
            content = firstName;
        } else {
            content = String.format("%s %s", firstName, lastName).trim();
        }
        return "@" + content;
    }
}
