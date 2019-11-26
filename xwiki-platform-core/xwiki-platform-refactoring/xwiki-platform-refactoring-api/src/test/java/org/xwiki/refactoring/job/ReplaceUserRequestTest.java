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
package org.xwiki.refactoring.job;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link ReplaceUserRequest}.
 * 
 * @version $Id$
 */
public class ReplaceUserRequestTest
{
    @Test
    public void isReplaceDocumentAuthor()
    {
        ReplaceUserRequest request = new ReplaceUserRequest();

        assertFalse(request.isReplaceDocumentAuthor());

        request.setReplaceDocumentAuthor(true);

        assertTrue(request.isReplaceDocumentAuthor());

        request.setReplaceDocumentAuthor(false);

        assertFalse(request.isReplaceDocumentAuthor());
    }

    @Test
    public void isReplaceDocumentCreator()
    {
        ReplaceUserRequest request = new ReplaceUserRequest();

        assertFalse(request.isReplaceDocumentCreator());

        request.setReplaceDocumentCreator(true);

        assertTrue(request.isReplaceDocumentCreator());

        request.setReplaceDocumentCreator(false);

        assertFalse(request.isReplaceDocumentCreator());
    }

    @Test
    public void isReplaceDocumentContentAuthor()
    {
        ReplaceUserRequest request = new ReplaceUserRequest();

        assertFalse(request.isReplaceDocumentContentAuthor());

        request.setReplaceDocumentContentAuthor(true);

        assertTrue(request.isReplaceDocumentContentAuthor());

        request.setReplaceDocumentContentAuthor(false);

        assertFalse(request.isReplaceDocumentContentAuthor());
    }
}
