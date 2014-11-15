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
package org.nuxeo.file.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.im4java.core.ETOperation;
import org.im4java.core.ExiftoolCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.process.ArrayListOutputConsumer;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.BlobHelper;
import org.nuxeo.file.metadata.im4java.StringOutputConsumer;
import org.nuxeo.file.metadata.BinaryMetadataConstants.*;
import org.nuxeo.runtime.api.Framework;

public class MetadataReader {

    private static Log log = LogFactory.getLog(MetadataReader.class);

    protected String filePath = null;

    protected static int exifToolAvailability = -1;

    protected static String whyExifToolNotAvailable = "";

    protected static int graphicsMagickAvailability = -1;

    protected static String whyGraphicsMagickNotAvailable = "";

    protected static String SYNC_STRING = "MetadataReader - lock";

    public enum WHICH_TOOL {
        IMAGEMAGICK, EXIFTOOL, GRAPHICSMAGICK
    };

    public MetadataReader(Blob inBlob) throws IOException {

        // We try to directly get the full path of the binary, if possible
        filePath = "";
        try {

            File f = BlobHelper.getFileFromBlob(inBlob);
            filePath = f.getAbsolutePath();

        } catch (Exception e) {
            filePath = "";
        }

        if (filePath.isEmpty()) {
            File tempFile = File.createTempFile("IMDR-", "");
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
        if(!isExifToolAvailable(false)) {
            log.warn("ExifTool is not available, some command may fail");
        }

        if(!isGraphicsMagickAvailable(false)) {
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

    public String getAllMetadata() throws InfoException {
        String result = "";

        Info info = new Info(filePath);

        Enumeration<String> props = info.getPropertyNames();
        while (props.hasMoreElements()) {
            String propertyName = props.nextElement();
            result += propertyName + "=" + info.getProperty(propertyName)
                    + "\n";
        }

        return result;
    }

    /*
     * We want to protect the global change to im4java. For non blocking use of
     * GraohicsMagick, use the dedicated classes: GMOperation and
     * GraphicsMagickCmd
     */
    protected synchronized Info getInfoFromGraphicsMagick()
            throws ClientException {

        Info info = null;
        Properties props = System.getProperties();
        props.setProperty("im4java.useGM", "true");

        try {
            info = new Info(filePath);
        } catch (InfoException e) {
            throw new ClientException(e);
        } finally {
            props.setProperty("im4java.useGM", "false");
        }

        return info;
    }

    /**
     * Wrapper for getMetadata(String[] inTheseKeys, WHICH_TOOL inToolToUse)
     * using ImageMagick by default
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
        return getMetadata(inTheseKeys, WHICH_TOOL.IMAGEMAGICK);
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
            WHICH_TOOL inToolToUse) throws ClientException {

        HashMap<String, String> result = new HashMap<String, String>();

        try {
            if (inToolToUse == WHICH_TOOL.EXIFTOOL) {

                result = getMetadataWithExifTool(inTheseKeys);

            } else {

                Info info = null;

                if (inToolToUse == WHICH_TOOL.GRAPHICSMAGICK) {
                    info = getInfoFromGraphicsMagick();
                } else {
                    info = new Info(filePath);
                }

                if (inTheseKeys == null || inTheseKeys.length == 0) {

                    Enumeration<String> props = info.getPropertyNames();
                    while (props.hasMoreElements()) {
                        String propertyName = props.nextElement();
                        result.put(propertyName,
                                info.getProperty(propertyName));
                    }

                } else {
                    for (String oneProp : inTheseKeys) {
                        String value = "";
                        
                        if(oneProp != null && !oneProp.isEmpty()) {
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
        } catch (NullPointerException | InfoException e) {
            throw new ClientException(e);
        } finally {

        }
        return result;
    }

    /**
     *
     * @return the whole XMP as XML
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

    public class FilterLine {

        protected String line;

        protected String key;

        protected String value;

        public FilterLine() {

        }

        protected void parseLine() {
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
            parseLine();
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
                    if(oneProp != null && !oneProp.isEmpty()) {
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
        } finally {

        }

        return result;
    }

    protected String test() throws IOException, InterruptedException,
            IM4JavaException {
        ETOperation op = new ETOperation();
        // op.getTags("Filename","ImageWidth","ImageHeight","keywords",
        // "Brand_Code", "Title", "Creator", "Description", "Rights");
        // REMEMBER: Tag names are case insensitive with exiftool. So ImageWidth
        // or imagewidth are OK

        /*
         * op.getTags("FileName", "ImageWidth", "ImageHeight", "FileType",
         * "Orientation", "XResolution", "YResolution", "ResolutionUnit");
         * op.getTags("CreatorTool"); op.getTags("Title", "Subject",
         * "Caption-Abstract", "Creator", "Description", "Rights");
         *
         * // Specific XMP-EJ-GALLo op.getTags("Brand_Code", "Brand_Name",
         * "Project_Name");
         */
        op.getTags("all");
        op.addImage();

        // We don't wa nt the output as Human Readable. We want "ImageWidht",
        // "XResolution", and not "Image Width", "X Reslution"
        op.addRawArgs("-s");

        // setup command and execute it (capture output)
        ArrayListOutputConsumer output = new ArrayListOutputConsumer();
        ExiftoolCmd et = new ExiftoolCmd();
        et.setOutputConsumer(output);
        et.run(op, filePath);

        String s = "";
        // dump output
        ArrayList<String> cmdOutput = output.getOutput();
        for (String line : cmdOutput) {
            s += line + "\n";
        }

        return s;
    }

    public String getMetadataWithGM() throws IOException, InterruptedException,
            IM4JavaException {

        String result = "";

        result = test();

        return result;

        /*
         * GMOperation op = new GMOperation(); op.addRawArgs("-verbose");
         * op.addImage(); StringOutputConsumer output = new
         * StringOutputConsumer(); GraphicsMagickCmd cmd = new
         * GraphicsMagickCmd("identify"); cmd.setOutputConsumer(output);
         * cmd.run(op, filePath); String s4 = output.getOutput();
         *
         * String s3 = ""; if(true) { ETOperation op2 = new ETOperation();
         * op2.getTags("iptc:all");//op2.getTags("iptc", "all"); op2.addImage();
         *
         * // setup command and execute it (capture output) StringOutputConsumer
         * output2 = new StringOutputConsumer(); ExiftoolCmd et = new
         * ExiftoolCmd(); et.setOutputConsumer(output2); et.run(op, filePath);
         * s3 = output2.getOutput(); }
         *
         *
         *
         * Info infoIM = new Info(filePath); String s1 = "";
         * Enumeration<String> zeProps = infoIM.getPropertyNames(); while
         * (zeProps.hasMoreElements()) { String propertyName =
         * zeProps.nextElement(); s1 += propertyName + "=" +
         * infoIM.getProperty(propertyName) + "\n"; }
         *
         * Properties props = System.getProperties();
         * props.setProperty("im4java.useGM", "true"); Info infoGM = new
         * Info(filePath); props.setProperty("im4java.useGM", "false");
         *
         *
         * String s2 = ""; zeProps = infoGM.getPropertyNames(); while
         * (zeProps.hasMoreElements()) { String propertyName =
         * zeProps.nextElement(); s2 += propertyName + "=" +
         * infoGM.getProperty(propertyName) + "\n"; }
         */

        // return result;
    }
}
