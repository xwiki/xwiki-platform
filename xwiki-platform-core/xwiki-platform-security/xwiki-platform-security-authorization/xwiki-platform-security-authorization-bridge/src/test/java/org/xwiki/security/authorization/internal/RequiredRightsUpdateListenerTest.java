package org.xwiki.security.authorization.internal;

import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserDeletingDocumentEvent;
import com.xpn.xwiki.internal.event.UserUpdatingDocumentEvent;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequiredRightsUpdateListener}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class RequiredRightsUpdateListenerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    @MockComponent
    private AuthorizationManager authorizationManager;

    @InjectMockComponents
    private RequiredRightsUpdateListener listener;

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteWithRequiredRight(boolean allowRight)
    {
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        XWikiDocument originalDocument = document.clone();
        document.setOriginalDocument(originalDocument);
        originalDocument.setRequiredRightsActivated(true);
        originalDocument.getRequiredRights().setRights(Set.of(Right.PROGRAM));

        when(this.authorizationManager.hasAccess(Right.PROGRAM, USER_REFERENCE, DOCUMENT_REFERENCE))
            .thenReturn(allowRight);

        UserDeletingDocumentEvent event = new UserDeletingDocumentEvent(USER_REFERENCE, DOCUMENT_REFERENCE);
        this.listener.onEvent(event, document, null);
        assertEquals(!allowRight, event.isCanceled());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void updateEnablingRequiredRights(boolean allowRight)
    {
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        XWikiDocument originalDocument = document.clone();
        document.setOriginalDocument(originalDocument);
        // Enable required rights on the updated document.
        document.getRequiredRights().setRights(Set.of(Right.PROGRAM));
        document.setRequiredRightsActivated(true);

        when(this.authorizationManager.hasAccess(Right.PROGRAM, USER_REFERENCE, DOCUMENT_REFERENCE))
            .thenReturn(allowRight);

        UserUpdatingDocumentEvent event = new UserUpdatingDocumentEvent(USER_REFERENCE, DOCUMENT_REFERENCE);
        this.listener.onEvent(event, document, null);
        assertEquals(!allowRight, event.isCanceled());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void increaseRequiredRight(boolean allowRight)
    {
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        document.setRequiredRightsActivated(true);
        XWikiDocument originalDocument = document.clone();
        document.setOriginalDocument(originalDocument);
        // Enable required rights on the updated document.
        document.getRequiredRights().setRights(Set.of(Right.PROGRAM));
        originalDocument.getRequiredRights().setRights(Set.of(Right.SCRIPT));

        when(this.authorizationManager.hasAccess(Right.PROGRAM, USER_REFERENCE, DOCUMENT_REFERENCE))
            .thenReturn(allowRight);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, USER_REFERENCE, DOCUMENT_REFERENCE))
            .thenReturn(true);

        UserUpdatingDocumentEvent event = new UserUpdatingDocumentEvent(USER_REFERENCE, DOCUMENT_REFERENCE);
        this.listener.onEvent(event, document, null);
        assertEquals(!allowRight, event.isCanceled());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void decreaseRequiredRight(boolean allowRight)
    {
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        document.setRequiredRightsActivated(true);
        XWikiDocument originalDocument = document.clone();
        document.setOriginalDocument(originalDocument);
        // Enable required rights on the updated document.
        originalDocument.getRequiredRights().setRights(Set.of(Right.PROGRAM));
        document.getRequiredRights().setRights(Set.of(Right.SCRIPT));

        when(this.authorizationManager.hasAccess(Right.PROGRAM, USER_REFERENCE, DOCUMENT_REFERENCE))
            .thenReturn(allowRight);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, USER_REFERENCE, DOCUMENT_REFERENCE))
            .thenReturn(true);

        UserUpdatingDocumentEvent event = new UserUpdatingDocumentEvent(USER_REFERENCE, DOCUMENT_REFERENCE);
        this.listener.onEvent(event, document, null);
        assertEquals(!allowRight, event.isCanceled());
    }
}
