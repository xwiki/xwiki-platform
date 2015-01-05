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
package org.xwiki.mail.internal.script;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.internal.iterator.factory.GroupMimeMessageIteratorFactory;
import org.xwiki.mail.internal.iterator.factory.SerializedFilesMimeMessageIteratorFactory;
import org.xwiki.mail.internal.iterator.factory.UsersMimeMessageIteratorFactory;
import org.xwiki.model.reference.DocumentReference;

/**
 * Get an {@link Iterator} of {@link javax.mail.internet.MimeMessage} by internally finding some Iterator Factory
 * component based on various parameters passed.
 *
 * @version $Id$
 * @since 6.4M3
 */
public final class MimeMessageIteratorFactoryProvider
{
    private MimeMessageIteratorFactoryProvider()
    {
        // Hide the default constructor for utility classes
    }

    /**
     *
     * @param hint the MimeMessageIterator factories hint
     * @param source the source from which to prefill the Mime Message iterator (depends on the implementation)
     * @param factory to create MimeMessage
     * @param parameters an optional generic list of parameters. The supported parameters depend on the implementation
     * @param componentManager used to dynamically load all MimeMessageIterator
     * @return Iterator of MimeMessage
     * @throws Exception when an error occurs
     */
    public static Iterator<MimeMessage> get(String hint, Object source, MimeMessageFactory factory,
        Map<String, Object> parameters, ComponentManager componentManager) throws Exception
    {
        Iterator<MimeMessage> iterator;
        if (hint.equals("users")) {
            UsersMimeMessageIteratorFactory iteratorFactory = componentManager.getInstance(
                UsersMimeMessageIteratorFactory.class);
            iterator = iteratorFactory.create((List<DocumentReference>) source, factory, parameters);
        } else if (hint.equals("group")) {
            GroupMimeMessageIteratorFactory iteratorFactory = componentManager.getInstance(
                GroupMimeMessageIteratorFactory.class);
            iterator = iteratorFactory.create((DocumentReference) source, factory, parameters);
        } else if (hint.equals("files")) {
            SerializedFilesMimeMessageIteratorFactory iteratorFactory = componentManager.getInstance(
                SerializedFilesMimeMessageIteratorFactory.class);
            iterator = iteratorFactory.create((UUID) source, parameters);
        } else {
            iterator = null;
        }
        return iterator;
    }
}
