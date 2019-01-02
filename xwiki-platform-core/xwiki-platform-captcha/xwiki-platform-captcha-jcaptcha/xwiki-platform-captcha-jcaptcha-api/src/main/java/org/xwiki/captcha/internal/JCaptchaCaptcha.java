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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.captcha.AbstractCaptcha;
import org.xwiki.captcha.CaptchaException;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.octo.captcha.service.CaptchaService;
import com.octo.captcha.service.CaptchaServiceException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * JCaptcha based CATPCHA implementation.
 *
 * @version $Id$
 * @since 10.8RC1
 */
@Component
@Named("jcaptcha")
@Singleton
public class JCaptchaCaptcha extends AbstractCaptcha
{
    private static final List<String> JCAPTCHA_SPACE_LIST = Arrays.asList(XWiki.SYSTEM_SPACE, "Captcha", "JCaptcha");

    private static final LocalDocumentReference CONFIGURATION_DOCUMENT_REFERENCE =
        new LocalDocumentReference(JCAPTCHA_SPACE_LIST, "Configuration");

    private static final LocalDocumentReference CONFIGURATION_CLASS_REFERENCE =
        new LocalDocumentReference(JCAPTCHA_SPACE_LIST, "ConfigurationClass");

    private static final LocalDocumentReference DISPLAYER_DOCUMENT_REFERENCE =
        new LocalDocumentReference(JCAPTCHA_SPACE_LIST, "Displayer");

    private static final Map<String, Object> DEFAULT_PARAMETERS = new HashMap<>();
    {
        DEFAULT_PARAMETERS.put("type", "image");
        DEFAULT_PARAMETERS.put("engine", "com.octo.captcha.engine.image.gimpy.DefaultGimpyEngine");
    }

    @Inject
    private CaptchaServiceManager captchaServiceManager;

    @Override
    protected LocalDocumentReference getDisplayerDocumentReference()
    {
        return DISPLAYER_DOCUMENT_REFERENCE;
    }

    @Override
    protected LocalDocumentReference getConfigurationDocumentReference()
    {
        return CONFIGURATION_DOCUMENT_REFERENCE;
    }

    @Override
    protected LocalDocumentReference getConfigurationClassReference()
    {
        return CONFIGURATION_CLASS_REFERENCE;
    }

    @Override
    protected Map<String, Object> getDefaultParameters()
    {
        return DEFAULT_PARAMETERS;
    }

    @Override
    protected boolean validate(Map<String, Object> captchaParameters) throws CaptchaException, CaptchaServiceException
    {
        XWikiRequest request = getContext().getRequest();

        // Use the parameters to instantiate the correct CAPTCHA factory.
        CaptchaService captchaService = captchaServiceManager.getCaptchaService(captchaParameters);

        // Use the Session ID as CAPTCHA ID. The consequence is that one user can have only 1 CAPTCHA at a time,
        // i.e. getting a new CAPTCHA in one tab will invalidate the CAPTCHA in the previous tabs.
        String id = request.getSession().getId();

        String answer = request.getParameter("captchaAnswer");

        boolean result = captchaService.validateResponseForID(id, answer);
        return result;
    }
}
