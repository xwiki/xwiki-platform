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
package org.xwiki.model.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;

import org.junit.*;
import org.xwiki.environment.Environment;
import org.xwiki.model.Content;
import org.xwiki.model.DocumentEntity;
import org.xwiki.model.UniqueReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @since 5.2M2
 */
@ComponentList({
    DefaultGitStore.class
})
public class GitEntityManagerTest
{
    @Rule
    public MockitoComponentMockingRule<GitEntityManager> mocker =
        new MockitoComponentMockingRule<GitEntityManager>(GitEntityManager.class, Arrays.asList(GitStore.class));

    @Test
    public void getEntityWhenDocument() throws Exception
    {
        Environment environment = this.mocker.registerMockComponent(Environment.class);
        File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));
        when(environment.getPermanentDirectory()).thenReturn(temporaryDirectory);

        // Create a Document in Git so that we can test that we can get it.
        GitStore gitStore = this.mocker.getInstance(GitStore.class);
        // TODO: Define the way to store content in git. Idea: use the json format to define metadata. More to come.
        gitStore.addFile("wiki/space/page", "default.json", new ByteArrayInputStream("content".getBytes()));

        UniqueReference reference = new UniqueReference(new DocumentReference("wiki", "space", "page"));
        DocumentEntity entity = this.mocker.getComponentUnderTest().getEntity(reference);
        assertEquals(new Content("content", Syntax.XWIKI_2_1), entity.getContent());
    }
}
