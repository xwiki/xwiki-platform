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

import java.util.Collections;
import java.util.Iterator;

/**
 * An empty {@link EmptyMailStatusResult}.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
public class EmptyMailStatusResult implements MailStatusResult
{
    /**
     * An instance of {@link EmptyMailStatusResult}.
     */
    public static final EmptyMailStatusResult INSTANCE = new EmptyMailStatusResult();

    @Override
    public long getTotalMailCount()
    {
        return 0;
    }

    @Override
    public long getProcessedMailCount()
    {
        return 0;
    }

    @Override
    public void waitTillProcessed(long timeout)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProcessed()
    {
        return true;
    }

    @Override
    public Iterator<MailStatus> getAll()
    {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<MailStatus> getAllErrors()
    {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<MailStatus> getByState(MailState state)
    {
        return Collections.emptyIterator();
    }
}
