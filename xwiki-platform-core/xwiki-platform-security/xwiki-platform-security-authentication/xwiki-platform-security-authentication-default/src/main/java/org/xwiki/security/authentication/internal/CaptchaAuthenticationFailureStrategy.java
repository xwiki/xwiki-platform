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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.captcha.Captcha;
import org.xwiki.captcha.CaptchaConfiguration;
import org.xwiki.captcha.CaptchaException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.security.authentication.AuthenticationFailureStrategy;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Captcha Strategy for repeated authentication failures. The main idea of this strategy is to add a captcha form field
 * in the login form and to ask user to fill it for validating their authentication.
 *
 * @version $Id$
 * @since 11.6RC1
 */
@Component
@Named("captcha")
@Singleton
public class CaptchaAuthenticationFailureStrategy implements AuthenticationFailureStrategy
{
    /**
     * Exception message thrown by jCaptcha library when no captcha is registered for the session id.
     */
    private static final String UNEXISTING_CAPTCHA_EXCEPTION =
        "Invalid ID, could not validate unexisting or already " + "validated captcha";

    @Inject
    private CaptchaConfiguration captchaConfiguration;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ContextualLocalizationManager contextLocalization;

    @Inject
    private Logger logger;

    @Override
    public String getErrorMessage(String username)
    {
        return contextLocalization.getTranslationPlain("security.authentication.strategy.captcha.errorMessage");
    }

    private Captcha getCaptcha() throws ComponentLookupException
    {
        return this.componentManager.getInstance(Captcha.class, this.captchaConfiguration.getDefaultName());
    }

    @Override
    public String getForm(String username)
    {
        try {
            return getCaptcha().display();
        } catch (CaptchaException | ComponentLookupException e) {
            logger.error("Error while displaying the CAPTCHA.", e);
            return "";
        }
    }

    @Override
    public boolean validateForm(String username, HttpServletRequest request)
    {
        try {
            Map<String, Object> map = new HashMap<>(request.getParameterMap());
            map.putAll(request.getParameterMap());
            return getCaptcha().isValid(map);
        } catch (CaptchaException | ComponentLookupException e) {
            // We skip the error log if we did not manage to find the captcha: this might indeed happen in case
            // an user fails to authenticate without using the form.
            if (UNEXISTING_CAPTCHA_EXCEPTION.equals(ExceptionUtils.getRootCause(e).getMessage())) {
                logger.debug("Unexisting captcha exception", e);
            } else {
                logger.error("Error while validating the CAPTCHA.", e);
            }
            return false;
        }
    }

    @Override
    public void notify(String username)
    {
        // do nothing
    }
}
