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

import java.util.Map;

/**
 * A {@link MailListener} doing nothing.
 * 
 * @version $Id$
 * @since 12.6
 */
public class VoidMailListener implements MailListener
{
    @Override
    public void onPrepareBegin(String batchId, Map<String, Object> parameters)
    {

    }

    @Override
    public void onPrepareMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {

    }

    @Override
    public void onPrepareMessageError(ExtendedMimeMessage message, Exception e, Map<String, Object> parameters)
    {

    }

    @Override
    public void onPrepareFatalError(Exception exception, Map<String, Object> parameters)
    {

    }

    @Override
    public void onPrepareEnd(Map<String, Object> parameters)
    {

    }

    @Override
    public void onSendMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {

    }

    @Override
    public void onSendMessageError(ExtendedMimeMessage message, Exception exception, Map<String, Object> parameters)
    {

    }

    @Override
    public void onSendMessageFatalError(String uniqueMessageId, Exception exception, Map<String, Object> parameters)
    {

    }

    @Override
    public MailStatusResult getMailStatusResult()
    {
        return EmptyMailStatusResult.INSTANCE;
    }
}
