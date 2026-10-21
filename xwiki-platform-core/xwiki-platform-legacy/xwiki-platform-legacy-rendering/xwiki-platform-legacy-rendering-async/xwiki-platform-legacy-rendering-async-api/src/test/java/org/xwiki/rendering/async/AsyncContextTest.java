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
package org.xwiki.rendering.async;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test of {@link AsyncContext} after being aspectified by {@link AsyncContextCompatibilityAspect}.
 *
 * @version $Id$
 */
class AsyncContextTest
{
    @Test
    void useRight()
    {
        AsyncContext asyncContext = mock(AsyncContext.class, CALLS_REAL_METHODS);
        DocumentReference user = new DocumentReference("wiki", "XWiki", "User");
        DocumentReference document = new DocumentReference("wiki", "Space", "Page");

        asyncContext.useRight(Right.VIEW, user, document, true);
        asyncContext.useRight(Right.EDIT, user, document, false);

        verify(asyncContext).useRight(Right.VIEW, user, document, Boolean.TRUE);
        verify(asyncContext).useRight(Right.EDIT, user, document, Boolean.FALSE);
    }
}
