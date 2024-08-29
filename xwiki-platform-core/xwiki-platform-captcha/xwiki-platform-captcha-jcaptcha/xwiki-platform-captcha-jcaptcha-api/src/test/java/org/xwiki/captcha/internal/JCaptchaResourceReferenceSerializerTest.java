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
package org.xwiki.captcha.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate the behaviour of {@link JCaptchaResourceReferenceSerializer}.
 *
 * @version $Id$
 * @since 11.10
 */
@ComponentTest
class JCaptchaResourceReferenceSerializerTest
{
    @InjectMockComponents
    private JCaptchaResourceReferenceSerializer jCaptchaResourceReferenceSerializer;

    @MockComponent
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> extendedURLNormalizer;

    @BeforeEach
    void setup()
    {
        when(extendedURLNormalizer.normalize(any())).then(returnsFirstArg());
    }

    @Test
    void serialize() throws UnsupportedResourceReferenceException, SerializeResourceReferenceException
    {
        JCaptchaResourceReference jCaptchaResourceReference = new JCaptchaResourceReference("fooType", "barEngine");
        ExtendedURL extendedURL = jCaptchaResourceReferenceSerializer.serialize(jCaptchaResourceReference);

        verify(extendedURLNormalizer, times(1)).normalize(any());
        List<String> segments = extendedURL.getSegments();
        assertEquals(3, segments.size());
        assertEquals("jcaptcha", segments.get(0));
        assertEquals("fooType", segments.get(1));
        assertEquals("barEngine", segments.get(2));
        assertTrue(extendedURL.getParameters().isEmpty());
    }

    @Test
    void serializeWithParameters()
        throws UnsupportedResourceReferenceException, SerializeResourceReferenceException
    {
        JCaptchaResourceReference jCaptchaResourceReference = new JCaptchaResourceReference("bar", "foo");
        jCaptchaResourceReference.addParameter("myCustomParam", "someValue");
        ExtendedURL extendedURL = jCaptchaResourceReferenceSerializer.serialize(jCaptchaResourceReference);

        verify(extendedURLNormalizer, times(1)).normalize(any());
        List<String> segments = extendedURL.getSegments();
        assertEquals(3, segments.size());
        assertEquals("jcaptcha", segments.get(0));
        assertEquals("bar", segments.get(1));
        assertEquals("foo", segments.get(2));

        Map<String, List<String>> parameters = extendedURL.getParameters();
        assertEquals(1, parameters.size());
        assertTrue(parameters.containsKey("myCustomParam"));
        assertEquals(Arrays.asList("someValue"), parameters.get("myCustomParam"));
    }
}
