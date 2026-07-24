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
package org.xwiki.distributionwizard;

import org.xwiki.stability.Unstable;

/**
 * Any exception thrown by DW API.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Unstable
public class DistributionWizardException extends Exception
{
    /**
     * Default constructor with a single message.
     * @param message cause of the exception.
     */
    public DistributionWizardException(String message)
    {
        super(message);
    }

    /**
     * Default constructor with message and root cause.
     * @param message cause of the exception.
     * @param cause root cause of the exception.
     */
    public DistributionWizardException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
