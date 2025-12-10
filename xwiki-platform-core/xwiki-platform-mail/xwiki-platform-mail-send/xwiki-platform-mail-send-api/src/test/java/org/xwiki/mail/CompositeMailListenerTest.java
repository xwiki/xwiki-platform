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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Validate {@link CompositeMailListener}.
 * 
 * @version $Id$
 */
class CompositeMailListenerTest
{
    private CompositeMailListener compositeListener;

    private MailListener mainListener;

    private MailListener listener1;

    private MailListener listener2;

    @BeforeEach
    void beforeEach()
    {
        this.mainListener = mock(MailListener.class);
        this.listener1 = mock(MailListener.class);
        this.listener2 = mock(MailListener.class);

        this.compositeListener = new CompositeMailListener(this.mainListener, this.listener1, this.listener2);
    }

    @Test
    void onPrepareBegin()
    {
        this.compositeListener.onPrepareBegin(null, null);

        verify(this.mainListener).onPrepareBegin(null, null);
        verify(this.listener1).onPrepareBegin(null, null);
        verify(this.listener2).onPrepareBegin(null, null);
    }

    @Test
    void onPrepareMessageSuccess()
    {
        this.compositeListener.onPrepareMessageSuccess(null, null);

        verify(this.mainListener).onPrepareMessageSuccess(null, null);
        verify(this.listener1).onPrepareMessageSuccess(null, null);
        verify(this.listener2).onPrepareMessageSuccess(null, null);
    }

    @Test
    void onPrepareMessageError()
    {
        this.compositeListener.onPrepareMessageError(null, null, null);

        verify(this.mainListener).onPrepareMessageError(null, null, null);
        verify(this.listener1).onPrepareMessageError(null, null, null);
        verify(this.listener2).onPrepareMessageError(null, null, null);
    }

    @Test
    void onPrepareFatalError()
    {
        this.compositeListener.onPrepareFatalError(null, null);

        verify(this.mainListener).onPrepareFatalError(null, null);
        verify(this.listener1).onPrepareFatalError(null, null);
        verify(this.listener2).onPrepareFatalError(null, null);
    }

    @Test
    void onPrepareEnd()
    {
        this.compositeListener.onPrepareEnd(null);

        verify(this.mainListener).onPrepareEnd(null);
        verify(this.listener1).onPrepareEnd(null);
        verify(this.listener2).onPrepareEnd(null);
    }

    @Test
    void onSendMessageSuccess()
    {
        this.compositeListener.onSendMessageSuccess(null, null);

        verify(this.mainListener).onSendMessageSuccess(null, null);
        verify(this.listener1).onSendMessageSuccess(null, null);
        verify(this.listener2).onSendMessageSuccess(null, null);
    }

    @Test
    void onSendMessageError()
    {
        this.compositeListener.onSendMessageError(null, null, null);

        verify(this.mainListener).onSendMessageError(null, null, null);
        verify(this.listener1).onSendMessageError(null, null, null);
        verify(this.listener2).onSendMessageError(null, null, null);
    }

    @Test
    void onSendMessageFatalError()
    {
        this.compositeListener.onSendMessageFatalError(null, null, null);

        verify(this.mainListener).onSendMessageFatalError(null, null, null);
        verify(this.listener1).onSendMessageFatalError(null, null, null);
        verify(this.listener2).onSendMessageFatalError(null, null, null);
    }

    @Test
    void getMailStatusResult()
    {
        this.compositeListener.getMailStatusResult();

        verify(this.mainListener).getMailStatusResult();
        verifyNoInteractions(this.listener1);
        verifyNoInteractions(this.listener2);
    }
}
