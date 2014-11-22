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

package org.nuxeo.binary.metadata.operations;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.im4java.core.InfoException;
import org.nuxeo.binary.metadata.MetadataReader;
import org.nuxeo.binary.metadata.MiscUtils;
import org.nuxeo.binary.metadata.ExternalTools.TOOL;
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
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * Extract the metadata from the binary stored in the <code>xpath</code> field.
 * <p>
 * <code>properties</code> contains a list of <code>xpath=Metadata Key</code>
 * where Metadata Key is the exact name (case sensitive) of a property to
 * retrieve. For example:
 *
 * <pre>
 * <code>dc:format=Format</code>
 * <code>dc:format=Format</code>
 * <code>dc:format=Format</code>
 * </pre>
 * <p>
 * If you use ExifTool, remember the tags are not the same as in ImageMagick.
 * Also, even if ExifTool is not case sensitive for the tags, this operation is,
 * because it handles key=value
 * <p>
 * Also, there is a special property: If you pass
 * <code>schemaprefix:field=all</code>, then all the properties are returned
 * (the field must be a String field)
 * <p>
 *
 */
@Operation(id = ExtractBinaryMetadataInDocumentOp.ID, category = Constants.CAT_DOCUMENT, label = "Document: Extract Binary Metadata", description = "Extract the metadata from the file stored in the <code>xpath</code> field. <code>properties</code> (optional) contains a list of <code>xpath=Metadata Key</code> where Metadata Key is the exact name (case sensitive) of a property to retrieve. For example: <code>dc:format=Format</code>If <code>properties</code> is not used, the operation extracts <code>width</code>, <code>height</code>, <code>resolution</code> and <code>colorspace</code> from the picture file, and save the values in the <code>image_metadata</code> schema (the DPI is realigned if needed.)There is a special property: If you pass <code>schemaprefix:field=all</code>, then all the properties are returned (the field must be a String field)")
public class ExtractBinaryMetadataInDocumentOp {

    public static final String ID = "Document.ExtractBinaryMetadata";

    private static final Log log = LogFactory.getLog(ExtractBinaryMetadataInDocumentOp.class);

    @Context
    protected CoreSession session;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "properties", required = true)
    protected Properties properties;

    @Param(name = "tool", required = false, widget = Constants.W_OPTION, values = {
            "ImageMagick", "GraphicsMagick", "ExifTool" })
    String tool = "ImageMagick";

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) throws ClientException,
            IOException, InfoException {

        // We do nothing if we don't have the correct kind of document.
        // We could return an error, but we are more generic here,
        // avoiding an hassle to the caller.
        if (inDoc.isImmutable()) {
            return inDoc;
        }

        TOOL toolToUse = null;
        switch (tool.toLowerCase()) {
        case "graphicsmagick":
            toolToUse = TOOL.GRAPHICSMAGICK;
            break;

        case "exiftool":
            toolToUse = TOOL.EXIFTOOL;
            break;

        default:
            toolToUse = TOOL.IMAGEMAGICK;
            break;
        }


        // Get the blob, do nothing if there is no blob
        Blob theBlob = MiscUtils.getDocumentBlob(inDoc, xpath);
        if (theBlob == null) {
            return inDoc;
        }

        MetadataReader imdr = new MetadataReader(theBlob);
        HashMap<String, String> result = null;
        String xpathForAll = "";

        // The names of the properties are stored as values in the map
        String[] keysStr = null;
        if (properties != null && properties.size() > 0) {
            keysStr = new String[properties.size()];
            int idx = 0;
            for (String inXPath : properties.keySet()) {
                keysStr[idx] = properties.get(inXPath);
                if (keysStr[idx].toLowerCase().equals("all")) {
                    xpathForAll = inXPath;
                }

                idx += 1;
            }
        }

        boolean ok = true;
        try {
            result = imdr.readMetadata(keysStr, toolToUse);
        } catch (Exception e) {
            // Just ignore if the tool can't read the file (a .csv for
            // example)
            log.error(
                    "Error reading the metadata for document id "
                            + inDoc.getId(), e);
            ok = false;
        }

        if (ok) {
            for (String inXPath : properties.keySet()) {
                String value = result.get(properties.get(inXPath));

                String theType = utils_getBasePropertyType(inDoc.getProperty(inXPath));
                if (theType.equals("int") || theType.equals("long")) {
                    if (value.isEmpty()) {
                        value = "0";
                    }
                    long v = Math.round(Double.valueOf(value));
                    value = "" + v;
                } else if (theType.equals("float")) {
                    if (value.isEmpty()) {
                        value = "0.0";
                    }
                    float v = Float.valueOf(value);
                    value = "" + v;
                } else if (theType.equals("double")) {
                    if (value.isEmpty()) {
                        value = "0.0";
                    }
                    double v = Double.valueOf(value);
                    value = "" + v;
                }

                inDoc.setPropertyValue(inXPath, value);
            }

            if (!xpathForAll.isEmpty()) {
                inDoc.setPropertyValue(xpathForAll, imdr.readAllMetadata());
            }

            // Save the document
            if (save) {
                session.saveDocument(inDoc);
            }
        }

        return inDoc;
    }

    protected String utils_getBasePropertyType(Property inProp) {

        Type t = inProp.getType();
        String theType;

        do {
            theType = t.getName();
            t = t.getSuperType();
        } while (t != null);

        return theType;
    }

}
