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

public class UrlBase64BinaryStringEncoderTest extends AbstractBinaryStringEncoderTest
{
    {
        ENCODED_BYTES =
                  "S3J5cHRvZ3JhcGhpZSAodm9uIGdyaWVjaGlzY2g6IM66z4HPhc-Az4TPjM-CLCDi"
                + "gJ52ZXJib3JnZW7igJwgdW5kIM6zz4HOrM-GzrXOuc69LCDigJ5zY2hyZWliZW7i"
                + "gJwpIGlzdCBkaWUgV2lzc2Vuc2NoYWZ0IGRlciBWZXJzY2hsw7xzc2VsdW5nIHZv"
                + "biBJbmZvcm1hdGlvbmVuLg..";

        WRAPPED_ENCODED_BYTES =
                  "S3J5cHRvZ3JhcGhpZSAodm9uIGdyaWVjaGlzY2g6IM66z4HPhc-Az4TPjM-CLCDi" + '\n'
                + "gJ52ZXJib3JnZW7igJwgdW5kIM6zz4HOrM-GzrXOuc69LCDigJ5zY2hyZWliZW7i" + '\n'
                + "gJwpIGlzdCBkaWUgV2lzc2Vuc2NoYWZ0IGRlciBWZXJzY2hsw7xzc2VsdW5nIHZv" + '\n'
                + "biBJbmZvcm1hdGlvbmVuLg..";
    }

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<BinaryStringEncoder> mocker =
        new MockitoComponentMockingRule(UrlBase64BinaryStringEncoder.class);

    @Before
    public void configure() throws Exception
    {
        encoder = mocker.getComponentUnderTest();
    }
}
