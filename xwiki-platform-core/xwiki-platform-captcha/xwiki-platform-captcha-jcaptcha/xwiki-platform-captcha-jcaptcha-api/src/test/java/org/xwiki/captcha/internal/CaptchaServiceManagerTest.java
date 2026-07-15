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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.captcha.CaptchaException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Validate the behaviour of {@link CaptchaServiceManager}.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@ComponentTest
class CaptchaServiceManagerTest
{
    @InjectMockComponents
    private CaptchaServiceManager captchaServiceManager;

    @MockComponent
    @Named("jcaptcha")
    private ConfigurationSource configuration;

    @Test
    void getCaptchaServiceWithNonExistentClass()
    {
        when(configuration.getProperty(JCaptchaCaptcha.ENGINE, JCaptchaCaptcha.DEFAULT_ENGINE))
            .thenReturn("com.example.NonExistentEngine");
        CaptchaException exception = assertThrows(CaptchaException.class,
            () -> captchaServiceManager.getCaptchaService());
        assertEquals("Invalid engine [com.example.NonExistentEngine]", exception.getMessage());
    }

    @Test
    void getCaptchaServiceWithNonEngineClass()
    {
        when(configuration.getProperty(JCaptchaCaptcha.ENGINE, JCaptchaCaptcha.DEFAULT_ENGINE))
            .thenReturn("java.lang.String");
        CaptchaException exception = assertThrows(CaptchaException.class,
            () -> captchaServiceManager.getCaptchaService());
        assertEquals("Invalid engine [java.lang.String]: not a CaptchaEngine", exception.getMessage());
    }
}

