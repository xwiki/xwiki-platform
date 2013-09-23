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

import javax.servlet.http.HttpServletRequest;

import org.xwiki.captcha.CaptchaVerifier;

/**
 * VelocityCaptchaVerifier wraps a CaptchaVerifier object and passes all calls to it and returns from it.
 * It catches any Exceptions because Velocity is unable to catch exceptions and it never returns null
 * because Velocity will not make an assignment if the output is null.
 *
 * @version $Id$
 * @since 2.2M2
 */
public class VelocityCaptchaVerifier implements CaptchaVerifier
{
    /** The internal captcha which this VelocityCaptcha wraps. */
    private final CaptchaVerifier wrappedCaptcha;

    /**
     * The Constructor.
     *
     * @param toWrap A Captcha object which this VelocityCaptcha should wrap.
     */
    public VelocityCaptchaVerifier(CaptchaVerifier toWrap)
    {
        this.wrappedCaptcha = toWrap;
    }

    @Override
    public String getUserId(HttpServletRequest request)
    {
        String x = wrappedCaptcha.getUserId(request);
        // If getUserId returns null, we should return "" because velocity will not assign null.
        return (x == null) ? "" : x;
    }

    @Override
    public boolean isAnswerCorrect(String userId, String answer)
    {
        try {
            return (userId.length() == 0) ? false : wrappedCaptcha.isAnswerCorrect(userId, answer);
        } catch (Exception e) {
            return false;
        }
    }
}
