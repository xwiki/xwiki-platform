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

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.security.authentication.RegistrationConfiguration;

/**
 * Default implementation of {@link RegistrationConfiguration} based on the document {@code XWiki.RegistrationConfiguration}.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Component
@Singleton
public class DefaultRegistrationConfiguration implements RegistrationConfiguration
{
    @Inject
    @Named("registration")
    private ConfigurationSource configuration;

    @Inject
    @Named("wiki")
    private ConfigurationSource wikiPreferencesConfigurationSource;

    @Override
    public int getPasswordMinimumLength()
    {
        int length = configuration.getProperty("passwordLength", DEFAULT_MINIMUM_PASSWORD_LENGTH);
        return (length > 1) ? length : DEFAULT_MINIMUM_PASSWORD_LENGTH;
    }

    @Override
    public Set<PasswordRules> getPasswordRules()
    {
        Set<PasswordRules> result = new HashSet<>();
        if (configuration.getProperty("passwordRuleOneLowerCaseEnabled", 0) == 1) {
            result.add(PasswordRules.ONE_LOWER_CASE_CHARACTER);
        }
        if (configuration.getProperty("passwordRuleOneNumberEnabled", 0) == 1) {
            result.add(PasswordRules.ONE_NUMBER_CHARACTER);
        }
        if (configuration.getProperty("passwordRuleOneSymbolEnabled", 0) == 1) {
            result.add(PasswordRules.ONE_SYMBOL_CHARACTER);
        }
        if (configuration.getProperty("passwordRuleOneUpperCaseEnabled", 0) == 1) {
            result.add(PasswordRules.ONE_UPPER_CASE_CHARACTER);
        }
        return result;
    }

    @Override
    public boolean isCaptchaRequired()
    {
        return configuration.getProperty("requireCaptcha", 0) == 1;
    }

    @Override
    public boolean isEmailValidationRequired()
    {
        return wikiPreferencesConfigurationSource.getProperty("use_email_verification", 0) == 1;
    }

    @Override
    public boolean isAutoLoginEnabled()
    {
        return configuration.getProperty("loginButton_autoLogin_enabled", 0) == 1;
    }

    @Override
    public boolean isLoginEnabled()
    {
        return configuration.getProperty("loginButton_enabled", 1) == 1;
    }
}
