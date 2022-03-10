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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.mentions.DisplayStyle.FIRST_NAME;
import static org.xwiki.mentions.DisplayStyle.FULL_NAME;
import static org.xwiki.mentions.DisplayStyle.LOGIN;

/**
 * Test of {@link UserMentionsFormatter}.
 *
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
class UserMentionsFormatterTest
{
    @InjectMockComponents
    private UserMentionsFormatter formatter;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Test
    void formatMentionNoFirstNameNoLastName()
    {
        UserReference userReference = mock(UserReference.class);
        UserProperties userProperties = mock(UserProperties.class);
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User");

        when(this.userReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(userReference);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(userProperties);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(documentReference);
        when(userProperties.getFirstName()).thenReturn(null);
        when(userProperties.getLastName()).thenReturn(null);

        String actual = this.formatter.formatMention("xwiki:XWiki.User", FULL_NAME);

        Assertions.assertEquals("@User", actual);
    }

    @Test
    void formatMentionNoFirstName()
    {
        UserReference userReference = mock(UserReference.class);
        UserProperties userProperties = mock(UserProperties.class);
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User");

        when(this.userReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(userReference);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(userProperties);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(documentReference);
        when(userProperties.getFirstName()).thenReturn(null);
        when(userProperties.getLastName()).thenReturn("Last Name");

        String actual = this.formatter.formatMention("xwiki:XWiki.User", FULL_NAME);

        Assertions.assertEquals("@Last Name", actual);
    }

    @Test
    void formatMentionNoLastName()
    {
        UserReference userReference = mock(UserReference.class);
        UserProperties userProperties = mock(UserProperties.class);
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User");

        when(this.userReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(userReference);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(userProperties);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(documentReference);
        when(userProperties.getFirstName()).thenReturn("First Name");
        when(userProperties.getLastName()).thenReturn(null);

        String actual = this.formatter.formatMention("xwiki:XWiki.User", FULL_NAME);

        Assertions.assertEquals("@First Name", actual);
    }

    @Test
    void formatMentionFirstName()
    {
        UserReference userReference = mock(UserReference.class);
        UserProperties userProperties = mock(UserProperties.class);
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User");

        when(this.userReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(userReference);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(userProperties);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(documentReference);
        when(userProperties.getFirstName()).thenReturn("First Name");
        when(userProperties.getLastName()).thenReturn(null);

        String actual = this.formatter.formatMention("xwiki:XWiki.User", FIRST_NAME);

        Assertions.assertEquals("@First Name", actual);
    }

    @Test
    void formatMentionLogin()
    {
        UserReference userReference = mock(UserReference.class);
        UserProperties userProperties = mock(UserProperties.class);
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User");

        when(this.userReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(userReference);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(userProperties);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(documentReference);
        when(userProperties.getFirstName()).thenReturn("First Name");
        when(userProperties.getLastName()).thenReturn(null);

        String actual = this.formatter.formatMention("xwiki:XWiki.User", LOGIN);

        Assertions.assertEquals("@User", actual);
    }

    @Test
    void formatMentionFullName()
    {
        UserReference userReference = mock(UserReference.class);
        UserProperties userProperties = mock(UserProperties.class);
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "User");

        when(this.userReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(userReference);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(userProperties);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.User")).thenReturn(documentReference);
        when(userProperties.getFirstName()).thenReturn("First Name");
        when(userProperties.getLastName()).thenReturn("Last Name");

        String actual = this.formatter.formatMention("xwiki:XWiki.User", FULL_NAME);

        Assertions.assertEquals("@First Name Last Name", actual);
    }
}