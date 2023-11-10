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
package com.xpn.xwiki.web;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.captcha.Captcha;
import org.xwiki.captcha.CaptchaConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authentication.RegistrationConfiguration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Register xwiki action.
 *
 * @version $Id$
 */
@Component
@Named("register")
@Singleton
public class RegisterAction extends XWikiAction
{
    /** Name of the corresponding template and URL parameter. */
    private static final String REGISTER = "register";

    /**
     * Context variable used to store the state in case of problem.
     */
    private static final String REG_CONSTANT = "reg";

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterAction.class);

    /** Space where the registration config and class are stored. */
    private static final String WIKI_SPACE = "XWiki";

    /** Allowed templates for this action. */
    private static final List<String> ALLOWED_TEMPLATES = Arrays.asList(REGISTER, "registerinline");

    @Inject
    private RegistrationConfiguration registrationConfiguration;

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        // Limit template overrides with xpage to allowed templates.
        if (!ALLOWED_TEMPLATES.contains(Utils.getPage(context.getRequest(), REGISTER))) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                String.format("Forbidden template override with 'xpage' in [%s] action.", REGISTER));
        }

        String register = request.getParameter(REGISTER);
        if (register != null && register.equals("1")) {
            // CSRF prevention
            if (!csrfTokenCheck(context)) {
                return false;
            }
            int result;
            // Let's verify that the user submitted the right CAPTCHA (if required).
            if (!verifyCaptcha(context, xwiki)) {
                result = -9;
            } else {
                if (this.registrationConfiguration.isEmailValidationRequired()) {
                    result = xwiki.createUser(true, "edit", context);
                } else {
                    result = xwiki.createUser(context);
                }
            }
            getCurrentScriptContext().setAttribute(REG_CONSTANT, Integer.valueOf(result), ScriptContext.ENGINE_SCOPE);

            // Redirect if a redirection parameter is passed.
            String redirect = Utils.getRedirect(request, null);
            if (redirect == null) {
                return true;
            } else {
                sendRedirect(response, redirect);
                return false;
            }
        }

        return true;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return REGISTER;
    }

    /**
     * Verifies the user CAPTCHA answer (if required).
     *
     * @param context Current context
     * @param xwiki Current wiki
     * @return true If the user submitted the correct answer or if no CAPTCHA is required
     * @throws XWikiException exception
     */
    private boolean verifyCaptcha(XWikiContext context, XWiki xwiki) throws XWikiException
    {
        // No verification if the current user has programming rights.
        if (xwiki.getRightService().hasProgrammingRights(context)) {
            return true;
        }

        // The document where the "requirecaptcha" parameter is stored.
        DocumentReference configRef = new DocumentReference(context.getWikiId(), WIKI_SPACE, "RegistrationConfig");
        DocumentReference classReference = new DocumentReference(context.getWikiId(), WIKI_SPACE, "Registration");
        XWikiDocument configDoc = xwiki.getDocument(configRef, context);
        // Retrieve the captcha configuration.
        int requireCaptcha = configDoc.getIntValue(classReference, "requireCaptcha");

        if (requireCaptcha == 1) {
            CaptchaConfiguration captchaConfiguration =
                Utils.getComponent(org.xwiki.captcha.CaptchaConfiguration.class);
            String defaultCaptchaName = captchaConfiguration.getDefaultName();
            try {
                // Use the currently configured default CAPTCHA implementation.
                Captcha captcha = Utils.getComponent(org.xwiki.captcha.Captcha.class, defaultCaptchaName);

                if (!captcha.isValid()) {
                    LOGGER.warn("Incorrect CAPTCHA answer");
                    return false;
                }
            } catch (Exception e) {
                LOGGER.warn("Cannot verify answer for CAPTCHA of type [{}]: {}", defaultCaptchaName,
                    ExceptionUtils.getRootCauseMessage(e));
                return false;
            }
        }

        return true;
    }
}
