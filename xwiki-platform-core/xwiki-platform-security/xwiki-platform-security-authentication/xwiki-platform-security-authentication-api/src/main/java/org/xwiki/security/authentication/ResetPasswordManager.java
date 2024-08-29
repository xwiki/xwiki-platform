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
package org.xwiki.security.authentication;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Component dedicated to handle the reset password operation.
 * This component is designed to handle a reset password in 3 steps:
 *   1. a request is performed for doing a reset password for a given user: a verification code is transmitted to the
 *      user by a side way (e.g. email)
 *   2. the user certifies her identity by sending back the verification code
 *   3. the user specify a new password which is updated internally.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Role
public interface ResetPasswordManager
{
    /**
     * Perform a reset password request and return the information to send to the user.
     * Note that the implementation of this method might have some side effect like modifying the user information.
     * @param userReference the reference of the user for which to reset the password.
     * @return the needed information to send to the user for confirming her identity.
     * @throws ResetPasswordException if any problem occurs.
     */
    ResetPasswordRequestResponse requestResetPassword(UserReference userReference) throws ResetPasswordException;

    /**
     * Send {@link ResetPasswordRequestResponse} information by email to the user.
     *
     * @param requestResponse the reset password information to send to the user to confirm her identity.
     * @throws ResetPasswordException in case of problem for sending the email.
     */
    void sendResetPasswordEmailRequest(ResetPasswordRequestResponse requestResponse) throws ResetPasswordException;

    /**
     * Check if the given verification code is correct for the user reference.
     * This method throws the {@link ResetPasswordException} if the verification code is not correct.
     * The verification code must be reset at each check, even if the validation is not correct, to ensure that an
     * attacker cannot bruteforce it.
     *
     * @param userReference the reference for which to check the verification code.
     * @param verificationCode the code to check.
     * @return the information about the user and the up-to-date verification code.
     * @throws ResetPasswordException if the verification code is wrong or cannot be validated.
     */
    ResetPasswordRequestResponse checkVerificationCode(UserReference userReference, String verificationCode)
        throws ResetPasswordException;

    /**
     * Reset the password of the given user with the given new password.
     * Note that this method should always be called after the verification code has been checked out.
     *
     * @param userReference the reference of the user for which to reset the password.
     * @param newPassword the new password to set.
     * @throws ResetPasswordException in case of problem when modifying the password.
     */
    void resetPassword(UserReference userReference, String newPassword)
        throws ResetPasswordException;

    /**
     * Ensure that the password matches the requirements provided by {@link RegistrationConfiguration}.
     * Note that for backward compatibility reason the default return of this method is always {@code true}.
     *
     * @param newPassword the password to check
     * @return {@code true} if the rules exposed in the {@link RegistrationConfiguration} are all respected.
     * @since 15.10RC1
     */
    @Unstable
    default boolean isPasswordCompliantWithRegistrationRules(String newPassword)
    {
        return true;
    }
}
