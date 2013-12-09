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
package org.xwiki.crypto.internal.encoder;

import org.junit.Before;
import org.junit.Rule;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

public class HexBinaryStringEncoderTest extends AbstractBinaryStringEncoderTest
{
    {
        ENCODED_BYTES =
                  "4b727970746f677261706869652028766f6e20677269656368697363683a20ce"
                + "bacf81cf85cf80cf84cf8ccf822c20e2809e766572626f7267656ee2809c2075"
                + "6e6420ceb3cf81ceaccf86ceb5ceb9cebd2c20e2809e73636872656962656ee2"
                + "809c2920697374206469652057697373656e7363686166742064657220566572"
                + "7363686cc3bc7373656c756e6720766f6e20496e666f726d6174696f6e656e2e";

        WRAPPED_ENCODED_BYTES =
                  "4b727970746f677261706869652028766f6e20677269656368697363683a20ce" + '\n'
                + "bacf81cf85cf80cf84cf8ccf822c20e2809e766572626f7267656ee2809c2075" + '\n'
                + "6e6420ceb3cf81ceaccf86ceb5ceb9cebd2c20e2809e73636872656962656ee2" + '\n'
                + "809c2920697374206469652057697373656e7363686166742064657220566572" + '\n'
                + "7363686cc3bc7373656c756e6720766f6e20496e666f726d6174696f6e656e2e" + '\n';
    }

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<BinaryStringEncoder> mocker =
        new MockitoComponentMockingRule(HexBinaryStringEncoder.class);

    @Before
    public void configure() throws Exception
    {
        encoder = mocker.getComponentUnderTest();
    }
}
