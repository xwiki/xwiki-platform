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
package org.xwiki.captcha.internal.script;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.captcha.internal.JCaptchaResourceReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate the behaviour of {@link JCaptchaInternalScriptService}.
 *
 * @since 11.10
 * @version $Id$
 */
@ComponentTest
public class JCaptchaInternalScriptServiceTest
{
    @InjectMockComponents
    private JCaptchaInternalScriptService scriptService;

    @MockComponent
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> serializer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    public void getURL() throws UnsupportedResourceReferenceException, SerializeResourceReferenceException
    {
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(serializer.serialize(any())).thenReturn(extendedURL);
        this.scriptService.getURL("myType", "myEngine", Collections.singletonMap("customParam", "customValue"));

        JCaptchaResourceReference jCaptchaResourceReference = new JCaptchaResourceReference("myType", "myEngine");
        jCaptchaResourceReference.addParameter("customParam", "customValue");

        verify(serializer, times(1)).serialize(jCaptchaResourceReference);
        verify(extendedURL, times(1)).serialize();
    }

    @Test
    public void getURLSerializerError() throws UnsupportedResourceReferenceException, SerializeResourceReferenceException
    {
        when(serializer.serialize(any())).thenThrow(new UnsupportedResourceReferenceException("customMessage"));
        assertNull(this.scriptService.getURL("myType", "myEngine", null));

        JCaptchaResourceReference jCaptchaResourceReference = new JCaptchaResourceReference("myType", "myEngine");

        verify(serializer, times(1)).serialize(jCaptchaResourceReference);
        assertEquals("Error while serializing JCaptcha URL for type [myType], engine = [myEngine]."
                + " Root cause = [UnsupportedResourceReferenceException: customMessage]",
            logCapture.getMessage(0));
    }

    @Test
    public void getURLNullParams()
    {
        assertNull(this.scriptService.getURL(null, "foo", null));
        assertNull(this.scriptService.getURL("foo", null, null));
    }
}
