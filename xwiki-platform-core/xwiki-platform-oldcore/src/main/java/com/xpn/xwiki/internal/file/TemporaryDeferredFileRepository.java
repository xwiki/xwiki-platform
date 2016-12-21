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
package com.xpn.xwiki.internal.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;

/**
 * Provide tools to manipulate a repository of unique temporary files.
 * 
 * @version $Id$
 * @since 9.0RC1
 */
@Component(roles = TemporaryDeferredFileRepository.class)
@Singleton
public class TemporaryDeferredFileRepository
{
    private static final int THRESHOLD = 10000;

    /**
     * The data stored in this "file" is stored in memory until the #THRESHOLD is reached in which case it start stored
     * the data in a temporary file.
     * 
     * @version $Id$
     */
    public class TemporaryDeferredFile
    {
        private final String repositoryPath;

        private DeferredFileOutputStream currentOutputStream;

        /**
         * @param repositoryPath the path of the repository inside the temporary directory
         */
        TemporaryDeferredFile(String repositoryPath)
        {
            this.repositoryPath = repositoryPath;
        }

        /**
         * @return create a new input stream to read the data stored in that file
         * @throws IOException when failing to create an {@link InputStream}
         */
        public InputStream getInputStream() throws IOException
        {
            if (this.currentOutputStream != null) {
                if (this.currentOutputStream.isInMemory()) {
                    return new ByteArrayInputStream(this.currentOutputStream.getData());
                } else {
                    return new FileInputStream(this.currentOutputStream.getFile());
                }
            } else {
                return new ByteArrayInputStream(ArrayUtils.EMPTY_BYTE_ARRAY);
            }
        }

        /**
         * @return create a new output stream to read the data stored in that file
         * @throws IOException when failing to create an {@link OutputStream}
         */
        public OutputStream getOutputStream() throws IOException
        {
            File file = createTemporaryFile(this.repositoryPath);
            this.currentOutputStream = new DeferredFileOutputStream(THRESHOLD, file);
            return this.currentOutputStream;
        }

        /**
         * @return the length of the content
         */
        public long length()
        {
            if (this.currentOutputStream != null) {
                if (this.currentOutputStream.isInMemory()) {
                    return this.currentOutputStream.getData().length;
                } else {
                    return this.currentOutputStream.getFile().length();
                }
            }

            return 0;
        }

        /**
         * @return the content of the file as byte[]
         * @throws IOException when failing to read the file
         */
        public byte[] getBytes() throws IOException
        {
            if (this.currentOutputStream != null) {
                if (this.currentOutputStream.isInMemory()) {
                    return this.currentOutputStream.getData();
                } else {
                    return FileUtils.readFileToByteArray(this.currentOutputStream.getFile());
                }
            } else {
                return ArrayUtils.EMPTY_BYTE_ARRAY;
            }
        }
    }

    /**
     * {@link String} oriented {@link TemporaryDeferredFile}.
     * 
     * @version $Id$
     */
    public class TemporaryDeferredStringFile extends TemporaryDeferredFile
    {
        private final Charset charset;

        /**
         * @param repositoryPath the path of the repository inside the temporary directory
         * @param charset the encoding in which to read the file
         */
        TemporaryDeferredStringFile(String repositoryPath, Charset charset)
        {
            super(repositoryPath);
            this.charset = charset;
        }

        /**
         * @return the reader
         * @throws IOException when failing to create a {@link Reader}
         */
        public Reader getReader() throws IOException
        {
            return new InputStreamReader(getInputStream(), this.charset);
        }

        /**
         * @return the writer
         * @throws IOException when failing to create a {@link Writer}
         */
        public Writer getWriter() throws IOException
        {
            OutputStream stream = getOutputStream();

            return new OutputStreamWriter(stream, this.charset);
        }

        /**
         * @return the content as a {@link String}
         * @throws IOException when failing to read the content
         */
        public String getString() throws IOException
        {
            try (Reader reader = getReader()) {
                StringWriter writer = new StringWriter();

                IOUtils.copyLarge(reader, writer);

                return writer.toString();
            }
        }

        /**
         * @param str the string to write
         * @throws IOException when failing to write the content
         */
        public void setString(String str) throws IOException
        {
            try (Writer writer = getWriter()) {
                IOUtils.write(str, writer);
            }
        }
    }

    /**
     * UID used in unique file name generation.
     */
    private final String uid = UUID.randomUUID().toString().replace('-', '_');

    /**
     * Counter used in unique identifier generation.
     */
    private final AtomicLong counter = new AtomicLong(0);

    @Inject
    private Environment environment;

    /**
     * @param repositoryPath the path of the repository inside the temporary directory
     * @return the folder associated to passed repository path
     * @throws IOException when failing to create the repository
     */
    public File getRepository(String repositoryPath) throws IOException
    {
        File repository = new File(this.environment.getTemporaryDirectory(), repositoryPath);

        if (!repository.mkdirs() && !repository.exists()) {
            throw new IOException("Failed to create directory [" + repository + "]");
        }

        return repository;
    }

    /**
     * @param repositoryPath the path of the repository inside the temporary directory
     * @return a new temporary file in the repository, the returned instance is automatically deleting the file when
     *         it's not used anymore
     * @throws IOException when failing to create the temporary file
     */
    public File createTemporaryFile(String repositoryPath) throws IOException
    {
        StringBuilder filename = new StringBuilder();
        filename.append(this.uid);
        filename.append('-');
        filename.append(this.counter.getAndIncrement());

        return new TemporaryFile(getRepository(repositoryPath), filename.toString());
    }

    /**
     * @param repositoryPath the path of the repository inside the temporary directory
     * @return a new instance of temporary {@link TemporaryDeferredFile}
     */
    public TemporaryDeferredFile createTemporaryDeferredFile(String repositoryPath)
    {
        return new TemporaryDeferredFile(repositoryPath);
    }

    /**
     * @param repositoryPath the path of the repository inside the temporary directory
     * @param charset the encoding in which to read the file
     * @return a new instance of temporary {@link TemporaryDeferredFile}
     */
    public TemporaryDeferredStringFile createTemporaryDeferredStringFile(String repositoryPath, Charset charset)
    {
        return new TemporaryDeferredStringFile(repositoryPath, charset);
    }
}
