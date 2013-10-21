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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Various APIs to make it easy to perform Git commands from within scripts.
 *
 * @version $Id$
 * @since 4.2M1
 */
@Component
@Named("git")
@Singleton
@Unstable
public class GitScriptService implements ScriptService
{
    /**
     * Required to get access to the Environment's permanent directory, where the Script service will clone Git
     * repositories.
     */
    @Inject
    private Environment environment;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Clone a Git repository by storing it locally in the XWiki Permanent directory. If the repository is already
     * cloned, no action is done.
     *
     * @param repositoryURI the URI to the Git repository to clone (eg "git://github.com/xwiki/xwiki-commons.git")
     * @param localDirectoryName the name of the directory where the Git repository will be cloned (this directory is
     *        relative to the permanent directory
     * @return the cloned Repository instance
     */
    public Repository getRepository(String repositoryURI, String localDirectoryName)
    {
        Repository repository;

        File localDirectory = new File(this.environment.getPermanentDirectory(), "git/" + localDirectoryName);
        File gitDirectory = new File(localDirectory, ".git");
        this.logger.debug("Local Git repository is at [{}]", gitDirectory);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        try {
            // Step 1: Initialize Git environment
            repository = builder.setGitDir(gitDirectory)
                .readEnvironment()
                .findGitDir()
                .build();
            Git git = new Git(repository);

            // Step 2: Verify if the directory exists and isn't empty.
            if (!gitDirectory.exists()) {
                // Step 2.1: Need to clone the remote repository since it doesn't exist
                git.cloneRepository()
                    .setDirectory(localDirectory)
                    .setURI(repositoryURI)
                    .call();
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to execute Git command in [%s]", gitDirectory), e);
        }

        return repository;
    }
}
