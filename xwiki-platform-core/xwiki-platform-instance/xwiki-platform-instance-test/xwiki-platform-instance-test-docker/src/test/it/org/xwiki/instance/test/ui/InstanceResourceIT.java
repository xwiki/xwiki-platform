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
package org.xwiki.instance.test.ui;

import java.util.UUID;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.jupiter.api.Test;
import org.xwiki.instance.rest.InstanceResource;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Functional tests for {@link InstanceResource}.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@UITest
class InstanceResourceIT
{
    @Test
    void getInstanceIdReturnsValidUUID(TestUtils setup) throws Exception
    {
        GetMethod get = setup.rest().executeGet(InstanceResource.class);
        try {
            assertEquals(HttpStatus.SC_OK, get.getStatusCode());
            String body = get.getResponseBodyAsString().trim();
            assertDoesNotThrow(() -> UUID.fromString(body));
        } finally {
            get.releaseConnection();
        }
    }

    @Test
    void getInstanceIdIsIdempotent(TestUtils setup) throws Exception
    {
        GetMethod get1 = setup.rest().executeGet(InstanceResource.class);
        String id1;
        try {
            assertEquals(HttpStatus.SC_OK, get1.getStatusCode());
            id1 = get1.getResponseBodyAsString().trim();
        } finally {
            get1.releaseConnection();
        }
        assertNotNull(id1, "Instance id should not be null");

        GetMethod get2 = setup.rest().executeGet(InstanceResource.class);
        String id2;
        try {
            assertEquals(HttpStatus.SC_OK, get2.getStatusCode());
            id2 = get2.getResponseBodyAsString().trim();
        } finally {
            get2.releaseConnection();
        }

        assertEquals(id1, id2);
    }
}
