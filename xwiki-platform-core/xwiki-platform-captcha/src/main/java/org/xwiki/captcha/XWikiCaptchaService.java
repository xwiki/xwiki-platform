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

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Provides access to the classes implementing Captcha.
 *
 * @version $Id$
 * @since 2.2M2
 */
@Role
public interface XWikiCaptchaService
{
    /**
     * Get a {@link org.xwiki.captcha.CaptchaVerifier} from the service.
     *
     * @param captchaName The name of the type of captcha, use {@link #listCaptchaNames()} for a list
     * @return A captcha object which can be used to check a specific answer for a given challange
     * @throws CaptchaVerifierNotFoundException If the named VaptchaVerifier could not be found.
     */
    CaptchaVerifier getCaptchaVerifier(String captchaName) throws CaptchaVerifierNotFoundException;

    /** @return a List of the names of all registered classes implementing {@link org.xwiki.captcha.CaptchaVerifier}. */
    List<String> listCaptchaNames();

    /**
     * If this is false, the captcha system will still work.
     * It is for velocity scripts to determine whether captchas are needed.
     *
     * @return Is the captcha service enabled in the configuration.
     * @deprecated since 2.3M1 Captcha should only be enabled/disabled on an application by application basis.
     */
    @Deprecated
    boolean isEnabled();
}
