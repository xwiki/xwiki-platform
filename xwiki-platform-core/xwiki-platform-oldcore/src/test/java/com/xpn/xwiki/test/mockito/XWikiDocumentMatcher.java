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
package com.xpn.xwiki.test.mockito;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.Equality;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Match a XWikiDocument parameter with a reference.
 * 
 * @version $Id$
 * @since 9.0RC1
 */
public class XWikiDocumentMatcher implements ArgumentMatcher<XWikiDocument>
{
    private final DocumentReference reference;

    public XWikiDocumentMatcher(DocumentReference reference)
    {
        this.reference = reference;
    }

    @Override
    public boolean matches(XWikiDocument argument)
    {
        if (argument != null) {
            if (this.reference.getLocale() != null) {
                return Equality.areEqual(this.reference, argument.getDocumentReferenceWithLocale());
            } else {
                return Equality.areEqual(this.reference, argument.getDocumentReference());
            }
        }

        return false;
    }
}
