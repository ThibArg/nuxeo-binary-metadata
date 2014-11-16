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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.im4java.core.ETOperation;
import org.im4java.core.ExiftoolCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.process.ArrayListOutputConsumer;
import org.nuxeo.binary.metadata.BinaryMetadataConstants.*;
import org.nuxeo.binary.metadata.im4java.StringOutputConsumer;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.runtime.api.Framework;

public class MetadataReader {

    private static Log log = LogFactory.getLog(MetadataReader.class);

    protected String filePath = null;

    protected static int exifToolAvailability = -1;

    protected static String whyExifToolNotAvailable = "";

    protected static int graphicsMagickAvailability = -1;

    protected static String whyGraphicsMagickNotAvailable = "";

    protected static String SYNC_STRING = "MetadataReader - lock";

    public enum TOOL {
        IMAGEMAGICK, EXIFTOOL, GRAPHICSMAGICK
    };

    public MetadataReader(Blob inBlob) throws IOException {

        filePath = "";
        try {
            File f = BlobHelper.getFileFromBlob(inBlob);
            filePath = f.getAbsolutePath();
        } catch (Exception e) {
            filePath = "";
        }

        if (filePath.isEmpty()) {
            File tempFile = File.createTempFile("MDR-", "");
            inBlob.transferTo(tempFile);
            filePath = tempFile.getAbsolutePath();
            tempFile.deleteOnExit();
            Framework.trackFile(tempFile, this);
        }
    }

    public MetadataReader(String inFullPath) {
        filePath = inFullPath;
    }

    protected void checkCommandLines() {
        if (!isExifToolAvailable(false)) {
            log.warn("ExifTool is not available, some command may fail");
        }

        if (!isGraphicsMagickAvailable(false)) {
            log.warn("GraphicsMagick is not available, some command may fail");
        }

    }

    public static boolean isExifToolAvailable(boolean inForceRetry) {

        if (exifToolAvailability == -1 || inForceRetry) {

            try {
                Runtime.getRuntime().exec("exiftool -ver");
                exifToolAvailability = 1;
            } catch (Exception e) {
                exifToolAvailability = 0;
                whyExifToolNotAvailable = e.getMessage();
            }
        }

        return exifToolAvailability == 1;
    }

    public static boolean isGraphicsMagickAvailable(boolean inForceRetry) {

        if (graphicsMagickAvailability == -1 || inForceRetry) {

            try {
                Runtime.getRuntime().exec("gm -version");
                graphicsMagickAvailability = 1;
            } catch (Exception e) {
                graphicsMagickAvailability = 0;
                whyGraphicsMagickNotAvailable = e.getMessage();
            }
        }

        return graphicsMagickAvailability == 1;
    }

    /*
     * If we want to use GraphicsMagick without using the dedictaed im4java
     * class, we must dynamically change a System property. We want to protect
     * the global change to im4java. For non blocking use of GraohicsMagick, use
     * the dedicated classes: GMOperation and GraphicsMagickCmd
     */
    protected synchronized Info getInfo(boolean inUseGM) throws ClientException {

        Properties props = null;
        Info info = null;

        if (inUseGM) {
            props = System.getProperties();
            props.setProperty("im4java.useGM", "true");
        }

        try {
            info = new Info(filePath);
        } catch (InfoException e) {
            throw new ClientException(e);
        } finally {
            if (inUseGM) {
                props.setProperty("im4java.useGM", "false");
            }
        }

        return info;
    }

    /**
     * Wrapper for getAllMetadata(WHICH_TOOL inToolToUse) using ImageMagick
     *
     * @return a string with all the metadata. The string is formated by the
     *         tool used
     * @throws InfoException
     *
     * @since 6.0
     */
    public String getAllMetadata() throws InfoException {
        return getAllMetadata(TOOL.IMAGEMAGICK);
    }

    /**
     * Get all the tags available using <code>identify -verbose</code> for
     * ImageMagick and GraphicsMagick, and the <code>-all</code> tag when used
     * with ExifTool.
     *
     * @param inToolToUse
     * @return a string with all the metadata. The string is formated by the
     *         tool used
     * @throws InfoException
     *
     * @since 6.0
     */
    public String getAllMetadata(TOOL inToolToUse)
            throws ClientException, InfoException {

        String result = "";

        if (inToolToUse == TOOL.EXIFTOOL) {

            HashMap<String, String> r = getMetadataWithExifTool(null);
            Set<String> allKeys = r.keySet();
            for (String oneProp : allKeys) {
                result += oneProp + "=" + r.get(oneProp) + "\n";
            }

        } else {
            Info info = getInfo(inToolToUse == TOOL.GRAPHICSMAGICK);

            Enumeration<String> props = info.getPropertyNames();
            while (props.hasMoreElements()) {
                String propertyName = props.nextElement();
                result += propertyName + "=" + info.getProperty(propertyName)
                        + "\n";
            }
        }

        return result;
    }

