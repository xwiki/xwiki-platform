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
package org.xwiki.eventstream.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Define the EventClass XObjects.
 *
 * @version $Id$
 * @since 9.6RC1
 */
@Component
@Named("XWiki.EventStream.Code.EventClass")
@Singleton
public class EventClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the space where the class is located.
     */
    private static final List<String> SPACE_PATH = Arrays.asList("XWiki", "EventStream", "Code");

    private static final String INPUT = "input";

    private static final String STATIC_LIST_FIELD_SEPARATOR = " ,|";

    /**
     * Default constructor.
     */
    public EventClassDocumentInitializer()
    {
        super(new LocalDocumentReference(SPACE_PATH, "EventClass"));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField("applicationName", "Application Name", 64);
        xclass.addTextField("applicationId", "Application Identifier", 64);
        xclass.addTextField("eventType", "Event type", 64);
        xclass.addTextField("applicationIcon", "Event icon", 64);
        xclass.addTextField("eventTypeIcon", "Event Type icon", 64);
        xclass.addTextField("eventDescription", "Event description", 64);
        xclass.addStaticListField("listenTo", "Listen to …", 64, true,
                false, "", INPUT, STATIC_LIST_FIELD_SEPARATOR);
        xclass.addStaticListField("objectType", "Object type", 256, true,
                false, "", INPUT, STATIC_LIST_FIELD_SEPARATOR);
        xclass.addTextAreaField("validationExpression", "Validation expression",
                40, 3, TextAreaClass.ContentType.VELOCITY_CODE);
    }
}
