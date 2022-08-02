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
package org.xwiki.user.internal.document;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.user.api.XWikiUser;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link XWikiUserUserReferenceResolver}.
 *
 * @version $Id$
 */
@ComponentTest
public class XWikiUserUserReferenceResolverTest
{
    @InjectMockComponents
    private XWikiUserUserReferenceResolver resolver;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Test
    void resolve()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        UserReference userReference = this.resolver.resolve(new XWikiUser(documentReference));

        verify(this.documentReferenceUserReferenceResolver).resolve(documentReference);
    }
}
