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
package org.xwiki.captcha;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Describes a CAPTCHA that is able to display a challenge and validate and answer for the current context (i.e. user,
 * request, XWiki Context, etc.).
 *
 * @version $Id$
 * @since 10.8RC1
 */
@Unstable
@Role
public interface Captcha
{
    /**
     * @return the generated and displayed CAPTCHA challenge for the current context
     * @throws CaptchaException in case of problems
     */
    String display() throws CaptchaException;

    /**
     * @param parameters the configuration values to override when displaying the CAPTCHA. Missing parameters value are
     *            taken from the CAPTCHA's configuration
     * @return the generated and displayed CAPTCHA challenge for the current context using the given parameters to
     *         override the configuration
     * @throws CaptchaException in case of problems
     */
    String display(Map<String, Object> parameters) throws CaptchaException;

    /**
     * @return {@code true} if the CAPTCHA answer passed to the current context is valid; {@code false} otherwise
     * @throws CaptchaException in case of problems
     */
    boolean isValid() throws CaptchaException;

    /**
     * @param parameters the configuration values to override when validating the CAPTCHA's answer. Missing parameters
     *            value are taken from the CAPTCHA's configuration
     * @return {@code true} if the CAPTCHA answer passed to the current context, using the given parameters to override
     *         the configuration, is valid; {@code false} otherwise
     * @throws CaptchaException in case of problems
     */
    boolean isValid(Map<String, Object> parameters) throws CaptchaException;
}
