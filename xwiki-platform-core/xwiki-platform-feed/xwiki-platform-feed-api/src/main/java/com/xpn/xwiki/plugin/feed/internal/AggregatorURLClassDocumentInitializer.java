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
package com.xpn.xwiki.plugin.feed.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Create or update the {@code XWiki.AggregatorURLClass} document with all required information.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component
@Singleton
@Named("XWiki.AggregatorURLClass")
public class AggregatorURLClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The local reference of the target class.
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "AggregatorURLClass");

    /**
     * Default constructor.
     */
    public AggregatorURLClassDocumentInitializer()
    {
        super(REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.setCustomMapping("internal");

        xclass.addTextField("name", "Name", 80);
        xclass.addTextField("url", "URL", 80);
        xclass.addTextField("imgurl", "Image URL", 80);
        xclass.addDateField("date", "Date", "dd/MM/yyyy HH:mm:ss");
        xclass.addNumberField("nb", "Count", 5, "integer");
    }
}
