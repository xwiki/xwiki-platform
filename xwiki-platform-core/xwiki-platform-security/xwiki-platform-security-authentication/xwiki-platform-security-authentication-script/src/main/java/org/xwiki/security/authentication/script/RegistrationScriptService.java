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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authentication.RegistrationConfiguration;
import org.xwiki.security.script.SecurityScriptService;

/**
 * Script service for accessing configuration related to registration.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Component
@Named(SecurityScriptService.ROLEHINT + '.' + RegistrationScriptService.ID)
@Singleton
public class RegistrationScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ID = "registration";

    @Inject
    private RegistrationConfiguration registrationConfiguration;

    /**
     * @return the minimum required length for a new password.
     */
    public int getPasswordMinimumLength()
    {
        return this.registrationConfiguration.getPasswordMinimumLength();
    }

    /**
     * The rules are returned as {@code String} and not as
     * {@link org.xwiki.security.authentication.RegistrationConfiguration.PasswordRules} because manipulating a
     * {@link Set} of {@link Enum} in velocity is not handy.
     *
     * @return the set of rules to comply with for a creating a new password.
     */
    public Set<String> getPasswordRules()
    {
        return this.registrationConfiguration.getPasswordRules()
            .stream().map(RegistrationConfiguration.PasswordRules::name)
            .collect(Collectors.toSet());
    }

    /**
     * @return {@code true} if a CAPTCHA should be solved for registration.
     */
    public boolean isCaptchaRequired()
    {
        return this.registrationConfiguration.isCaptchaRequired();
    }

    /**
     * @return {@code true} if an email validation needs to be sent for registration.
     */
    public boolean isEmailValidationRequired()
    {
        return this.registrationConfiguration.isEmailValidationRequired();
    }

    /**
     * @return {@code true} if users don't have to click to login after registration.
     */
    public boolean isAutoLoginEnabled()
    {
        return this.registrationConfiguration.isAutoLoginEnabled();
    }

    /**
     * @return {@code true} if there's a mechanism to login in single click after registration.
     */
    public boolean isLoginEnabled()
    {
        return this.registrationConfiguration.isLoginEnabled();
    }
}
