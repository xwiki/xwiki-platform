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
package org.xwiki.index.tree.internal.nestedpages.pinned;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * Initialize the PinnedChildPagesClass class.
 * 
 * @version $Id$
 * @since 16.4.0RC1
 */
@Component
@Named(PinnedChildPagesClassInitializer.CLASS_REFERENCE_STRING)
@Singleton
public class PinnedChildPagesClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The class name.
     */
    public static final String CLASS_NAME = "PinnedChildPagesClass";

    /**
     * The class reference string.
     */
    public static final String CLASS_REFERENCE_STRING = XWiki.SYSTEM_SPACE + "." + CLASS_NAME;

    /**
     * The reference of the class used to store the pinned child pages.
     */
    public static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, CLASS_NAME);

    /**
     * The name of the property used to store the pinned child pages.
     */
    public static final String PROPERTY_NAME = "pinnedChildPages";

    /**
     * Default constructor.
     */
    public PinnedChildPagesClassInitializer()
    {
        super(CLASS_REFERENCE, "Pinned Child Pages Class");
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        StaticListClass pinnedChildPages = xclass.addStaticListField(PROPERTY_NAME);
        pinnedChildPages.setMultiSelect(true);
        pinnedChildPages.setRelationalStorage(true);
        pinnedChildPages.setCustomDisplay("{{template name=\"pinnedChildPagesDisplayer.vm\"/}}");
    }
}
