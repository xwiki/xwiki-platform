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
package org.xwiki.vfs.test.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.junit.Assert.assertEquals;

/**
 * Functional tests for the VFS feature.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class VfsTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void testVfsMacro() throws Exception
    {
        // Delete pages that we create in the test
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());

        // Scenario:
        // - Attach a zip to a wiki page
        // - Use the VFS Tree Macro to display the content of that zip
        // - Click on a tree node to display the content of a file inside the zip
        getUtil().attachFile(getTestClassName(), getTestMethodName(), "test.zip", createZipInputStream(), false);
        String content = "{{vfsTree root=\"attach:test.zip\"/}}";
        ViewPage vp = getUtil().createPage(getTestClassName(), getTestMethodName(), content, "VFS Test");

        // Get hold of the Tree and expand the directory node and the click on the first children node
        TreeElement tree = new TreeElement(getDriver().findElement(By.cssSelector(".xtree")));
        tree.waitForIt();
        TreeNodeElement node = tree.getNode("//directory");
        node = node.open();
        node.waitForIt();
        node.getChildren().get(0).select();
        assertEquals("content2", getDriver().findElement(By.tagName("body")).getText());
    }

    private InputStream createZipInputStream() throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Entry 1: File 1
            ZipEntry entry = new ZipEntry("node1");
            zos.putNextEntry(entry);
            zos.write("content1".getBytes());
            zos.closeEntry();
            // Entry 2: File 2
            entry = new ZipEntry("directory/node2");
            zos.putNextEntry(entry);
            zos.write("content2".getBytes());
            zos.closeEntry();
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

}
