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
package org.xwiki.test.docker.internal.junit5;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

/**
 * To be used instead of {@link GenericContainer} to fix an OOM issue on TC side, see
 * <a href="https://github.com/testcontainers/testcontainers-java/issues/4203">issue 4203</a>.
 *
 * @param <T> the type of container
 * @version $Id$
 * @since 16.1.0RC1
 */
// TODO: Remove this class when https://github.com/testcontainers/testcontainers-java/issues/4203 is fixed
public class XWikiGenericContainer<T extends GenericContainer<T>> extends GenericContainer<T>
{
    /**
     * @see GenericContainer#GenericContainer(String)
     *
     * @param dockerImageName the name of the docker image to start a container from
     */
    public XWikiGenericContainer(String dockerImageName)
    {
        super(dockerImageName);
    }

    /**
     * @see GenericContainer#GenericContainer(Future)
     *
     * @param image the image to start a container from
     */
    public XWikiGenericContainer(Future<String> image)
    {
        super(image);
    }

    /**
     * Copies a file to the container, using a temporary TAR file to avoid keeping the whole content in memory. The
     * code applies the patch provided in
     * <a href="https://github.com/testcontainers/testcontainers-java/pull/2864/files#">issue 2864</a>.
     *
     * @param transferable file which is copied into the container
     * @param containerPath destination path inside the container
     */
    @Override
    public void copyFileToContainer(Transferable transferable, String containerPath)
    {
        if (!isCreated()) {
            throw new IllegalStateException("copyFileToContainer can only be used with created / running container");
        }
        try {
            Path targetDirectory = Path.of("target");
            Path tarFile = Files.createTempFile(targetDirectory, "tc-tar-tmp", ".tar");
            try {
                copyFileToContainer(transferable, containerPath, tarFile);
            } finally {
                Files.delete(tarFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to copy [%s] to [%s]", transferable, containerPath), e);
        }
    }

    private void copyFileToContainer(Transferable transferable, String containerPath, Path tarFile) throws IOException
    {
        try (OutputStream outputStream = Files.newOutputStream(tarFile);
            TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(outputStream))
        {
            tarArchive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tarArchive.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            transferable.transferTo(tarArchive, containerPath);
            tarArchive.finish();
        }
        try (InputStream inputStream = Files.newInputStream(tarFile)) {
            DockerClientFactory.instance().client()
                .copyArchiveToContainerCmd(getContainerId())
                .withTarInputStream(inputStream)
                .withRemotePath("/")
                .exec();
        }
    }
}
