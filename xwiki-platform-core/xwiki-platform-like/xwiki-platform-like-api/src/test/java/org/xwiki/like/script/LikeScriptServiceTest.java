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
package org.xwiki.like.script;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.like.LikeConfiguration;
import org.xwiki.like.LikeException;
import org.xwiki.like.LikeManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LikeScriptService}.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@ComponentTest
class LikeScriptServiceTest
{
    @InjectMockComponents
    private LikeScriptService likeScriptService;

    @MockComponent
    private LikeManager likeManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private LikeConfiguration likeConfiguration;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Mock
    private Right likeRight;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private UserReference userReference;

    private DocumentReference docUserReference;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setup()
    {
        when(this.likeManager.getLikeRight()).thenReturn(this.likeRight);
        when(this.contextProvider.get()).thenReturn(this.xWikiContext);
        this.docUserReference = new DocumentReference("xwiki", "XWiki", "User");
        when(this.xWikiContext.getUserReference()).thenReturn(this.docUserReference);
        when(this.userReferenceResolver.resolve(docUserReference)).thenReturn(userReference);
    }

    @Test
    void isAuthorized()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");

        when(this.authorizationManager.hasAccess(this.likeRight, this.docUserReference, entityReference))
            .thenReturn(true);
        assertTrue(this.likeScriptService.isAuthorized(entityReference));
        verify(this.authorizationManager).hasAccess(this.likeRight, this.docUserReference, entityReference);

