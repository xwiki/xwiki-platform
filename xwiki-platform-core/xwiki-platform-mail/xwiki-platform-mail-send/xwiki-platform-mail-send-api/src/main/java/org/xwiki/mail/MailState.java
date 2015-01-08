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

package org.xwiki.mail;

/**
 * Represents the state of a mail (ready to be sent, sent successfully or failed to be sent).
 *
 * @version $Id$
 * @since 6.4M3
 */
public enum MailState
{
    /**
     *  Mail ready to be sent.
     */
    READY,

    /**
     * Mail sent with success.
     */
    SENT,
    /**
     * Error was encountered during sending mail.
     */
    FAILED;

    /**
     * @return the lower case String version of the enum, to use lowercase String on database
     */
    @Override
    public String toString()
    {
        return super.toString().toLowerCase();
    }

    /**
     * Create a MailState object from a String.
     *
     * @param state the state represented as a string
     * @return the MailState object
     * @throws java.lang.IllegalArgumentException if the passed stated is invalid
     */
    public static MailState parse(String state)
    {
        MailState result;
        if (state.equalsIgnoreCase(READY.toString())) {
            result = READY;
        } else if (state.equalsIgnoreCase(SENT.toString())) {
            result = SENT;
        } else if (state.equalsIgnoreCase(FAILED.toString())) {
            result = FAILED;
        } else {
            throw new IllegalArgumentException(String.format("Invalid mail state [%s]", state));
        }
        return result;
    }
}
