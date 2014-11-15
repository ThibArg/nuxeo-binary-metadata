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

public class BinaryMetadataConstants {

    /*
     * Some common keys.
     *
     * WARNING: The value of each item here must match (case sensitive) the
     * parameter of the im4java Info#getProperty method. When a property does
     * not exist, the returned value is null ("Resolution" for a gif for
     * example)
     */
    public class KEYS {
        public static final String WIDTH = "Width";

        public static final String HEIGHT = "Height";

        public static final String COLORSPACE = "Colorspace";

        public static final String RESOLUTION = "Resolution";

        public static final String UNITS = "Units";

        public static final String FORMAT = "Format";
    }

    public static final String[] DEFAULT_KEYS = { KEYS.WIDTH, KEYS.HEIGHT,
            KEYS.COLORSPACE, KEYS.RESOLUTION, KEYS.UNITS };

    public class RESOLUTION_UNITS {
        public static final String PIXELS_PER_CENTIMETER = "PixelsPerCentimeter";

        public static final String PIXELS_PER_INCH = "PixelsPerInch";

        public static final String UNDEFINED = "Undefined";
    }
}
