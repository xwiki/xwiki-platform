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

import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.captcha.Captcha;
import org.xwiki.captcha.CaptchaConfiguration;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Register xwiki action.
 *
 * @version $Id$
 */
public class RegisterAction extends XWikiAction
{
    /** Name of the corresponding template and URL parameter. */
    private static final String REGISTER = "register";

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterAction.class);

    /** Space where the registration config and class are stored. */
    private static final String WIKI_SPACE = "XWiki";

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        String register = request.getParameter(REGISTER);
        if (register != null && register.equals("1")) {
            // CSRF prevention
            if (!csrfTokenCheck(context)) {
                return false;
            }
            // Let's verify that the user submitted the right CAPTCHA (if required).
            if (!verifyCaptcha(context, xwiki)) {
                return false;
            }

            int useemail = xwiki.getXWikiPreferenceAsInt("use_email_verification", 0, context);
            int result;
            if (useemail == 1) {
                result = xwiki.createUser(true, "edit", context);
            } else {
                result = xwiki.createUser(context);
            }
            getCurrentScriptContext().setAttribute("reg", Integer.valueOf(result), ScriptContext.ENGINE_SCOPE);

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
