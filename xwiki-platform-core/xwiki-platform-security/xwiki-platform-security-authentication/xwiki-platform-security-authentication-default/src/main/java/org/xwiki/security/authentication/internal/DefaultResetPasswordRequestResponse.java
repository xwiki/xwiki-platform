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
package org.xwiki.security.authentication.internal;

import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.security.authentication.ResetPasswordRequestResponse;
import org.xwiki.user.UserReference;

/**
 * Default implementation of the {@link ResetPasswordRequestResponse}.
 *
 * @version $Id$
 * @since 13.1RC1
 */
public final class DefaultResetPasswordRequestResponse implements ResetPasswordRequestResponse
{
    private final UserReference userReference;
    private final InternetAddress userEmail;
    private final String verificationCode;

    /**
     * Default constructor.
     * @param reference the user for whom a reset password request is performed.
     * @param userEmail the email of the user.
     * @param verificationCode the code to send for resetting the password.
     */
    DefaultResetPasswordRequestResponse(UserReference reference, InternetAddress userEmail, String verificationCode)
    {
        this.userReference = reference;
        this.userEmail = userEmail;
        this.verificationCode = verificationCode;
    }

    /**
     * @return the user for whom a reset password request is performed.
     */
    public UserReference getUserReference()
    {
        return userReference;
    }

    /**
     * @return the email of the user.
     */
    public InternetAddress getUserEmail()
    {
        return userEmail;
    }

    /**
     * @return the code to send for resetting the password.
     */
    public String getVerificationCode()
    {
        return verificationCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultResetPasswordRequestResponse that = (DefaultResetPasswordRequestResponse) o;

        return new EqualsBuilder()
            .append(userReference, that.userReference)
            .append(userEmail, that.userEmail)
            .append(verificationCode, that.verificationCode)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(userReference)
            .append(userEmail)
            .append(verificationCode)
            .toHashCode();
    }
}
