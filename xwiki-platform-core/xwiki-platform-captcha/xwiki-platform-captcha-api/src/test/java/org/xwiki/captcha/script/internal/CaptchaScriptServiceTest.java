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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.captcha.Captcha;
import org.xwiki.captcha.CaptchaConfiguration;
import org.xwiki.captcha.script.CaptchaScriptService;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
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
        // An instance map of mock instances.
        Map<String, Object> instanceMap = new HashMap<>();
        instanceMap.put("captcha1", mock(Captcha.class));
        instanceMap.put("captcha2", mock(Captcha.class));
        instanceMap.put("captcha3", mock(Captcha.class));

        // Mock the "context" CM call to return the mocked instances map, to make sure we're explicitly using the
        // correct CM (in this case, not the test's default CM).
        ComponentManager mockContextComponentManager = componentManager.getInstance(ComponentManager.class, "context");
        when(mockContextComponentManager.getInstanceMap(Captcha.class)).thenReturn(instanceMap);

        assertEquals(Arrays.asList("captcha1", "captcha2", "captcha3"), captchaScriptService.getCaptchaNames());

        // Double check that we've not used the default CM to mock the instances.
        assertNotEquals(componentManager.getInstanceMap(Captcha.class), instanceMap);
    }
}
