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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A helper used to notify several {@link MailListener}.
 * 
 * @version $Id$
 * @since 12.6
 */
public class CompositeMailListener implements MailListener
{
    private final MailListener mainListener;

    private final List<MailListener> listeners;

    /**
     * @param mainListener the main listener used for {@link #getMailStatusResult()}
     * @param listeners the other listeners to notify
     */
    public CompositeMailListener(MailListener mainListener, MailListener... listeners)
    {
        this.mainListener = mainListener;
        this.listeners = new ArrayList<>(1 + listeners.length);
        this.listeners.add(mainListener);
        for (MailListener listener : listeners) {
            this.listeners.add(listener);
        }
    }

    @Override
    public void onPrepareBegin(String batchId, Map<String, Object> parameters)
    {
        this.listeners.forEach(l -> l.onPrepareBegin(batchId, parameters));
    }

    @Override
    public void onPrepareMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {
        this.listeners.forEach(l -> l.onPrepareMessageSuccess(message, parameters));
    }

    @Override
    public void onPrepareMessageError(ExtendedMimeMessage message, Exception e, Map<String, Object> parameters)
    {
        this.listeners.forEach(l -> l.onPrepareMessageError(message, e, parameters));
    }

    @Override
    public void onPrepareFatalError(Exception exception, Map<String, Object> parameters)
    {
        this.listeners.forEach(l -> l.onPrepareFatalError(exception, parameters));
    }

    @Override
    public void onPrepareEnd(Map<String, Object> parameters)
    {
        this.listeners.forEach(l -> l.onPrepareEnd(parameters));
    }

    @Override
    public void onSendMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {
        this.listeners.forEach(l -> l.onSendMessageSuccess(message, parameters));
    }

    @Override
    public void onSendMessageError(ExtendedMimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        this.listeners.forEach(l -> l.onSendMessageError(message, exception, parameters));
    }

    @Override
    public void onSendMessageFatalError(String uniqueMessageId, Exception exception, Map<String, Object> parameters)
    {
        this.listeners.forEach(l -> l.onSendMessageFatalError(uniqueMessageId, exception, parameters));
    }

    @Override
    public MailStatusResult getMailStatusResult()
    {
        return this.mainListener.getMailStatusResult();
    }
}
