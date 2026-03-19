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
package org.xwiki.flamingo.test.docker;

import java.net.URI;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Validate xwiki/bin/skin/ endpoint.
 * 
 * @version $Id$
 */
@UITest
class SkinActionIT
{
    @ParameterizedTest
    @ValueSource(strings = {"skin.properties", "..%252f/..%252f/skin.properties"})
    void validSkinProperties(String resource, TestUtils setup) throws Exception
    {
        URI uri = new URI(
            Strings.CS.removeEnd(setup.rest().getBaseURL(), "rest") + "bin/skin/" + resource);

        GetMethod response = setup.rest().executeGet(uri);

        try {
            assertEquals(200, response.getStatusCode(), "Failed to retrieve resource: " + resource);
            assertEquals("parent=\noutputSyntax=html/5.0\n", response.getResponseBodyAsString());
        } finally {
            response.releaseConnection();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"..%252f/..%252fWEB-INF/xwiki.cfg", "..%2f/..%2fWEB-INF/xwiki.cfg"})
    void invalidSkinResource(String resource, TestUtils setup) throws Exception
    {
        URI uri = new URI(
            Strings.CS.removeEnd(setup.rest().getBaseURL(), "rest") + "bin/skin/" + resource);

        GetMethod response = setup.rest().executeGet(uri);

        try {
            assertNotEquals(200, response.getStatusCode(), "Unexpectedly found resource: " + resource);
        } finally {
            response.releaseConnection();
        }
    }
}
