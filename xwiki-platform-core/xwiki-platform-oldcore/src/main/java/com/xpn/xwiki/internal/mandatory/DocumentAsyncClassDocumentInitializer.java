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
package com.xpn.xwiki.internal.mandatory;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;

/**
 * A class indicating that a document should be displayed/included asynchronously.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Named("XWiki.DocumentAsyncClass")
@Singleton
public class DocumentAsyncClassDocumentInitializer extends AbstractAsyncClassDocumentInitializer
{
    /**
     * The local reference of the class.
     */
    public static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "DocumentAsyncClass");

    /**
     * The default constructor.
     */
    public DocumentAsyncClassDocumentInitializer()
    {
        super(CLASS_REFERENCE);
    }
}
