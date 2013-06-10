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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;

@Component
public class DefaultGitStore implements GitStore, Initializable, Disposable
{
    @Inject
    private Environment environment;

    private File repositoryDirectory;

    private Git git;

    @Override
    public void initialize() throws InitializationException
    {
        this.repositoryDirectory = new File(this.environment.getPermanentDirectory(), "git");
        InitCommand initCommand = Git.init();
        initCommand.setDirectory(this.repositoryDirectory);
        try {
            this.git = initCommand.call();
        } catch (GitAPIException e) {
            throw new InitializationException(
                String.format("Failed to initialize Git repository at [%s]", this.repositoryDirectory), e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.git.getRepository().close();
    }

    @Override
    public void addFile(String directory, String fileName, InputStream content) throws IOException, GitAPIException
    {
        File dir = new File(this.repositoryDirectory, directory);
        dir.mkdirs();
        File newFile = new File(dir, fileName);
        createFile(newFile, content);
        addAndCommit("new content", ".");

    }

    @Override
    public InputStream getContent(String revSpec, String filePath) throws IOException
    {
        InputStream result = null;
        Repository repository = this.git.getRepository();
        final ObjectId id = repository.resolve(revSpec);

        // Get the commit object for that revision
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(id);

        // Get the commit's file tree
        RevTree tree = commit.getTree();
        // .. and narrow it down to the single file's path
        TreeWalk treewalk = TreeWalk.forPath(repository, filePath, tree);

        if (treewalk != null) { // if the file exists in that commit
            // use the blob id to read the file's data
            result = repository.open(treewalk.getObjectId(0)).openStream();
        }
        return result;
    }

    private void addAndCommit(String message, String pathToAdd) throws GitAPIException
    {
        add(pathToAdd);
        commit(message);
    }

    private void add(String pathToAdd) throws GitAPIException
    {
        AddCommand add = this.git.add();
        add.addFilepattern(pathToAdd).call();
    }

    private void commit(String message) throws GitAPIException
    {
        CommitCommand commit = this.git.commit();
        commit.setMessage(message).call();
    }

    private void createFile(File file, InputStream content) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(file);
        IOUtils.copy(content, fos);
        fos.close();
    }
}
