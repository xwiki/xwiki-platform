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
package org.xwiki.internal.document;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * A {@link MandatoryDocumentInitializer} for the required right class.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@Unstable
@Component
@Singleton
@Named("XWiki.RequiredRightClass")
public class RequiredRightClassMandatoryDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Default constructor.
     */
    public RequiredRightClassMandatoryDocumentInitializer()
    {
        super(DocumentRequiredRightsReader.CLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addStaticListField(DocumentRequiredRightsReader.PROPERTY_NAME, "Level", 1, false,
            "edit|script|wiki_admin|programming", "select", " ,|");
    }
}
