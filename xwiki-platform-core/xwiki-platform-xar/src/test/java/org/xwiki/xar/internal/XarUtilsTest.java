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
package org.xwiki.xar.internal;

import java.util.Locale;

import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xar.XarException;

import static org.junit.Assert.assertEquals;

/**
 * Validate {@link XarUtils}.
 * 
 * @version $Id$
 */
public class XarUtilsTest
{
    @Test
    public void getReference() throws XarException
    {
        LocalDocumentReference documentReference =
            new LocalDocumentReference("space.nestedspace", "page", Locale.ENGLISH);
        LocalDocumentReference nestedDocumentReference =
            new LocalDocumentReference(new EntityReference("page", EntityType.DOCUMENT, new EntityReference(
                "nestedspace", EntityType.SPACE, new EntityReference("space", EntityType.SPACE))), Locale.ENGLISH);

        assertEquals(nestedDocumentReference,
            XarUtils.getReference(getClass().getResourceAsStream("/document/document.xml")));
        assertEquals(nestedDocumentReference,
            XarUtils.getReference(getClass().getResourceAsStream("/document/newdocument.xml")));
        assertEquals(documentReference,
            XarUtils.getReference(getClass().getResourceAsStream("/document/legacydocument.xml")));
    }
}
