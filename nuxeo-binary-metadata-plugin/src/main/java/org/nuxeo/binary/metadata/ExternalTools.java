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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 *
 * @since TODO
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
}
