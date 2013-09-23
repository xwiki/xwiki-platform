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

import java.util.HashMap;
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
            put(EntityType.ATTACHMENT, new String[] {ATTACHMENTSEP, ESCAPE});
            put(EntityType.DOCUMENT, new String[] {SPACESEP, ESCAPE});
            put(EntityType.SPACE, new String[] {SPACESEP, WIKISEP, ESCAPE});
            put(EntityType.OBJECT, new String[] {OBJECTSEP, ESCAPE});
            put(EntityType.OBJECT_PROPERTY, new String[] {PROPERTYSEP, ESCAPE});
            put(EntityType.CLASS_PROPERTY, new String[] {CLASSPROPSEP, SPACESEP, ESCAPE});
        }
    };

    /**
     * The replacement list corresponding to the list in {@link #ESCAPES} map.
     */
    Map<EntityType, String[]> REPLACEMENTS = new HashMap<EntityType, String[]>()
    {
        {
            put(EntityType.ATTACHMENT, new String[] {ESCAPE + ATTACHMENTSEP, DBLESCAPE});
            put(EntityType.DOCUMENT, new String[] {ESCAPE + SPACESEP, DBLESCAPE});
            put(EntityType.SPACE, new String[] {ESCAPE + SPACESEP, ESCAPE + WIKISEP, DBLESCAPE});
            put(EntityType.OBJECT, new String[] {ESCAPE + OBJECTSEP, DBLESCAPE});
            put(EntityType.OBJECT_PROPERTY, new String[] {ESCAPE + PROPERTYSEP, DBLESCAPE});
            put(EntityType.CLASS_PROPERTY, new String[] {ESCAPE + CLASSPROPSEP, ESCAPE + SPACESEP, DBLESCAPE});
        }
    };

    /**
     * Map defining syntax separators for each type of reference.
     */
    Map<EntityType, char[]> SEPARATORS = new HashMap<EntityType, char[]>()
    {
        {
            put(EntityType.DOCUMENT, new char[] {CSPACESEP, CWIKISEP});
            put(EntityType.ATTACHMENT, new char[] {CATTACHMENTSEP, CSPACESEP, CWIKISEP});
            put(EntityType.SPACE, new char[] {CWIKISEP});
            put(EntityType.OBJECT, new char[] {COBJECTSEP, CSPACESEP, CWIKISEP});
            put(EntityType.OBJECT_PROPERTY, new char[] {CPROPERTYSEP, COBJECTSEP, CSPACESEP, CWIKISEP});
            put(EntityType.CLASS_PROPERTY, new char[] {CCLASSPROPSEP, CSPACESEP, CWIKISEP});
        }
    };
}
