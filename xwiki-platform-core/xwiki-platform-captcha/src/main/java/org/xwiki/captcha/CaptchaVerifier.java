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

import javax.servlet.http.HttpServletRequest;

import org.xwiki.component.annotation.Role;

/**
 * The CaptchaVerifier interface allows us to check if an answer to a captcha was correct.
 * 
 * @version $Id$
 * @since 2.2M2
 */
@Role
public interface CaptchaVerifier
{
    /**
     * Check if the solution to a captcha is correct.
     *
     * @param captchaId A String used to identify the captcha which is to be solved because multiple users may 
     *                  be solving captchas at the same time and which captcha belongs to who can't be confused.
     * @param answer The provided solution.
     * @return true if the solution is correct for the identified captcha.
     * @throws Exception If for some reason the CaptchaService fails or cannot be reached, or if the
     *                   CaptchaService has no record of any captcha gotten with the given captchaId.
     */
    boolean isAnswerCorrect(String captchaId, String answer) throws Exception;

    /**
     * Uses an HttpServletRequest object to get a unique id string to tell different users apart.
     * depending on the implementation, the id may be the servlet id or a generated value passed through a parameter.
     *
     * @param request The HttpServletRequest sent by the user who must be identified.
     * @return A unique String representing the user who sent the request
     */
    String getUserId(HttpServletRequest request);
}
