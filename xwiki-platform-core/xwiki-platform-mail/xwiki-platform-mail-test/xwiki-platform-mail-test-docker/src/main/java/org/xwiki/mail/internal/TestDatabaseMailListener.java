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
package org.xwiki.mail.internal;

import java.util.Map;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.mail.ExtendedMimeMessage;

/**
 * Component override for the test.
 *
 * @version $Id$
 */
@Component
@Named("database")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class TestDatabaseMailListener extends DatabaseMailListener
{
    // Global counter so that we only have the special handling below once. Since we want the Scheduler Job Resender
    // to work when it tries to resent it!
    private static int counter = 0;

    @Override
    public void onPrepareMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {
        super.onPrepareMessageSuccess(message, parameters);
        // This is a special case for the test. We want to make sure that the mail is not sent so that we can resend
        // it. Thus we raise an exception to make sure it's not sent to the sender thread.
        if (counter == 0 && message.getType() != null && message.getType().equals("Test Not Sent")) {
            counter++;
            throw new RuntimeException("Test Not Sent");
        }
    }

    @Override
    public void onPrepareFatalError(Exception exception, Map<String, Object> parameters)
    {
        if (counter > 0 || StringUtils.isEmpty(exception.getMessage())
            || !exception.getMessage().equals("Test Not Sent"))
        {
            super.onPrepareFatalError(exception, parameters);
        }
    }
}