    /**
     * If inTheseKeys is null or its length is 0, we return all properties.
     * <p>
     * When used with ImageMagick or GraphicsMagick, the method uses the Info
     * class of im4java.
     * <p>
     * When used with ExifTool it just calls getMetadataWithExifTool() (see this
     * method). Notice the keys are not the same when used with ImageMagick or
     * ExifTool.
     * <p>
     * When a value is returned as null (the key does not exist), it is
     * realigned to the empty string "".
     *
     * @param inTheseKeys
     * @param inToolToUse
     * @return a hash map with the values. A key not found is in the map with a
     *         value of ""
     * @throws ClientException
     *
     * @since 6.0
     */
    public HashMap<String, String> getMetadata(String[] inTheseKeys,
            TOOL inToolToUse) throws ClientException {

        HashMap<String, String> result = new HashMap<String, String>();

        try {
            if (inToolToUse == TOOL.EXIFTOOL) {

                result = getMetadataWithExifTool(inTheseKeys);

            } else {

                Info info = getInfo(inToolToUse == TOOL.GRAPHICSMAGICK);
                if (inTheseKeys == null || inTheseKeys.length == 0) {

                    Enumeration<String> props = info.getPropertyNames();
                    while (props.hasMoreElements()) {
                        String propertyName = props.nextElement();
                        result.put(propertyName, info.getProperty(propertyName));
                    }

                } else {
                    for (String oneProp : inTheseKeys) {
                        String value = "";

                        if (oneProp != null && !oneProp.isEmpty()) {
                            value = info.getProperty(oneProp);
                            if (value == null) {
                                value = "";
                            }
                        }
                        result.put(oneProp, value);
                    }

                    // Handle special case(s)
                    // - Re-align resolution to 72x72 for GIF
                    String keyResolution = KEYS.RESOLUTION;
                    if (result.containsKey(keyResolution)
                            && result.get(keyResolution).isEmpty()) {
                        String format = info.getProperty(KEYS.FORMAT);
                        format = format.toLowerCase();
                        if (format.indexOf("gif") == 0) {
                            result.put(keyResolution, "72x72");
                        }
                    }
                }
            }
        } catch (ClientException e) {
            throw e;
        } finally {

        }
        return result;
    }

    /**
     * Wrapper for getMetadata(String[] inTheseKeys, WHICH_TOOL inToolToUse)
     * using ImageMagick by default.
     *
     * @param inTheseKeys
     * @return a hash map with the values. A key not found is in the map with a
     *         value of ""
     * @throws ClientException
     *
     * @since 6.0
     */
    public HashMap<String, String> getMetadata(String[] inTheseKeys)
            throws ClientException {
        return getMetadata(inTheseKeys, TOOL.IMAGEMAGICK);
    }

    /**
     * Uses ExifTool to extract the XML from the blob.
     *
     * @return the whole XMP as XML String
     * @throws ClientException
     *
     * @since 6.0
     */
    public String getXMP() throws ClientException {

        try {
            ETOperation op = new ETOperation();
            op.getTags("xmp", "b");
            op.addImage();

            // setup command and execute it (capture output)
            StringOutputConsumer output = new StringOutputConsumer();
            ExiftoolCmd et = new ExiftoolCmd();
            et.setOutputConsumer(output);
            et.run(op, filePath);
            return output.getOutput();

        } catch (IOException e) {
            throw new ClientException(e);
        } catch (InterruptedException e) {
            throw new ClientException(e);
        } catch (IM4JavaException e) {
            throw new ClientException(e);
        }
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
     * @since TODO
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

    /**
     * The key is case insensitive but must be one expected by ExifTool. And
     * there are hundreds of them. See ExifTool tags documentation at
     * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/index.html
     *
     * If inTheseKeys is null or its size is 0, we return all values (using the
     * -All tag of ExifTool)
     *
     * @param inTheseKeys
     * @return a hash map with the values. A key not found is in the map with a
     *         value of ""
     * @throws ClientException
     *
     * @since 6.0
     */
    public HashMap<String, String> getMetadataWithExifTool(String[] inTheseKeys)
            throws ClientException {

        HashMap<String, String> result = new HashMap<String, String>();

        try {
            boolean hasKeys = inTheseKeys != null && inTheseKeys.length > 0;

            ETOperation op = new ETOperation();
            if (hasKeys) {
                for (String oneProp : inTheseKeys) {
                    if (oneProp != null && !oneProp.isEmpty()) {
                        op.getTags(oneProp);
                    }
                }
            } else {
                op.getTags("All");
            }

            op.addImage();

            // We don't want the output as Human Readable. We want "ImageWidth",
            // "XResolution", and not "Image Width", "X Resolution" for example
            op.addRawArgs("-s");

            // Run
            ArrayListOutputConsumer output = new ArrayListOutputConsumer();
            ExiftoolCmd et = new ExiftoolCmd();
            et.setOutputConsumer(output);
            et.run(op, filePath);

            // Get the values
            FilterLine fl = new FilterLine();
            ArrayList<String> cmdOutput = output.getOutput();
            for (String line : cmdOutput) {
                fl.setLine(line);
                result.put(fl.getKey(), fl.getValue());
            }

            // Add the not-found values
            if (hasKeys) {
                for (String oneProp : inTheseKeys) {
                    if (!result.containsKey(oneProp)) {
                        result.put(oneProp, "");
                    }
                }
            }

        } catch (IOException | InterruptedException | IM4JavaException e) {
            throw new ClientException(e);
        }

        return result;
    }
}
