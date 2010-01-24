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

import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.captcha.CaptchaVerifier;
import org.xwiki.velocity.VelocityContextInitializer;
import org.xwiki.configuration.ConfigurationSource;

import org.apache.velocity.VelocityContext;

/**
 * Loads VelocityCaptchaService into the Velocity context.
 * 
 * @version $Id$
 * @since 2.2M2
 */
@Component("captchaservice")
public class CaptchaVelocityContextInitializer implements VelocityContextInitializer
{
    /** The key to use for the captcha in the velocity context. */
    public static final String VELOCITY_CONTEXT_KEY = "captchaservice";

    /** A Map of all captchas by their hint. */
    @Requirement(role = CaptchaVerifier.class)
    private Map<String, CaptchaVerifier> captchas;

    /** A ConfigurationSource in which to look for whether the captcha should be enabled. */
    @Requirement
    private ConfigurationSource configuration;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.velocity.VelocityContextInitializer#initialize(VelocityContext)
     */
    public void initialize(VelocityContext context)
    {
        context.put(VELOCITY_CONTEXT_KEY, new VelocityCaptchaService(captchas, configuration));
    }
}
