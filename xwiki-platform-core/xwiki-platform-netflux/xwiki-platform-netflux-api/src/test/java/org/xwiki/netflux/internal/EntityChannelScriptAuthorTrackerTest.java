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
package org.xwiki.netflux.internal;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EntityChannelScriptAuthorTracker}.
 *
 * @version $Id$
 */
@ComponentTest
class EntityChannelScriptAuthorTrackerTest
{
    @InjectMockComponents
    private EntityChannelScriptAuthorTracker tracker;

    @MockComponent
    private EntityChannelStore entityChannels;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserReferenceSerializer;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @MockComponent
    @Named("explicit")
    private EntityReferenceResolver<String> explicitEntityReferenceResolver;

    @Mock(name = "alice")
    private UserReference alice;

    @Mock(name = "bob")
    private UserReference bob;

    @Mock(name = "carol")
    private UserReference carol;

    private DocumentReference documentReference = new DocumentReference("test", "Space", "Page");

    private DocumentReference translationReference = new DocumentReference(this.documentReference, Locale.FRENCH);

    private ObjectPropertyReference objectPropertyReference =
        new ObjectPropertyReference("text", new ObjectReference("Some.Class[0]", this.documentReference));

    @BeforeEach
    void beforeEach()
    {
        // Alice has only script rights.
        DocumentReference aliceReference = new DocumentReference("xwiki", "Users", "Alice");
        when(this.documentUserReferenceSerializer.serialize(this.alice)).thenReturn(aliceReference);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, aliceReference,
            this.documentReference))
            .thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, aliceReference,
            this.translationReference))
            .thenReturn(true);

        // Bob has no script rights.
        DocumentReference bobReference = new DocumentReference("xwiki", "Users", "Bob");
        when(this.documentUserReferenceSerializer.serialize(this.bob)).thenReturn(bobReference);

        // Carol has programming rights.
        DocumentReference carolReference = new DocumentReference("xwiki", "Users", "Carol");
        when(this.documentUserReferenceSerializer.serialize(this.carol)).thenReturn(carolReference);
        when(this.authorizationManager.hasAccess(Right.PROGRAM, null, carolReference, this.documentReference))
            .thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.PROGRAM, null, carolReference, this.translationReference))
            .thenReturn(true);

        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.carol);

        when(this.explicitEntityReferenceResolver.resolve("Some.Class[0].text", EntityType.OBJECT_PROPERTY,
            documentReference)).thenReturn(this.objectPropertyReference);
    }

    @Test
    void getScriptAuthor()
    {
        // No messages received yet on this channel.
        assertFalse(this.tracker.getScriptAuthor("channelKey").isPresent());

        EntityChannel entityChannel =
            new EntityChannel(documentReference, List.of("fr", "content", "wiki"), "channelKey");
        when(this.entityChannels.getChannel(entityChannel.getKey())).thenReturn(Optional.of(entityChannel));

        // Alice sends a message on the channel.
        this.tracker.maybeUpdateScriptAuthor(entityChannel, alice);

        assertEquals(alice, this.tracker.getScriptAuthor("channelKey").get().getAuthor());

        // Bob sends a message on the channel. Bob has less script rights than Alice.
        this.tracker.maybeUpdateScriptAuthor(entityChannel, bob);

        assertEquals(bob, this.tracker.getScriptAuthor("channelKey").get().getAuthor());

        // Alice sends another message on the channel, but it should not change the script author because Alice has more
        // script rights than Bob (we can only lower the script level).
        this.tracker.maybeUpdateScriptAuthor(entityChannel, alice);

        // Close the entity channel.
        when(this.entityChannels.getChannel(entityChannel.getKey())).thenReturn(Optional.empty());

        // We can still get the script author once.
        assertEquals(bob, this.tracker.getScriptAuthor("channelKey").get().getAuthor());

        // The script author was reset.
        assertFalse(this.tracker.getScriptAuthor("channelKey").isPresent());
    }
}
