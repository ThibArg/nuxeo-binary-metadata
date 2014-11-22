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

import java.io.Serializable;

import org.nuxeo.binary.metadata.MetadataWriter;
import org.nuxeo.binary.metadata.MiscUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Write the metadata passed in the <code>properties</code> parameter. This
 * operation uses <code>ExifTool/code> and only the single <code>=</code>
 * operator. It does not handle other operators supported by ExifTool, such as
 * += or -= for example.
 *
 */
@Operation(id = WriteMetadataToBlobInDocOp.ID, category = Constants.CAT_DOCUMENT, label = "Document: Write Blob Metadata", description = "")
public class WriteMetadataToBlobInDocOp {

    public static final String ID = "Document.WriteBlobMetadata";

    @Context
    protected CoreSession session;

    @Param(name = "properties", required = true)
    protected Properties properties;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) throws ClientException {

        // We do nothing if we don't have the correct kind of document.
        // We could return an error, but we are more generic here,
        // avoiding an hassle to the caller.
        if (inDoc.isImmutable()) {
            return inDoc;
        }

        // Get the blob, do nothing if there is no blob
        Blob theBlob = MiscUtils.getDocumentBlob(inDoc, xpath);
        if (theBlob == null) {
            return inDoc;
        }

        try {
            MetadataWriter mw = new MetadataWriter(theBlob);
            theBlob = mw.writeMetadata(properties, false);
            inDoc.setPropertyValue(xpath, (Serializable) theBlob);
            if (save) {
                session.saveDocument(inDoc);
            }

        } catch (Exception e) {
            throw new ClientException(e);
        }

        return inDoc;
    }

}
