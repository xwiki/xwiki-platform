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
package org.xwiki.captcha.internal.velocity;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.xwiki.captcha.CaptchaVerifier;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Provides access to the classes implementing Captcha.
 *
 * @version $Id$
 * @since 2.2M2
 */
public class VelocityCaptchaService
{
    /** The configuration key for the.  */
    private static final String IS_CAPTCHA_ENABLED_CFG_KEY = "captcha.internal.velocity.VelocityCaptchaService.enabled";

    /** A Map of all captchas by their names. */
    private Map<String, CaptchaVerifier> captchas;

    /** A ConfigurationSource in which to look for whether the captcha should be enabled. */
    private ConfigurationSource configuration;

    /**
     * The Constructor.
     *
     * @param captchas A Map of registered components which implement Captcha by their names
     * @param configuration A ConfigurationSource in which to look for whether the captcha should be enabled
     */
    public VelocityCaptchaService(Map<String, CaptchaVerifier> captchas, ConfigurationSource configuration)
    {
        this.captchas = captchas;
        this.configuration = configuration;
    }

    /**
     * Get a {@link org.xwiki.captcha.CaptchaVerifier} from the service.
     *
     * @param captchaName The name of the type of captcha, use {@link #listCaptchaNames()} for a list
     * @return A captcha object which can be used to check a specific answer for a given challange
     * @throws CaptchaVerifierNotFoundException If the named VaptchaVerifier could not be found.
     */
    public CaptchaVerifier getCaptchaVerifier(String captchaName) throws CaptchaVerifierNotFoundException
    {
        if (captchas.get(captchaName) == null) {
            throw new CaptchaVerifierNotFoundException(captchaName);
        }
        return new VelocityCaptchaVerifier(captchas.get(captchaName));
    }

    /** @return a List of the names of all registered classes implementing {@link org.xwiki.captcha.CaptchaVerifier}. */
    public List<String> listCaptchaNames()
    {
        return new ArrayList(captchas.keySet());
    }

    /**
     * If this is false, the captcha system will still work.
     * It is for velocity scripts to determine whether captchas are needed.
     *
     * @return Is the captcha service enabled in the configuration.
     */
    public boolean isEnabled()
    {
        return configuration.getProperty(IS_CAPTCHA_ENABLED_CFG_KEY, Boolean.TRUE).booleanValue();
    }
}
