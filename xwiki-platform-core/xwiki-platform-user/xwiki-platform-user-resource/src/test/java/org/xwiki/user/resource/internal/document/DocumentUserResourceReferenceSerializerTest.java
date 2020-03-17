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
package org.xwiki.user.resource.internal.document;

import java.net.URL;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.user.internal.document.DocumentUserReference;
import org.xwiki.user.resource.internal.UserResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentUserResourceReferenceSerializer}.
 *
 * @version $Id$
 */
@ComponentTest
public class DocumentUserResourceReferenceSerializerTest
{
    @InjectMockComponents
    private DocumentUserResourceReferenceSerializer serializer;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @MockComponent
    private ModelContext modelContext;

    @Test
    void serialize() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentUserReference documentUserReference = mock(DocumentUserReference.class);
        when(documentUserReference.getReference()).thenReturn(documentReference);
        UserResourceReference userResourceReference = new UserResourceReference(documentUserReference);

        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(urlFactory.createURL("space", "page", xcontext)).thenReturn(new URL("https://localhost:8080/path/user"));
        when(xcontext.getURLFactory()).thenReturn(urlFactory);
        when(this.xwikiContextProvider.get()).thenReturn(xcontext);

        ExtendedURL extendedURL = this.serializer.serialize(userResourceReference);
        assertEquals("/path/user", extendedURL.serialize());

        verify(this.modelContext).setCurrentEntityReference(documentReference.getWikiReference());
    }

    @Test
    void serializeWhenNotDocumentUserReference()
    {
        UserResourceReference userReference = mock(UserResourceReference.class);
        Throwable exception = assertThrows(RuntimeException.class,
            () -> this.serializer.serialize(userReference));
        assertEquals("The passed user resource reference is not pointing to a wiki Document", exception.getMessage());
    }
}