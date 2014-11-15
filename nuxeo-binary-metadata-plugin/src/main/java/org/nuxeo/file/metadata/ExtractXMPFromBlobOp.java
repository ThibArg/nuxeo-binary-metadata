/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.file.metadata;

import java.io.IOException;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;

/**
 *
 */
@Operation(id = ExtractXMPFromBlobOp.ID, category = Constants.CAT_BLOB, label = "Extract XMP", description = "Use <code>exiftool</code> to extract the XMP datat from the blob. Return the raw XMP as text (empty if there is no XMP metadata) in the <code>varName</code> context variable.")
public class ExtractXMPFromBlobOp {

    public static final String ID = "Blob.ExtractXMP";

    @Context
    protected OperationContext ctx;

    @Param(name = "varName", required = true)
    protected String varName;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) throws IOException {

        String xmp;

        MetadataReader mdr = new MetadataReader(inBlob);
        xmp = mdr.getXMP();

        ctx.put(varName, xmp);

        return inBlob;
    }

}
