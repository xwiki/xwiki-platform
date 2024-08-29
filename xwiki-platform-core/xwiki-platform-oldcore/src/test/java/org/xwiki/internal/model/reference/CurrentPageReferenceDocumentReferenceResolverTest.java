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
package org.xwiki.internal.model.reference;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link  CurrentPageReferenceDocumentReferenceResolver}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@ComponentTest
@ComponentList({
    CurrentReferenceDocumentReferenceResolver.class,
    CurrentReferenceEntityReferenceResolver.class,
    CurrentEntityReferenceProvider.class,
    DefaultModelConfiguration.class,
    DefaultEntityReferenceProvider.class
})
class CurrentPageReferenceDocumentReferenceResolverTest
{
    @InjectMockComponents
    private CurrentPageReferenceDocumentReferenceResolver resolver;

    @MockComponent
    private DocumentAccessBridge dab;

    @Test
    void resolveWhenDocumentReferenceExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "pageA", "WebHome");
        when(this.dab.exists(documentReference)).thenReturn(true);

        assertEquals(documentReference, this.resolver.resolve(new PageReference("wiki", "pageA")));
    }

    @Test
    void resolveWhenDocumentReferenceDoesntExistsAndTopLevelDocument() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "pageA", "WebHome");
        when(this.dab.exists(documentReference)).thenReturn(false);

        assertEquals(documentReference, this.resolver.resolve(new PageReference("wiki", "pageA")));
    }

    @Test
    void resolveWhenDocumentReferenceDoesntExistsAndNotTopLevelDocumentAndTerminalDocumentExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("pageA", "pageB"), "WebHome");
        when(this.dab.exists(documentReference)).thenReturn(false);
        DocumentReference terminalReference = new DocumentReference("wiki", "pageA", "pageB");
        when(this.dab.exists(terminalReference)).thenReturn(true);

        assertEquals(terminalReference, this.resolver.resolve(new PageReference("wiki", "pageA", "pageB")));
    }

    @Test
    void resolveWhenDocumentReferenceDoesntExistsAndNotTopLevelDocumentAndTerminalDocumentDoesntExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("pageA", "pageB"), "WebHome");
        when(this.dab.exists(documentReference)).thenReturn(false);
        DocumentReference terminalReference = new DocumentReference("wiki", "pageA", "pageB");
        when(this.dab.exists(terminalReference)).thenReturn(false);

        assertEquals(documentReference, this.resolver.resolve(new PageReference("wiki", "pageA", "pageB")));
    }
}
