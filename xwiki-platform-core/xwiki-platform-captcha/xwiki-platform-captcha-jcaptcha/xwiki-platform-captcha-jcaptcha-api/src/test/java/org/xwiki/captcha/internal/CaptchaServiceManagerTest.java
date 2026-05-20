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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.captcha.CaptchaException;
import org.xwiki.component.phase.InitializationException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate the behaviour of {@link CaptchaServiceManager}.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
class CaptchaServiceManagerTest
{
    private CaptchaServiceManager captchaServiceManager;

    @BeforeEach
    void setUp() throws InitializationException
    {
        captchaServiceManager = new CaptchaServiceManager();
        captchaServiceManager.initialize();
    }

    @Test
    void getCaptchaStoreIsNotNull()
    {
        assertNotNull(captchaServiceManager.getCaptchaStore());
    }

    @Test
    void getCaptchaServiceWithNonExistentClass()
    {
        CaptchaException exception = assertThrows(CaptchaException.class,
            () -> captchaServiceManager.getCaptchaService("com.example.NonExistentEngine"));
        assertTrue(exception.getMessage().contains("com.example.NonExistentEngine"));
    }

    @Test
    void getCaptchaServiceWithNonEngineClass()
    {
        CaptchaException exception = assertThrows(CaptchaException.class,
            () -> captchaServiceManager.getCaptchaService("java.lang.String"));
        assertTrue(exception.getMessage().contains("not a CaptchaEngine"));
    }
}
