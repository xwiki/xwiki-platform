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
package org.xwiki.filter.test.xarinstance;

import java.io.IOException;

import org.junit.Test;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.instance.internal.input.AbstractInstanceInputFilterStreamTest;
import org.xwiki.filter.instance.internal.output.DocumentInstanceOutputFilterStream;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.test.annotation.AllComponents;

/**
 * Validate {@link DocumentInstanceOutputFilterStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class DocumentInstanceInputFilterStreamTest extends AbstractInstanceInputFilterStreamTest
{
    // Tests

    @Test
    public void testImportDocumentsPreserveVersion() throws FilterException, IOException
    {
        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();
        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(false);

        DocumentInstanceInputProperties inputProperties = new DocumentInstanceInputProperties();
        inputProperties.setWithRevisions(false);
        inputProperties.setWithJRCSRevisions(false);
        inputProperties.setVerbose(false);

        assertXML("document1", outputProperties, inputProperties);
    }
}
