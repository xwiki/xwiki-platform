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
package org.xwiki.localization.wiki.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update XWiki.TranslationDocumentClass document with all required informations.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named(TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE_STRING)
@Singleton
public class TranslationDocumentClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Default constructor.
     */
    public TranslationDocumentClassInitializer()
    {
        super(TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addStaticListField(TranslationDocumentModel.TRANSLATIONCLASS_PROP_SCOPE, "Scope",
            "ON_DEMAND|WIKI|USER|GLOBAL");
    }
}
