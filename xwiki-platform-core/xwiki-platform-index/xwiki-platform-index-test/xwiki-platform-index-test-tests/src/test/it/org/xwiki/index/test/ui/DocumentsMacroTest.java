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
package org.xwiki.index.test.ui;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.index.test.po.DocumentsMacroPage;
import org.xwiki.test.ui.AbstractGuestTest;
import org.xwiki.test.ui.po.LiveTableElement;

 /**
  * Tests for the DocumentsMacro page.
  *
  * @version $Id$
  * @since 4.2M3
  */
 public class DocumentsMacroTest extends AbstractGuestTest
 {
     /**
      * Verify that the {{documents}} macro works by going to the page defining this wiki macro since it contains
      * an example usage and we can verify it displays the expected result.
      */
     @Test
     public void testDocumentsMacro() throws Exception
     {
         // Create a dummy page in the Main space and having Main.WebHome as its parent so that it appears in the
         // Documents Macro livetable (since the example lists pages in the Main space having Main.WebHome as their
         // parent).
         getUtil().createPage("Main", getTestMethodName(), "", "Test Title", "xwiki/2.1", "Main.WebHome");

         DocumentsMacroPage dmp = DocumentsMacroPage.gotoPage();
         LiveTableElement livetable = dmp.getDocumentsExampleLiveTable();

         // Verify that we have a Page column
         Assert.assertTrue("No Title column found", livetable.hasColumn("Title"));

         // Verify there are several rows displayed
         Assert.assertTrue(livetable.getRowCount() > 0);

         // Verify that page titles are displayed by filtering on one page for which we know the title
         livetable.filterColumn("xwiki-livetable-example-filter-2", getTestMethodName());
         Assert.assertTrue(livetable.hasRow("Title", "Test Title"));
     }
 }
