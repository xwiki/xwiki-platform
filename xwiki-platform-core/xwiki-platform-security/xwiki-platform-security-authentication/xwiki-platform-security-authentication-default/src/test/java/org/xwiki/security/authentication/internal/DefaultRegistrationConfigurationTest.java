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
package org.xwiki.security.authentication.internal;

import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.security.authentication.RegistrationConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.xwiki.security.authentication.RegistrationConfiguration.DEFAULT_MINIMUM_PASSWORD_LENGTH;

/**
 * Tests for {@link DefaultRegistrationConfiguration}.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@ComponentTest
class DefaultRegistrationConfigurationTest
{
    @InjectMockComponents
    private DefaultRegistrationConfiguration defaultRegistrationConfiguration;

    @MockComponent
    @Named("registration")
    private ConfigurationSource configuration;

    @MockComponent
    @Named("wiki")
    private ConfigurationSource wikiPreferencesConfigurationSource;

    @Test
    void getPasswordMinimumLength()
    {
        when(configuration.getProperty("passwordLength", DEFAULT_MINIMUM_PASSWORD_LENGTH)).thenReturn(5);
        assertEquals(5, this.defaultRegistrationConfiguration.getPasswordMinimumLength());

        when(configuration.getProperty("passwordLength", DEFAULT_MINIMUM_PASSWORD_LENGTH)).thenReturn(-5);
        assertEquals(DEFAULT_MINIMUM_PASSWORD_LENGTH, this.defaultRegistrationConfiguration.getPasswordMinimumLength());
    }

    @Test
    void getPasswordRules()
    {
        when(configuration.getProperty("passwordRuleOneLowerCaseEnabled", 0)).thenReturn(1);
        when(configuration.getProperty("passwordRuleOneNumberEnabled", 0)).thenReturn(1);
        when(configuration.getProperty("passwordRuleOneSymbolEnabled", 0)).thenReturn(1);
        when(configuration.getProperty("passwordRuleOneUpperCaseEnabled", 0)).thenReturn(1);
        assertEquals(Set.of(
            RegistrationConfiguration.PasswordRules.ONE_LOWER_CASE_CHARACTER,
            RegistrationConfiguration.PasswordRules.ONE_NUMBER_CHARACTER,
            RegistrationConfiguration.PasswordRules.ONE_UPPER_CASE_CHARACTER,
            RegistrationConfiguration.PasswordRules.ONE_SYMBOL_CHARACTER
        ), this.defaultRegistrationConfiguration.getPasswordRules());

        when(configuration.getProperty("passwordRuleOneLowerCaseEnabled", 0)).thenReturn(0);
        when(configuration.getProperty("passwordRuleOneNumberEnabled", 0)).thenReturn(0);
        when(configuration.getProperty("passwordRuleOneSymbolEnabled", 0)).thenReturn(0);
        when(configuration.getProperty("passwordRuleOneUpperCaseEnabled", 0)).thenReturn(0);

        assertEquals(Set.of(), this.defaultRegistrationConfiguration.getPasswordRules());

        when(configuration.getProperty("passwordRuleOneLowerCaseEnabled", 0)).thenReturn(1);
        when(configuration.getProperty("passwordRuleOneUpperCaseEnabled", 0)).thenReturn(1);
        when(configuration.getProperty("passwordRuleOneNumberEnabled", 0)).thenReturn(0);
        when(configuration.getProperty("passwordRuleOneSymbolEnabled", 0)).thenReturn(0);

        assertEquals(Set.of(
            RegistrationConfiguration.PasswordRules.ONE_LOWER_CASE_CHARACTER,
            RegistrationConfiguration.PasswordRules.ONE_UPPER_CASE_CHARACTER
        ), this.defaultRegistrationConfiguration.getPasswordRules());

        when(configuration.getProperty("passwordRuleOneLowerCaseEnabled", 0)).thenReturn(0);
        when(configuration.getProperty("passwordRuleOneUpperCaseEnabled", 0)).thenReturn(0);
        when(configuration.getProperty("passwordRuleOneNumberEnabled", 0)).thenReturn(1);
        when(configuration.getProperty("passwordRuleOneSymbolEnabled", 0)).thenReturn(1);

        assertEquals(Set.of(
            RegistrationConfiguration.PasswordRules.ONE_NUMBER_CHARACTER,
            RegistrationConfiguration.PasswordRules.ONE_SYMBOL_CHARACTER
        ), this.defaultRegistrationConfiguration.getPasswordRules());
    }

    @Test
    void isEmailValidationRequired()
    {
        when(wikiPreferencesConfigurationSource.getProperty("use_email_verification", 0)).thenReturn(1);
        assertTrue(this.defaultRegistrationConfiguration.isEmailValidationRequired());

        when(wikiPreferencesConfigurationSource.getProperty("use_email_verification", 0)).thenReturn(0);
        assertFalse(this.defaultRegistrationConfiguration.isEmailValidationRequired());

        when(wikiPreferencesConfigurationSource.getProperty("use_email_verification", 0)).thenReturn(-8);
        assertFalse(this.defaultRegistrationConfiguration.isEmailValidationRequired());
    }

    @Test
    void isCaptchaRequired()
    {
        when(configuration.getProperty("requireCaptcha", 0)).thenReturn(1);
        assertTrue(this.defaultRegistrationConfiguration.isCaptchaRequired());

        when(configuration.getProperty("requireCaptcha", 0)).thenReturn(0);
        assertFalse(this.defaultRegistrationConfiguration.isCaptchaRequired());

        when(configuration.getProperty("requireCaptcha", 0)).thenReturn(11);
        assertFalse(this.defaultRegistrationConfiguration.isCaptchaRequired());
    }
}