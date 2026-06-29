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
package org.xwiki.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Validate {@link VoidMailListener}.
 * 
 * @version $Id$
 */
class VoidMailListenerTest
{
    @Test
    void test()
    {
        VoidMailListener listener = new VoidMailListener();

        listener.onPrepareBegin(null, null);
        listener.onPrepareMessageSuccess(null, null);
        listener.onPrepareMessageError(null, null, null);
        listener.onPrepareFatalError(null, null);
        listener.onPrepareEnd(null);
        listener.onSendMessageSuccess(null, null);
        listener.onSendMessageError(null, null, null);
        listener.onSendMessageFatalError(null, null, null);

        assertSame(EmptyMailStatusResult.INSTANCE, listener.getMailStatusResult());
    }
}
