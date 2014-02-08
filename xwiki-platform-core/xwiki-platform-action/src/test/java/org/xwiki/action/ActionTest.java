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
package org.xwiki.action;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.*;
import org.xwiki.resource.ActionId;
import org.xwiki.resource.Resource;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Action}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class ActionTest
{
    private class TestableAction extends AbstractAction
    {
        public TestableAction(int priority)
        {
            setPriority(priority);
        }

        @Override
        public List<ActionId> getSupportedActionIds()
        {
            return Arrays.asList(ActionId.VIEW);
        }

        @Override
        public void execute(Resource resource, ActionChain chain) throws ActionException
        {
        }
    };

    @Test
    public void priority()
    {
        Action action1 = new TestableAction(500);
        assertEquals(500, action1.getPriority());
        assertThat(action1.getSupportedActionIds(), Matchers.contains(ActionId.VIEW));

        Action action2 = new TestableAction(200);
        assertEquals(300, action1.compareTo(action2));
    }
}
