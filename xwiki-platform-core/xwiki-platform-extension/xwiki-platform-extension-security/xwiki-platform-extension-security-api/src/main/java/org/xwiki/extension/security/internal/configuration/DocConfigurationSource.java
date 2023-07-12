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
package org.xwiki.extension.security.internal.configuration;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Retrieve the extension security configuration from the {@link #XOBJECT_REFERENCE} document, using the
 * {@link #XCLASS_REFERENCE} XClass.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Named(DocConfigurationSource.ID)
@Singleton
public class DocConfigurationSource extends AbstractDocumentConfigurationSource
{
    /**
     * The hint of this component.
     */
    public static final String ID = "extensionSecurity";

    private static final List<String> SPACE = List.of("XWiki", "Extension", "Security", "Code");

    /**
     * The local reference of the configuration object.
     */
    public static final LocalDocumentReference XOBJECT_REFERENCE = new LocalDocumentReference(SPACE, "Config");

    /**
     * The local reference of the configuration class.
     */
    public static final LocalDocumentReference XCLASS_REFERENCE = new LocalDocumentReference(SPACE, "ConfigClass");

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return XCLASS_REFERENCE;
    }

    @Override
    protected String getCacheKeyPrefix()
    {
        return this.wikiManager.getCurrentWikiId();
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        return new DocumentReference(XOBJECT_REFERENCE, getCurrentWikiReference());
    }

    @Override
    protected String getCacheId()
    {
        return "configuration.extension.security";
    }
}
