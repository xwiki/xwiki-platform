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
package org.xwiki.notifications.filters.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Define the ToggleableFilterPreferenceClass XClass.
 * This XClass is used to store filters information in the user profile.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Named("XWiki.Notifications.Code.ToggleableFilterPreferenceClass")
@Singleton
public class ToggleableFilterPreferenceDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The path to the class parent document.
     */
    private static final List<String> PARENT_PATH = Arrays.asList("XWiki", "Notifications", "Code");
    public static final LocalDocumentReference XCLASS =
        new LocalDocumentReference(PARENT_PATH, "ToggleableFilterPreferenceClass");

    public static final String FIELD_FILTER_NAME = "filterName";

    public static final String FIELD_IS_ENABLED = "isEnabled";


    /**
     * Default constructor.
     */
    public ToggleableFilterPreferenceDocumentInitializer()
    {
        super(XCLASS);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(FIELD_FILTER_NAME, "Filter name", 64);
        xclass.addBooleanField(FIELD_IS_ENABLED, "Is enabled ?", "checkbox", "", true);
    }
}
