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

package org.nuxeo.binary.metadata.operations;

import java.io.IOException;
import org.nuxeo.binary.metadata.MetadataWriter;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Write the metadata passed in the <code>properties</code> parameter. This
 * operation uses <code>ExifTool/code> and only the single <code>=</code>
 * operator. It does not handle other operators supported by ExifTool, such as
 * += or -= for example.
 *
 */
@Operation(id = WriteMetadataToBlobOp.ID, category = Constants.CAT_BLOB, label = "Blob: Write Metadata", description = "")
public class WriteMetadataToBlobOp {

    public static final String ID = "Blob.WriteMetadata";

    @Param(name = "properties", required = true)
    protected Properties properties;

    @Param(name = "workOnACopy", required = false, values = { "false" })
    protected boolean workOnACopy = false;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) throws IOException {

        Blob result = inBlob;

        int count = properties.size();
        String[] expressions = new String[count];
        int idx = 0;
        for (String key : properties.keySet()) {
            expressions[idx] = key + "=" + properties.get(key);
            idx += 1;
        }

        try {
            MetadataWriter mw = new MetadataWriter(inBlob);
            result = mw.writeMetadata(expressions, workOnACopy);
        } catch (Exception e) {
            throw new ClientException(e);
        }
        return result;
    }

}
