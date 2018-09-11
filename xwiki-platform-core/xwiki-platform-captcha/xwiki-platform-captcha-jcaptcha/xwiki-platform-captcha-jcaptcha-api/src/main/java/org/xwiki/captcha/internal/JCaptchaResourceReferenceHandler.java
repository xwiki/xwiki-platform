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

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;

import com.octo.captcha.component.sound.wordtosound.AbstractFreeTTSWordToSound;
import com.octo.captcha.module.web.image.ImageToJpegHelper;
import com.octo.captcha.module.web.sound.SoundToWavHelper;
import com.octo.captcha.service.CaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;
import com.octo.captcha.service.sound.SoundCaptchaService;
import com.sun.star.lang.IllegalArgumentException;
import com.xpn.xwiki.XWikiContext;

/**
 * URL Resource Handler for exposing the generated CAPTCHA resources (image/sound/text).
 *
 * @version $Id$
 * @since 10.8RC1
 */
@Component
@Named("jcaptcha")
@Singleton
public class JCaptchaResourceReferenceHandler extends AbstractResourceReferenceHandler<EntityResourceAction>
{
    /**
     *
     */
    private static final String FREETTS_PROPERTIES_KEY = "freetts.voices";

    private static final EntityResourceAction ACTION = new EntityResourceAction("jcaptcha");

    private static final String TYPE_IMAGE = "image";

    private static final String TYPE_SOUND = "sound";

    private static final String TYPE_TEXT = "text";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private CaptchaServiceManager captchaServiceManager;

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return Arrays.asList(ACTION);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        try {
            XWikiContext context = contextProvider.get();

            HttpServletRequest request = context.getRequest();
            HttpServletResponse response = context.getResponse();

            Map<String, Object> captchaParameters = readCaptchaParameters(reference);

            // This system property configures and enables the the available voices. The FreeTTSWordToSound
            // implementation is broken (i.e. relevant code commented out apparently by mistake) and fails to register
            // them, so we have to do it. "kevin16" seems to be the default used by the
            // "com.octo.captcha.engine.sound.speller.SpellerSoundCaptchaEngine" engine, so we'll use it as default,
            // when no other value is provided through the system property.
            if (System.getProperty(FREETTS_PROPERTIES_KEY) == null) {
                System.setProperty(FREETTS_PROPERTIES_KEY, AbstractFreeTTSWordToSound.defaultVoicePackage);
            }

            CaptchaService captchaService = captchaServiceManager.getCaptchaService(captchaParameters);

            String type = (String) captchaParameters.get("type");
            String id = request.getSession().getId();
            Locale locale = request.getLocale();

            if (type == null) {
                throw new IllegalArgumentException("Missing [type] parameter");
            }
            switch (type) {
                case TYPE_IMAGE:
                    ImageToJpegHelper.flushNewCaptchaToResponse(request, response, null,
                        (ImageCaptchaService) captchaService, id, locale);
                    break;
                case TYPE_SOUND:
                    SoundToWavHelper.flushNewCaptchaToResponse(request, response, null,
                        (SoundCaptchaService) captchaService, id, locale);
                    break;
                case TYPE_TEXT:
                    response.setContentType("text/plain");
                    response.setCharacterEncoding("UTF-8");

                    // Disable any type of caching for this resource. The other types already handle this.
                    response.setHeader("Cache-Control", "no-store");
                    response.setHeader("Pragma", "no-cache");
                    response.setDateHeader("Expires", 0);

                    // Write the challenge to the response.
                    String challenge = (String) captchaService.getChallengeForID(id, locale);
                    try (OutputStream responseOutput = response.getOutputStream()) {
                        IOUtils.write(challenge, response.getOutputStream(), Charset.defaultCharset());
                    }

                    break;
                default:
                    throw new UnsupportedOperationException(
                        String.format("Unsupported value [%s] for the [type] parameter", type));
            }
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to handle resource [%s]", ACTION.getActionName()), e);
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    /**
     * @param reference the requested resource reference
     * @return the captchaParameters extracted from the request
     */
    private Map<String, Object> readCaptchaParameters(ResourceReference reference)
    {
        Map<String, Object> captchaParameters = new HashMap<>();

        // Convert the resource parameters (Map<String, List<String>>) to captcha parameters (Map<String, Object>).
        if (reference.getParameters() != null && reference.getParameters().size() > 0) {
            reference.getParameters().forEach((key, value) -> {
                if (value.size() == 1) {
                    captchaParameters.put(key, value.get(0));
                } else if (value.size() == 0) {
                    captchaParameters.remove(key);
                } else {
                    captchaParameters.put(key, value);
                }
            });
        }
        return captchaParameters;
    }
}
