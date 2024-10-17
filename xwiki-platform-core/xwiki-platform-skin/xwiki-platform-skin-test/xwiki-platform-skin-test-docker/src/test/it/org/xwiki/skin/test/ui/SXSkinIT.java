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
package org.xwiki.skin.test.ui;

import java.net.URI;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Verify the behavior of resource based skin resources.
 *
 * @version $Id$
 */
@UITest
class SXSkinIT
{
    @Test
    void pathTraversal(TestUtils setup) throws Exception
    {
        URI uri = new URI(setup.getURL("Main", "WebHome", "sx", "resource=../../WEB-INF/xwiki.cfg"));

        try (CloseableHttpResponse response = setup.rest().executeGet(uri)) {
            assertNotEquals(200, response.getCode());
        }
     }
}
