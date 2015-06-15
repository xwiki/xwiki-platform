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

package org.xwiki.model.internal.reference;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.model.EntityType;

/**
 * Define string separators for string reference serializer and resolver.
 *
 * @version $Id$
 * @since 3.3M2
 */
interface StringReferenceSeparators
{
    /**
     * A backslash string.
     */
    char CESCAPE = '\\';

    /**
     * A colon string. Colon is used to separate wiki name.
     */
    char CWIKISEP = ':';

    /**
     * A dot string. Dot is used to separate space names and document name.
     */
    char CSPACESEP = '.';

    /**
     * An at-sign string. At sign is used to separate attachment name.
     */
    char CATTACHMENTSEP = '@';

    /**
     * An hat sign string. Hat sign is used to separate object name.
     */
    char COBJECTSEP = '^';

    /**
     * An dot is used to separate object property name.
     */
    char CPROPERTYSEP = CSPACESEP;

    /**
     * An hat sign is used to separate class name.
     */
    char CCLASSPROPSEP = COBJECTSEP;

    /**
     * A backslash string.
     */
    String ESCAPE = Character.toString(CESCAPE);

    /**
     * A double backslash string.
     */
    String DBLESCAPE = ESCAPE + ESCAPE;

    /**
     * A colon string. Colon is used to separate wiki name.
     */
    String WIKISEP = Character.toString(CWIKISEP);

    /**
     * A dot string. Dot is used to separate space names and document name.
     */
    String SPACESEP = Character.toString(CSPACESEP);

    /**
     * An at-sign string. At sign is used to separate attachment name.
     */
    String ATTACHMENTSEP = Character.toString(CATTACHMENTSEP);

    /**
     * An hat sign string. Hat sign is used to separate object name.
     */
    String OBJECTSEP = Character.toString(COBJECTSEP);

    /**
     * An dot is used to separate object property name.
     */
    String PROPERTYSEP = Character.toString(CPROPERTYSEP);

    /**
     * An hat sign is used to separate class property name.
     */
    String CLASSPROPSEP = Character.toString(CCLASSPROPSEP);

    /**
     * The list of strings to escape for each type of entity.
     */
    Map<EntityType, String[]> ESCAPES = new HashMap<EntityType, String[]>()
    {
        {
            put(EntityType.ATTACHMENT, new String[] { ATTACHMENTSEP, ESCAPE });
            put(EntityType.DOCUMENT, new String[] { SPACESEP, ESCAPE });
            put(EntityType.SPACE, new String[] { SPACESEP, WIKISEP, ESCAPE });
            put(EntityType.OBJECT, new String[] { OBJECTSEP, ESCAPE });
            put(EntityType.OBJECT_PROPERTY, new String[] { PROPERTYSEP, ESCAPE });
            put(EntityType.CLASS_PROPERTY, new String[] { CLASSPROPSEP, SPACESEP, ESCAPE });
        }
    };

    /**
     * The replacement list corresponding to the list in {@link #ESCAPES} map.
     */
    Map<EntityType, String[]> REPLACEMENTS = new HashMap<EntityType, String[]>()
    {
        {
            put(EntityType.ATTACHMENT, new String[] { ESCAPE + ATTACHMENTSEP, DBLESCAPE });
            put(EntityType.DOCUMENT, new String[] { ESCAPE + SPACESEP, DBLESCAPE });
            put(EntityType.SPACE, new String[] { ESCAPE + SPACESEP, ESCAPE + WIKISEP, DBLESCAPE });
            put(EntityType.OBJECT, new String[] { ESCAPE + OBJECTSEP, DBLESCAPE });
            put(EntityType.OBJECT_PROPERTY, new String[] { ESCAPE + PROPERTYSEP, DBLESCAPE });
            put(EntityType.CLASS_PROPERTY, new String[] { ESCAPE + CLASSPROPSEP, ESCAPE + SPACESEP, DBLESCAPE });
        }
    };

    /**
     * Map<current entity, Map<parent separator, parent type>>.
     * 
     * @since 7.2M1
     */
    Map<EntityType, Map<Character, EntityType>> REFERENCE_SETUP = new HashMap<EntityType, Map<Character, EntityType>>()
    {
        {
            put(EntityType.DOCUMENT, Collections.singletonMap(CSPACESEP, EntityType.SPACE));
            put(EntityType.ATTACHMENT, Collections.singletonMap(CATTACHMENTSEP, EntityType.DOCUMENT));
            Map<Character, EntityType> spaceSetup = new LinkedHashMap<>();
            // Default parent (used for relative reference) is wiki so it's first in the map
            spaceSetup.put(CWIKISEP, EntityType.WIKI);
            spaceSetup.put(CSPACESEP, EntityType.SPACE);
            put(EntityType.SPACE, spaceSetup);
            put(EntityType.OBJECT, Collections.singletonMap(COBJECTSEP, EntityType.DOCUMENT));
            put(EntityType.OBJECT_PROPERTY, Collections.singletonMap(CPROPERTYSEP, EntityType.OBJECT));
            put(EntityType.CLASS_PROPERTY, Collections.singletonMap(CCLASSPROPSEP, EntityType.DOCUMENT));
        }
    };

    Map<EntityType, EntityType[]> TYPE_PARENTS = new HashMap<EntityType, EntityType[]>()
    {
        {
            put(EntityType.DOCUMENT, new EntityType[] {});
            put(EntityType.ATTACHMENT, new EntityType[] {});
            put(EntityType.SPACE, new EntityType[] {});
            put(EntityType.OBJECT, new EntityType[] {});
            put(EntityType.OBJECT_PROPERTY, new EntityType[] {});
            put(EntityType.CLASS_PROPERTY, new EntityType[] {});
            put(EntityType.WIKI, new EntityType[] {});
        }
    };
}
