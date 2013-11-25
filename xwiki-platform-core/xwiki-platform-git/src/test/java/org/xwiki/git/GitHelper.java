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
package org.xwiki.git;

import java.io.File;
import java.io.PrintWriter;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.*;
import org.xwiki.environment.Environment;

public class GitHelper
{
    private Environment environment;

    public GitHelper(Environment environment)
    {
        this.environment = environment;
    }

    public File getRepositoryFile(String repoName) throws Exception
    {
        File localGitDirectory = new File(this.environment.getPermanentDirectory(), "git");
        File localDirectory = new File(localGitDirectory, repoName);
        return localDirectory;
    }

    public boolean exists(String repoName) throws Exception
    {
        return getRepositoryFile(repoName).exists();
    }

    public Repository createGitTestRepository(String repoName) throws Exception
    {
        File localDirectory = getRepositoryFile(repoName);
        File gitDirectory = new File(localDirectory, ".git");
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(gitDirectory)
            .readEnvironment()
            .findGitDir()
            .build();
        if (!gitDirectory.exists()) {
            repository.create();
        }
        return repository;
    }

    public void add(File repo, String path, String content, PersonIdent author, PersonIdent committer,
        String message) throws Exception
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
        RevCommit commit = git.commit().setOnly(path).setMessage(message).setAuthor(author).setCommitter(committer)
            .call();
        Assert.assertNotNull(commit);
    }
}
