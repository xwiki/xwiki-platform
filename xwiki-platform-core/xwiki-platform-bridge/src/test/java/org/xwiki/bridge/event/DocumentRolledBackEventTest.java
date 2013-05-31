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
package org.xwiki.bridge.event;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

/**
 * Unit tests for {@link DocumentRolledBackEvent}.
 * 
 * @version $Id$
 */
public class DocumentRolledBackEventTest
{
    @Test
    public void match()
    {
        DocumentReference alice = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference bob = new DocumentReference("wiki", "Users", "Bob");

        Assert.assertFalse(new DocumentRolledBackEvent().matches(new DocumentRollingBackEvent()));
        Assert.assertTrue(new DocumentRolledBackEvent().matches(new DocumentRolledBackEvent()));
        Assert.assertTrue(new DocumentRolledBackEvent().matches(new DocumentRolledBackEvent(alice, "2.3")));

        Assert.assertFalse(new DocumentRolledBackEvent(bob).matches(new DocumentRolledBackEvent(alice, "1.5")));
        Assert.assertTrue(new DocumentRolledBackEvent(bob).matches(new DocumentRolledBackEvent(bob, "4.2")));

        Assert.assertFalse(new DocumentRolledBackEvent(bob, "3.1").matches(new DocumentRolledBackEvent(bob)));
        Assert.assertFalse(new DocumentRolledBackEvent(bob, "7.6").matches(new DocumentRolledBackEvent(bob, "7.5")));
        Assert.assertTrue(new DocumentRolledBackEvent(bob, "5.8").matches(new DocumentRolledBackEvent(bob, "5.8")));

        Assert.assertFalse(new DocumentRolledBackEvent(bob, "1.0").matches(new DocumentRolledBackEvent(alice, "1.0")));
    }
}
