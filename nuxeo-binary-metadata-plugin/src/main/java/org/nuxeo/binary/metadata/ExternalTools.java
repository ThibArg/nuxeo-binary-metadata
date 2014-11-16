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

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 *
 * @since 6.0
 */
public class ExternalTools {

    public enum TOOL {
        IMAGEMAGICK, EXIFTOOL, GRAPHICSMAGICK
    };

    public static class ToolAvailability {

        private static Log log = LogFactory.getLog(ToolAvailability.class);

        protected static int exifToolAvailability = -1;

        protected static String whyExifToolNotAvailable = "";

        protected static int graphicsMagickAvailability = -1;

        protected static String whyGraphicsMagickNotAvailable = "";

        protected static boolean toolsAvailabilityChecked = false;

        public static void checkAndLogToolsAvailability() {

            if (!toolsAvailabilityChecked) {
                if (!isExifToolAvailable(false)) {
                    log.warn("ExifTool is not available, some command may fail");
                }

                if (!isGraphicsMagickAvailable(false)) {
                    log.warn("GraphicsMagick is not available, some command may fail");
                }
            }

            toolsAvailabilityChecked = true;
        }

        public static boolean isExifToolAvailable() {
            return isExifToolAvailable(false);
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

        public static String whyExifToolIsNotAvailable() {
            return whyExifToolNotAvailable;
        }

        public static boolean isGraphicsMagickAvailable() {
            return isGraphicsMagickAvailable(false);
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

        public static String whyGraphicsMagickIsNotAvailable() {
            return whyGraphicsMagickNotAvailable;
        }
    }

    /**
     *
     * Utility class used to format the expression expected by im4java/exiftool
     * when writing a tag. The format must be:
     * <p>
     * <code>-TAG[+-][<]=Value</code>
     * <p>
     * <i>See ExifTool documentation</i>
     *
     * @since 6.0
     */
    public static class ExifToolTagFormatter {

        protected String tag;

        protected String op;

        protected String value;

        public ExifToolTagFormatter(String inTag, String inValue) {
            this(inTag, "=", inValue);
        }

        public ExifToolTagFormatter(String inTag, String inOp, String inValue) {
            tag = inTag;
            op = inOp;
            value = inValue;
        }

        @Override
        public String toString() {
            return tag + op + value;
        }

        /**
         * Convert with a simple assignment, TAG=VALUE
         *
         * @param inTagsAndValued
         * @return
         *
         * @since 6.0
         */
        public static ExifToolTagFormatter[] convertToTagWriters(
                HashMap<String, String> inTagsAndValued) {
            ExifToolTagFormatter[] finalTags = new ExifToolTagFormatter[inTagsAndValued.size()];

            int idx = 0;
            for (String oneTag : inTagsAndValued.keySet()) {
                finalTags[idx] = new ExifToolTagFormatter(oneTag, "=",
                        inTagsAndValued.get(oneTag));
                idx += 1;
            }

            return finalTags;
        }
    }
}
