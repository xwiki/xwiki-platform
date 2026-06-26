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
package org.xwiki.realtime.test;

import java.util.List;
import java.util.concurrent.Callable;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.resources.objects.ObjectPropertyResource;
import org.xwiki.test.ui.TestUtils;

/**
 * Utility class for Realtime tests.
 *
 * @version $Id$
 * @since 17.7.0RC1
 * @since 17.4.4
 * @since 16.10.11
 */
public final class RealtimeTestUtils
{
    private static final String NETFLUX = "'netflux'";

    private static final String BAD_NETFLUX = "'bad-netflux'";

    private RealtimeTestUtils()
    {
    }

    /**
     * Simulates a failed WebSocket connection by modifying the realtime configuration to use a non-existing Netflux
     * WebSocket, executing the provided callable, and then restoring the original configuration.
     *
     * @param <T> the type of the result returned by the callable
     * @param setup the test setup
     * @param callable the callable to execute while simulating a failed WebSocket connection
     * @return the result of the callable
     * @throws Exception if an error occurs while executing the callable
     */
    public static <T> T simulateFailedWebSocketConnection(TestUtils setup, Callable<T> callable) throws Exception
    {
        // Modify the realtime configuration to use a non-existing Netflux WebSocket end-point.
        setup.loginAsSuperAdmin();
        ObjectPropertyReference realtimeConfigReference = new ObjectPropertyReference(
            "content",
            new ObjectReference(
                "XWiki.UIExtensionClass[0]",
                new DocumentReference(
                    setup.getCurrentWiki(),
                    List.of("XWiki", "Realtime"),
                    "Configuration"
                )
            )
        );
        Property realtimeConfigProperty = setup.rest().get(ObjectPropertyResource.class,
            realtimeConfigReference);
        String originalValue = realtimeConfigProperty.getValue();
        realtimeConfigProperty.setValue(originalValue.replace(NETFLUX, BAD_NETFLUX));
        TestUtils.assertStatusCodes(
            setup.rest().executePut(
                ObjectPropertyResource.class,
                realtimeConfigProperty,
                setup.rest().toElements(realtimeConfigReference)
            ),
            true,
            TestUtils.STATUS_ACCEPTED
        );

        try {
            return callable.call();

        } finally {
            // Restore the original realtime configuration.
            setup.maybeLeaveEditMode();
            setup.loginAsSuperAdmin();
            // The original value might used the 'bad-netflux' value due to a previous failed test.
            realtimeConfigProperty.setValue(originalValue.replace(BAD_NETFLUX, NETFLUX));
            TestUtils.assertStatusCodes(
                setup.rest().executePut(
                    ObjectPropertyResource.class,
                    realtimeConfigProperty,
                    setup.rest().toElements(realtimeConfigReference)
                ),
                true,
                TestUtils.STATUS_ACCEPTED
            );
        }
    }
}