        when(this.authorizationManager.hasAccess(this.likeRight, this.docUserReference, entityReference))
            .thenReturn(false);
        assertFalse(this.likeScriptService.isAuthorized(entityReference));
        verify(this.authorizationManager, times(2)).hasAccess(this.likeRight, this.docUserReference, entityReference);
    }

    @Test
    void displayButton()
    {
        // like enabled and button always displayed -> displayButton returns true
        when(this.likeConfiguration.isEnabled()).thenReturn(true);
        when(this.likeConfiguration.alwaysDisplayButton()).thenReturn(true);
        assertTrue(this.likeScriptService.displayButton(null));
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");
        assertTrue(this.likeScriptService.displayButton(entityReference));

        // like enabled and button not always displayed -> displayButton returns false when there's no reference
        // or true when the reference can be accessed with Like right
        when(this.likeConfiguration.alwaysDisplayButton()).thenReturn(false);
        assertFalse(this.likeScriptService.displayButton(null));

        when(this.authorizationManager.hasAccess(this.likeRight, this.docUserReference, entityReference))
            .thenReturn(true);
        assertTrue(this.likeScriptService.displayButton(entityReference));
        verify(this.authorizationManager).hasAccess(this.likeRight, this.docUserReference, entityReference);

        // like disabled -> displayButton always return false
        when(this.likeConfiguration.isEnabled()).thenReturn(false);
        when(this.likeConfiguration.alwaysDisplayButton()).thenReturn(true);
        assertFalse(this.likeScriptService.displayButton(null));
        assertFalse(this.likeScriptService.displayButton(entityReference));

        when(this.likeConfiguration.alwaysDisplayButton()).thenReturn(false);
        assertFalse(this.likeScriptService.displayButton(null));
        assertFalse(this.likeScriptService.displayButton(entityReference));

        // check that this one has only be called once.
        verify(this.authorizationManager, times(1)).hasAccess(this.likeRight, this.docUserReference, entityReference);
    }

    @Test
    void like() throws LikeException
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");
        when(this.authorizationManager.hasAccess(this.likeRight, this.docUserReference, entityReference))
            .thenReturn(true);
        when(this.likeManager.saveLike(userReference, entityReference)).thenReturn(42L);
        Optional<Long> optionalLikedEntity = this.likeScriptService.like(entityReference);

        assertTrue(optionalLikedEntity.isPresent());
        assertSame(42L, optionalLikedEntity.get());
    }

    @Test
    void likeNotAuthorized()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");
        assertFalse(this.likeScriptService.like(entityReference).isPresent());
        assertEquals("[xwiki:XWiki.User] is not authorized to like [xwiki:Foo.Foo].", logCapture.getMessage(0));
    }

    @Test
    void likeNotDocument()
    {
        EntityReference entityReference = new EntityReference("something", EntityType.ATTACHMENT);
        assertFalse(this.likeScriptService.like(entityReference).isPresent());
        assertEquals("Like is only implemented for document for now. (Called with [Attachment something])",
            logCapture.getMessage(0));
    }

    @Test
    void likeError() throws LikeException
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");
        when(this.authorizationManager.hasAccess(this.likeRight, this.docUserReference, entityReference))
            .thenReturn(true);
        when(this.likeManager.saveLike(userReference, entityReference)).thenThrow(new LikeException("Problem"));
        assertFalse(this.likeScriptService.like(entityReference).isPresent());
        assertEquals("Error while liking [xwiki:Foo.Foo] by [xwiki:XWiki.User]", logCapture.getMessage(0));
    }

    @Test
    void unlike() throws LikeException
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");

        when(this.authorizationManager.hasAccess(this.likeRight, this.docUserReference, entityReference))
            .thenReturn(true);

        when(this.likeManager.getEntityLikes(entityReference)).thenReturn(41L);
        Optional<Long> optionalLikedEntity = this.likeScriptService.unlike(entityReference);

        assertTrue(optionalLikedEntity.isPresent());
        assertSame(41L, optionalLikedEntity.get());
        verify(this.likeManager).removeLike(userReference, entityReference);
    }

    @Test
    void unlikeNotAuthorized()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");
        assertFalse(this.likeScriptService.unlike(entityReference).isPresent());
        assertEquals("[xwiki:XWiki.User] is not authorized to unlike [xwiki:Foo.Foo].", logCapture.getMessage(0));
    }

    @Test
    void unlikeNotDocument()
    {
        EntityReference entityReference = new EntityReference("something", EntityType.ATTACHMENT);
        assertFalse(this.likeScriptService.unlike(entityReference).isPresent());
        assertEquals("Unlike is only implemented for document for now. (Called with [Attachment something])",
            logCapture.getMessage(0));
    }

    @Test
    void unlikeError() throws LikeException
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");
        when(this.authorizationManager.hasAccess(this.likeRight, this.docUserReference, entityReference))
            .thenReturn(true);
        when(this.likeManager.removeLike(userReference, entityReference)).thenThrow(new LikeException("Problem"));
        assertFalse(this.likeScriptService.unlike(entityReference).isPresent());
        assertEquals("Error while unliking [xwiki:Foo.Foo] by [xwiki:XWiki.User]", logCapture.getMessage(0));
    }

    @Test
    void getLikes() throws LikeException
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");

        when(this.likeManager.getEntityLikes(entityReference)).thenReturn(40L);
        Optional<Long> optionalLikedEntity = this.likeScriptService.getLikes(entityReference);

        assertTrue(optionalLikedEntity.isPresent());
        assertSame(40L, optionalLikedEntity.get());
    }

    @Test
    void getLikesError() throws LikeException
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");
        when(this.likeManager.getEntityLikes(entityReference)).thenThrow(new LikeException("Problem"));
        assertFalse(this.likeScriptService.getLikes(entityReference).isPresent());
        assertEquals("Error while getting like information for [xwiki:Foo.Foo]", logCapture.getMessage(0));
    }

    @Test
    void isLiked() throws LikeException
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");

        when(this.likeManager.isLiked(userReference, entityReference)).thenReturn(false);
        assertFalse(this.likeScriptService.isLiked(entityReference));

        when(this.likeManager.isLiked(userReference, entityReference)).thenReturn(true);
        assertTrue(this.likeScriptService.isLiked(entityReference));
    }

    @Test
    void isLikedError() throws LikeException
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Foo");
        when(this.likeManager.isLiked(userReference, entityReference)).thenThrow(new LikeException("Problem"));
        assertFalse(this.likeScriptService.isLiked(entityReference));
        assertEquals("Error while checking if [xwiki:Foo.Foo] is liked by [userReference]", logCapture.getMessage(0));
    }

    @Test
    void countUserLikes() throws LikeException
    {
        when(this.likeManager.countUserLikes(userReference)).thenReturn(43L);
        assertEquals(Optional.of(43L), this.likeScriptService.countUserLikes(this.userReference));
    }

    @Test
    void getUserLikes() throws LikeException
    {
        List<EntityReference> expectedList = Arrays.asList(
            mock(EntityReference.class),
            mock(EntityReference.class),
            mock(EntityReference.class));
        when(this.likeManager.getUserLikes(this.userReference, 2, 32)).thenReturn(expectedList);
        assertEquals(expectedList, this.likeScriptService.getUserLikes(this.userReference, 2, 32));
    }

    @Test
    void getLikers() throws LikeException
    {
        List<UserReference> expectedList = Arrays.asList(
            mock(UserReference.class),
            mock(UserReference.class),
            mock(UserReference.class));
        EntityReference entityReference = mock(EntityReference.class);
        when(this.likeManager.getLikers(entityReference, 12, 28)).thenReturn(expectedList);
        assertEquals(expectedList, this.likeScriptService.getLikers(entityReference, 12, 28));
    }
}
