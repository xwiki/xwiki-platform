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

package org.xwiki.test.ui;

import org.junit.Test;
import org.xwiki.rendering.syntax.Syntax;

import static org.junit.Assert.assertEquals;

/**
 * Check that all mandatory document initializers are properly declared in the demo package build and that the pages
 * are not created at first start only. See also XWIKI-22368.
 *
 * @version $Id$
 * @since 16.7.0RC1
 */
public class MandatoryDocInitializedTest extends AbstractTest
{
    private static final String GROOVY_CODE = "{{groovy}}\n"
        + "import com.xpn.xwiki.doc.MandatoryDocumentInitializer;\n"
        + "import java.text.SimpleDateFormat;\n"
        + "import java.lang.management.ManagementFactory;\n"
        + "\n"
        + "// define the pages that should be created at startup\n"
        + "def startupPages = [\"XWiki.XWikiServerXwiki\"];\n"
        + "\n"
        + "def dateFormat = new SimpleDateFormat(\"dd/MM/YYYY HH:mm:ss\");\n"
        + "def rb = ManagementFactory.getRuntimeMXBean();\n"
        + "def startedSince = new Date(rb.getStartTime());\n"
        + "\n"
        + "def startupPageRef = [];\n"
        + "for (startupPage in startupPages) {\n"
        + "  startupPageRef.add(services.model.resolveDocument(startupPage));\n"
        + "}\n"
        + "\n"
        + "def componentManager = services.component.getContextComponentManager();\n"
        + "def documentInitializersList = componentManager.getInstanceList(MandatoryDocumentInitializer.class);\n"
        + "\n"
        + "def foundErrorsCreatedAfter = [];\n"
        + "def foundErrorsCreatedBefore = [];\n"
        + "for (initializer in documentInitializersList) {\n"
        + "  def ref = initializer.getDocumentReference();\n"
        + "  def classDoc = xwiki.getDocument(ref);\n"
        + "  def creationDate = classDoc.getCreationDate();\n"
        + "  def shouldBeBefore = (!startupPageRef.contains(ref));\n"
        + "  if (shouldBeBefore && creationDate.after(startedSince)) {\n"
        + "    foundErrorsCreatedAfter.add(ref.toString() + \" (\"+dateFormat.format(creationDate)+\")\");\n"
        + "  } else if (!shouldBeBefore && creationDate.before(startedSince)) {\n"
        + "    foundErrorsCreatedBefore.add(ref.toString() + \" (\"+dateFormat.format(creationDate)+\")\");\n"
        + "  }\n"
        + "}\n"
        + "\n"
        + "if (foundErrorsCreatedAfter.size() > 0) {\n"
        + "  println \"Instance started at \" + dateFormat.format(startedSince);\n"
        + "  println foundErrorsCreatedAfter.size() + \" page found created after:\\n\";\n"
        + "  for (error in foundErrorsCreatedAfter) {\n"
        + "    println \"     * \" + error;\n"
        + "  }\n"
        + "}\n"
        + "if (foundErrorsCreatedBefore.size() > 0) {\n"
        + "  println \"Instance started at \" + dateFormat.format(startedSince);\n"
        + "  println foundErrorsCreatedBefore.size() + \" page found created before while they should be created at "
        + "startup:\\n\";\n"
        + "  for (error in foundErrorsCreatedBefore) {\n"
        + "    println \"     * \" + error;\n"
        + "  }\n"
        + "}\n"
        + "{{/groovy}}";

    @Test
    public void docsAreInitialized() throws Exception
    {
        getUtil().loginAsAdmin();
        String result = getUtil().executeWikiPlain(GROOVY_CODE, Syntax.XWIKI_2_1);

        // Using equals on purpose to see the output in case of failure.
        assertEquals("", result);
    }
}
