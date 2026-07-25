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
package org.xwiki.extension.security.internal.analyzer.osv.model.response;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.version.internal.DefaultVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@link VulnObject}.
 *
 * @version $Id$
 */
class VulnObjectTest
{
    private final VulnObject vulnObject = new VulnObject();

    @Test
    void getMainURLWithoutReferences()
    {
        assertEquals("", this.vulnObject.getMainURL());
    }

    @Test
    void getMaxFixVersionWithoutAffected()
    {
        assertEquals(Optional.empty(), this.vulnObject.getMaxFixVersion(new DefaultVersion("7.5")));
    }

    @Test
    void getMaxFixVersionWithAffectedWithoutRanges()
    {
        this.vulnObject.setAffected(List.of(new AffectObject()));
        assertEquals(Optional.empty(), this.vulnObject.getMaxFixVersion(new DefaultVersion("7.5")));
    }

    @Test
    void getMaxFixVersion()
    {
        EventObject eventObject = new EventObject();
        eventObject.setFixed("15.7");
        RangeObject rangeObject = new RangeObject();
        rangeObject.setEvents(List.of(eventObject));
        AffectObject affectObject = new AffectObject();
        affectObject.setRanges(List.of(rangeObject));
        this.vulnObject.setAffected(List.of(affectObject));

        assertEquals(Optional.of(new DefaultVersion("15.7")),
            this.vulnObject.getMaxFixVersion(new DefaultVersion("7.5")));
    }
}
