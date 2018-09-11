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
package org.xwiki.captcha.script.internal;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.captcha.Captcha;
import org.xwiki.captcha.CaptchaConfiguration;
import org.xwiki.captcha.script.CaptchaScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Test for {@link CaptchaScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
public class CaptchaScriptServiceTest
{
    @MockComponent
    private CaptchaConfiguration configuration;

    @InjectMockComponents
    private CaptchaScriptService captchaScriptService;

    @Test
    public void register(MockitoComponentManager componentManager) throws Exception
    {
        ScriptService service = componentManager.getInstance(ScriptService.class, "captcha");

        assertNotNull(service);
    }

    @Test
    public void getDefaultName()
    {
        when(configuration.getDefaultName()).thenReturn("SomeDefaultCaptchaName");

        assertEquals("SomeDefaultCaptchaName", captchaScriptService.getDefaultCaptchaName());
    }

    @Test
    public void getCaptchaNames(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerMockComponent(Captcha.class, "captcha1");
        componentManager.registerMockComponent(Captcha.class, "captcha2");
        componentManager.registerMockComponent(Captcha.class, "captcha3");

        assertEquals(Arrays.asList("captcha1", "captcha2", "captcha3"), captchaScriptService.getCaptchaNames());
    }
}
