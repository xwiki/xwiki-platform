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
package org.xwiki.git.script;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gitective.core.CommitFinder;
import org.gitective.core.filter.commit.CommitCountFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.StandardEnvironment;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.jmock.AbstractComponentTestCase;

/**
 * Unit tests for {@link org.xwiki.git.script.GitScriptService}.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class GitScriptServiceTest extends AbstractComponentTestCase
{
    private static final String TEST_REPO_ORIG = "test-repo-orig";

    private static final String TEST_REPO_CLONED = "test-repo-cloned";

    private File testRepository;

    @Before
    public void setupRepository() throws Exception
    {
        // Configure permanent directory to be the temporary directory
        StandardEnvironment environment = getComponentManager().getInstance(Environment.class);
        environment.setPermanentDirectory(environment.getTemporaryDirectory());

        // Delete repositories
        FileUtils.deleteDirectory(getRepositoryFile(TEST_REPO_ORIG));
        FileUtils.deleteDirectory(getRepositoryFile(TEST_REPO_CLONED));

        // Create a Git repository for the test
        this.testRepository = createGitTestRepository(TEST_REPO_ORIG);

        // Add a file so that we can test querying the test repository for more fun!
        add(testRepository, "test.txt", "test content", "first commit");
    }

    @Test
    public void pullRepository() throws Exception
    {
        GitScriptService service = (GitScriptService) getComponentManager().getInstance(ScriptService.class, "git");
        Repository repository = service.getRepository(this.testRepository.getAbsolutePath(), TEST_REPO_CLONED);
        Assert.assertEquals(true, new Git(repository).pull().call().isSuccessful());

        CommitFinder finder = new CommitFinder(repository);
        CommitCountFilter count = new CommitCountFilter();
        finder.setMatcher(count);
        finder.find();

        Assert.assertEquals(1, count.getCount());
    }

    private File getRepositoryFile(String repoName) throws Exception
    {
        Environment environment = getComponentManager().getInstance(Environment.class);
        return new File(environment.getPermanentDirectory(), "git/" + repoName);
    }

    private File createGitTestRepository(String repoName) throws Exception
    {
        File localDirectory = getRepositoryFile(repoName);
        File gitDirectory = new File(localDirectory, ".git");
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(gitDirectory)
            .readEnvironment()
            .findGitDir()
            .build();
        repository.create();
        return repository.getDirectory();
    }

    private void add(File repo, String path, String content, String message) throws Exception
    {
   		File file = new File(repo.getParentFile(), path);
   		if (!file.getParentFile().exists()) {
            Assert.assertTrue(file.getParentFile().mkdirs());
        }
   		if (!file.exists()) {
            Assert.assertTrue(file.createNewFile());
        }
   		PrintWriter writer = new PrintWriter(file);
   		if (content == null)
   			content = "";
   		try {
   			writer.print(content);
   		} finally {
   			writer.close();
   		}
   		Git git = Git.open(repo);
   		git.add().addFilepattern(path).call();
   		RevCommit commit = git.commit().setOnly(path).setMessage(message)
   				.setAuthor("test author", "john@does.com").setCommitter("test committer", "john@does.com").call();
        Assert.assertNotNull(commit);
    }
}
