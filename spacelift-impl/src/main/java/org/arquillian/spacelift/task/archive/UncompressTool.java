/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arquillian.spacelift.task.archive;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.arquillian.spacelift.task.Task;
import org.arquillian.spacelift.task.text.ReplacementTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uncompress Tool
 *
 * @author <a href="asotobu@gmail.com">Alex Soto</a>
 */
public abstract class UncompressTool extends Task<File, File> {
    private final static Logger LOGGER = LoggerFactory.getLogger(UncompressTool.class);

    protected static final String CUT_DIR_PATTERN = "^/?([^/]+)/(.*)";
    protected static final String CUT_DIR_REPLACEMENT = "$2";

    private static final int BUFFER = 2048;

    private List<ReplacementTuple<?>> replacements = new ArrayList<ReplacementTuple<?>>();

    private File dest;

    protected abstract ArchiveInputStream compressedInputStream(InputStream compressedFile);

    protected abstract int permissionsMode(ArchiveEntry archiveEntry);

    /**
     * @param pathToDestination
     *     destination where to uncompress a file
     */
    public UncompressTool toDir(String pathToDestination) {
        return toDir(new File(pathToDestination));
    }

    /**
     * @param destination
     *     destination where to uncompress a file
     */
    public UncompressTool toDir(File destination) {
        this.dest = destination;
        return this;
    }

    /**
     * Applies renaming for entries in compressed file. Uses standard Java regex notation.
     * <p>
     * Also ensures that prior replacement all filesystem delimiters are mapped to forward slash '/'.
     * You can define multiple patterns, if so, they are applied in order there are specified.
     *
     * @param pattern
     *     pattern to be find in archive
     * @param replacement
     *     replacement of the path in archive
     */
    public ReplacementTuple<UncompressTool> remap(String pattern) {
        ReplacementTuple<UncompressTool> replacement = new ReplacementTuple<UncompressTool>(this, pattern);
        replacements.add(replacement);
        return replacement;
    }

    /**
     * Applies renaming for entries in compressed file by removing the first directory in there.
     */
    public UncompressTool cutdirs() {
        ReplacementTuple<UncompressTool> replacement = new ReplacementTuple<UncompressTool>(this, CUT_DIR_PATTERN);
        replacement.with(CUT_DIR_REPLACEMENT);
        replacements.add(replacement);
        return this;
    }

    @Override
    protected File process(File input) throws Exception {
        LOGGER.trace(String.format("input exists: %s",
                input.exists()));
        ArchiveEntry entry = null;

        /** Read entries using the getNextEntry method **/

        ArchiveInputStream compressedInputStream = compressedInputStream(new FileInputStream(input));

        while ((entry = compressedInputStream.getNextEntry()) != null) {

            File file = new File(this.dest, remapEntryName(entry.getName()));
            LOGGER.trace(String.format("entry file: %s",
                    file.getAbsolutePath()));
            LOGGER.trace(String.format("entry file's parent exists: %s",
                    file.getParentFile().exists()));

            if (entry.isDirectory()) {
                LOGGER.trace("creating entry file because it's a directory");
                FileUtils.forceMkdir(file);
            } else {
                LOGGER.trace("entry file is not a directory, assuming file");
                if (!file.getParentFile().exists()) {
                    LOGGER.trace("creating inexisting parent directories of entry file");
                    FileUtils.forceMkdir(file.getParentFile());
                }

                int count;
                byte data[] = new byte[BUFFER];

                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = compressedInputStream.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.close();

                int permissionsMode = permissionsMode(entry);
                if (permissionsMode != 0) {
                    FilePermission filePermission = PermissionsUtil.toFilePermission(permissionsMode);
                    PermissionsUtil.applyPermission(file, filePermission);
                }
            }
        }

        compressedInputStream.close();

        return this.dest;
    }

    private String remapEntryName(String entryName) {

        if (entryName == null) {
            return entryName;
        }

        String finalName = entryName.replaceAll("\\\\", "/");
        for (ReplacementTuple<?> remap : replacements) {
            finalName = finalName.replaceAll(remap.getRegex(), remap.getReplacement());
        }
        return finalName;
    }
}
