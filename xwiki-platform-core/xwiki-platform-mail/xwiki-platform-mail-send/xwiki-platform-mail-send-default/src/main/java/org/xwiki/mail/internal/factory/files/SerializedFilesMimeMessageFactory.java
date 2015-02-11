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
package org.xwiki.mail.internal.factory.files;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.internal.factory.AbstractIteratorMimeMessageFactory;

/**
 * Loads Serialized {@link javax.mail.internet.MimeMessage} from the file system (from a directory name derived from
 * the batch id), located in the permanent directory).
 *
 * @version $Id$
 * @since 6.4.1
 */
@Component
@Named("files")
@Singleton
public class SerializedFilesMimeMessageFactory extends AbstractIteratorMimeMessageFactory
{
    @Override
    public Iterator<MimeMessage> createMessage(Session session, Object batchIdObject, Map<String, Object> parameters)
        throws MessagingException
    {
        String batchId = getTypedSource(batchIdObject, String.class);
        SerializedFilesMimeMessageIterator iterator = new SerializedFilesMimeMessageIterator(batchId, parameters,
            this.componentManagerProvider.get());
        return iterator;
    }
}
