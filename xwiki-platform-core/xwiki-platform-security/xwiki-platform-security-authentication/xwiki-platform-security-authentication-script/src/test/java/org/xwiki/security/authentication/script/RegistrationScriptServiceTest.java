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
package org.xwiki.security.authentication.script;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.security.authentication.RegistrationConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RegistrationScriptService}.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@ComponentTest
class RegistrationScriptServiceTest
{
    @InjectMockComponents
    private RegistrationScriptService scriptService;

    @MockComponent
    private RegistrationConfiguration registrationConfiguration;

    @Test
    void getPasswordMinimumLength()
    {
        when(registrationConfiguration.getPasswordMinimumLength()).thenReturn(745);
        assertEquals(745, scriptService.getPasswordMinimumLength());
    }

    @Test
    void getPasswordRules()
    {
        when(registrationConfiguration.getPasswordRules()).thenReturn(Set.of(
            RegistrationConfiguration.PasswordRules.ONE_LOWER_CASE_CHARACTER,
            RegistrationConfiguration.PasswordRules.ONE_SYMBOL_CHARACTER));
        assertEquals(Set.of("ONE_LOWER_CASE_CHARACTER","ONE_SYMBOL_CHARACTER"),
            scriptService.getPasswordRules());
    }

    @Test
    void isCaptchaRequired()
    {
        when(registrationConfiguration.isCaptchaRequired()).thenReturn(true);
        assertTrue(scriptService.isCaptchaRequired());
        verify(registrationConfiguration).isCaptchaRequired();
    }

    @Test
    void isEmailValidationRequired()
    {
        when(registrationConfiguration.isEmailValidationRequired()).thenReturn(true);
        assertTrue(scriptService.isEmailValidationRequired());
        verify(registrationConfiguration).isEmailValidationRequired();
    }

    @Test
    void isAutoLoginEnabled()
    {
        when(registrationConfiguration.isAutoLoginEnabled()).thenReturn(true);
        assertTrue(scriptService.isAutoLoginEnabled());
        verify(registrationConfiguration).isAutoLoginEnabled();
    }

    @Test
    void isLoginEnabled()
    {
        when(registrationConfiguration.isLoginEnabled()).thenReturn(true);
        assertTrue(scriptService.isLoginEnabled());
        verify(registrationConfiguration).isLoginEnabled();
    }
}