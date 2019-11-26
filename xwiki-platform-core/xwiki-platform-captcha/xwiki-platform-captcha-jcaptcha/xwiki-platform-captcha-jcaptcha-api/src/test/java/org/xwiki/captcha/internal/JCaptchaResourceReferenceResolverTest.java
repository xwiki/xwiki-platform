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
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validate the behaviour of {@link JCaptchaResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 11.10
 */
@ComponentTest
public class JCaptchaResourceReferenceResolverTest
{
    @InjectMockComponents
    private JCaptchaResourceReferenceResolver jCaptchaResourceReferenceResolver;

    @Test
    public void resolve() throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        ExtendedURL extendedURL =
            new ExtendedURL(Arrays.asList("foo", "bar"), Collections.singletonMap("customValue", Arrays.asList("baz")));
        JCaptchaResourceReference expectedReference = new JCaptchaResourceReference("foo", "bar");
        expectedReference.addParameter("customValue", "baz");

        assertEquals(expectedReference,
            jCaptchaResourceReferenceResolver.resolve(extendedURL,
                JCaptchaResourceReference.TYPE, Collections.emptyMap()));
    }

    @Test
    public void resolveWrongUrl()
    {
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("foo"), Collections.emptyMap());

        CreateResourceReferenceException createResourceReferenceException =
            assertThrows(CreateResourceReferenceException.class, () -> {
                jCaptchaResourceReferenceResolver
                    .resolve(extendedURL, JCaptchaResourceReference.TYPE, Collections.emptyMap());
            });

        assertEquals("Invalid JCaptcha URL format [/foo]", createResourceReferenceException.getMessage());
    }
}
