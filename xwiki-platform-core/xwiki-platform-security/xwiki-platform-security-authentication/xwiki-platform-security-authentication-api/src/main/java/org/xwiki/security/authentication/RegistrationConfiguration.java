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
package org.xwiki.security.authentication;

import java.util.Set;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Role;

/**
 * Define the various configuration options for registration.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Role
public interface RegistrationConfiguration
{
    /**
     * Default minimum password length.
     */
    int DEFAULT_MINIMUM_PASSWORD_LENGTH = 6;

    /**
     * Represents the rules to apply for creating a new password.
     */
    enum PasswordRules
    {
        /**
         * When one lower case character is mandatory.
         */
        ONE_LOWER_CASE_CHARACTER(".*[a-z]+.*"),

        /**
         * When one upper case character is mandatory.
         */
        ONE_UPPER_CASE_CHARACTER(".*[A-Z]+.*"),

        /**
         * When one symbol character is mandatory.
         */
        ONE_SYMBOL_CHARACTER(".*[_\\W]+.*"),

        /**
         * When one number character is mandatory.
         */
        ONE_NUMBER_CHARACTER(".*[0-9]+.*");

        private final String regularExpression;

        PasswordRules(String expression)
        {
            this.regularExpression = expression;
        }

        /**
         * @return the pattern used by the rule to ensure it's respected.
         */
        public Pattern getPattern()
        {
            return Pattern.compile(this.regularExpression);
        }
    }

    /**
     * @return the minimum required length for a new password.
     */
    int getPasswordMinimumLength();

    /**
     * @return the set of rules to comply with for a creating a new password.
     */
    Set<PasswordRules> getPasswordRules();

    /**
     * @return {@code true} if a CAPTCHA should be solved for registration.
     */
    boolean isCaptchaRequired();

    /**
     * @return {@code true} if an email validation needs to be sent for registration.
     */
    boolean isEmailValidationRequired();

    /**
     * @return {@code true} if users don't have to click to login after registration.
     */
    boolean isAutoLoginEnabled();

    /**
     * @return {@code true} if there's a mechanism to login in single click after registration.
     */
    boolean isLoginEnabled();
}
