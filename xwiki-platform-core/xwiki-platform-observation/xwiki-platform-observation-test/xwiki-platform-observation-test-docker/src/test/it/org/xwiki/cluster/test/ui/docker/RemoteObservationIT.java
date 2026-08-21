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
package org.xwiki.cluster.test.ui.docker;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.XWikiInstances;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Generic remove observation validation.
 * 
 * @version $Id$
 */
@UITest(xwikiInstances = @XWikiInstances(2))
class RemoteObservationIT
{
    @Test
    void observationId(TestUtils setup) throws Exception
    {
        String script = "{{velocity}}$services.observation.remote.isEnabled()-$services.observation.remote.id"
            + "{{/velocity}}";

        // The docker test framework enables remote events between the instances of a cluster and gives each of them
        // its index as identifier.
        setup.switchExecutor(0);
        assertEquals(String.format("true-%s", setup.getCurrentExecutor().getIndex()),
            setup.executeWikiPlain(script, Syntax.XWIKI_2_1));

        setup.switchExecutor(1);
        assertEquals(String.format("true-%s", setup.getCurrentExecutor().getIndex()),
            setup.executeWikiPlain(script, Syntax.XWIKI_2_1));
    }
}
