/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package org.nuxeo.binary.metadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @since 6.0
 */
public abstract class AbstractMetadataReadWrite {

    protected String filePath = null;

    protected Blob originalBlob = null;

    protected void updateFilePath(Blob inBlob) throws IOException {

        originalBlob = inBlob;
        try {
            File f = BlobHelper.getFileFromBlob(originalBlob);
            filePath = f.getAbsolutePath();
        } catch (Exception e) {
            filePath = "";
        }

        if (filePath.isEmpty()) {
            File tempFile = File.createTempFile("MDRW-", "");
            originalBlob.transferTo(tempFile);
            filePath = tempFile.getAbsolutePath();
            tempFile.deleteOnExit();
            Framework.trackFile(tempFile, this);
        }
    }

    protected String duplicateFile() throws IOException {

        File tempFile = File.createTempFile("MDRW-", "");
        Files.copy(Paths.get(filePath), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Framework.trackFile(tempFile, this);

        return tempFile.getAbsolutePath();
    }

    protected Blob copyOriginalBlobInfoToBlob(Blob inBlob) {

        if(originalBlob != null) {
            inBlob.setFilename(originalBlob.getFilename());
            inBlob.setMimeType(originalBlob.getMimeType());
            inBlob.setEncoding(originalBlob.getEncoding());
        }

        return inBlob;
    }

    /**
     * Utility class to parse a line returned by a "get tag" command line
     * (identify, ...). Usually, such info is returned in kind-of formatted
     * text. For example:
     *
     * <pre>
     * File Type                       : TIFF
     * MIME Type                       : image/tiff
     * Exif Byte Order                 : Little-endian (Intel, II)
     * Image Width                     : 2344
     * Image Height                    : 7200
     * </pre>
     *
     * This class parses the line, extracting key and value
     *
     * @since 6.0
     */
    public class FilterLine {

        protected String line;

        protected String key;

        protected String value;

        public FilterLine() {
            line = "";
        }

        protected void parse() {
            key = null;
            value = "";

            String[] splitted = line.split(":", 2);
            if (splitted.length > 0) {
                key = splitted[0].trim();
                if (splitted.length > 1) {
                    value = splitted[1].trim();
                }
            }
        }

        public void setLine(String inLine) {
            line = inLine;
            parse();
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
