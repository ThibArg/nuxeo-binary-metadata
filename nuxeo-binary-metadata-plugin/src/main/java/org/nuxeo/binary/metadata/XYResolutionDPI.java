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
 *     thibaud
 */
package org.nuxeo.binary.metadata;

import org.nuxeo.binary.metadata.BinaryMetadataConstants.RESOLUTION_UNITS;

/*
 * Utility to split the "nnnxnnn" into 2 values expressed in DPI
 * It is mainly about converting from PixelsPerCentimeter to PerInch
 * 1 pixel/centimeter  =  2.54 dot/inch
 *
 * inCurrentUnit must be one of BinaryMetadataConstants.RESOLUTION_UNITS
 */
public class XYResolutionDPI {
    protected int x;

    protected int y;

    public XYResolutionDPI(String inResolution, String inCurrentUnit) {
        x = y = 0;

        if (inResolution != null && !inResolution.isEmpty()) {
            double dx = 0.0, dy = 0.0;
            String[] values = inResolution.split("x");
            switch (values.length) {
            case 1:
                dx = Double.parseDouble(values[0]);
                dy = dx;
                break;
            case 2:
                dx = Double.parseDouble(values[0]);
                dy = Double.parseDouble(values[1]);
                break;
            }

            convertIfNeeded(dx, dy, inCurrentUnit);
        }
    }

    public XYResolutionDPI(double inX, double inY, String inCurrentUnit) {
        convertIfNeeded(inX, inY, inCurrentUnit);
    }

    protected void convertIfNeeded(double inX, double inY, String inCurrentUnit) {
        if (inCurrentUnit != null
                && inCurrentUnit.equals(RESOLUTION_UNITS.PIXELS_PER_CENTIMETER.toString())) {
            inX *= 2.54;
            inY *= 2.54;
        }
        // We don't convert if the unit is already DPI or is Undefined

        x = (int) Math.round(inX);
        y = (int) Math.round(inY);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
