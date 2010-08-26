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
package org.xwiki.observation;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.observation.event.AbstractCancelableEvent;
import org.xwiki.observation.event.CancelableEvent;


/**
 * Tests {@link AbstractCancelableEvent}.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class CancelableEventTest
{
    /**
     * Tested event implementation.
     */
    private class TestEvent extends AbstractCancelableEvent
    {
        /** Serial version ID. */
        private static final long serialVersionUID = 1L;
    }

    @Test
    public void testCancel()
    {
        CancelableEvent event = new TestEvent();
        Assert.assertFalse(event.isCanceled());
        Assert.assertNull(event.getReason());
        event.cancel();
        Assert.assertTrue(event.isCanceled());
        Assert.assertNull(event.getReason());
    }

    @Test
    public void testCancelWithReason()
    {
        String reason = "Tralala";
        CancelableEvent event = new TestEvent();
        Assert.assertFalse(event.isCanceled());
        Assert.assertNull(event.getReason());
        event.cancel(reason);
        Assert.assertTrue(event.isCanceled());
        Assert.assertEquals(reason, event.getReason());
    }
}

