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

import java.io.File;
import java.io.IOException;

import org.im4java.core.ETOperation;
import org.im4java.core.ExiftoolCmd;
import org.im4java.core.IM4JavaException;
import org.nuxeo.binary.metadata.ExternalTools.ExifToolTagFormatter;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * As of "today" (2014-11), the class only uses ExifTool to write metadata.
 * <p>
 * Ultimately, the class calls im4java which calls ExitTool to set the tag. So,
 * the expected, final, syntax to set a tag is:
 * <p>
 * <code>TAG[+-][<]=Value</code>
 * <P>
 * (See ExifTool documentation)
 *
 * @since 6.0
 */
public class MetadataWriter extends AbstractMetadataReadWrite {

    public MetadataWriter(Blob inBlob) throws IOException {

        super.updateFilePath(inBlob);

        ExternalTools.ToolAvailability.checkAndLogToolsAvailability();
    }

    public MetadataWriter(String inFullPath) {

        filePath = inFullPath;
        ExternalTools.ToolAvailability.checkAndLogToolsAvailability();
    }

    /**
     * Main entry point. Each expression must have the
     * <code>TAG[+-][<]=Value</code> syntax
     *
     * @param inExpressions
     * @param inWorkOnCopy
     * @return
     *
     * @since 6.0
     */
    public Blob writeMetadata(String[] inExpressions, boolean inWorkOnCopy) {

        Blob result = null;

        try {
            String destPath;
            if (inWorkOnCopy) {
                destPath = duplicateFile();
            } else {
                destPath = filePath;
            }

            ETOperation op = new ETOperation();
            for (String oneExpression : inExpressions) {
                op.setTags(oneExpression);
            }
            op.addImage();

            ExiftoolCmd et = new ExiftoolCmd();
            et.run(op, destPath);

            if (inWorkOnCopy || originalBlob == null) {
                result = new FileBlob(new File(destPath));
            } else {
                result = originalBlob;
            }

        } catch (IOException | InterruptedException | IM4JavaException e) {
            throw new ClientException(e);
        }

        return copyOriginalBlobInfoToBlob(result);

    }

    /**
     * Utility wrapper for writeMetadata(String[] inExpressions, boolean
     * inWorkOnCopy), using ecm.automation.core.util.Properties, as received in
     * a parameter of an Automation Chain for exampel
     *
     * @param inProps - WARNING: These are
     *            org.nuxeo.ecm.automation.core.util.Properties, not
     *            java.utils.Properties
     * @param inWorkOnCopy
     * @return
     * @throws ClientException
     *
     * @since TODO
     */
    public Blob writeMetadata(Properties inProps, boolean inWorkOnCopy)
            throws ClientException {

        int count = inProps.size();
        String[] expressions = new String[count];
        int idx = 0;
        for (String key : inProps.keySet()) {
            expressions[idx] = key + "=" + inProps.get(key);
            idx += 1;
        }

        return this.writeMetadata(expressions, inWorkOnCopy);
    }

    /**
     * Utility wrapper for writeMetadata(String[] inExpressions, boolean
     * inWorkOnCopy)
     *
     * @param inMetadata
     * @param inWorkOnCopy
     * @return
     * @throws ClientException
     *
     * @since 6.0
     */
    public Blob writeMetadata(ExifToolTagFormatter[] inMetadata,
            boolean inWorkOnCopy) throws ClientException {

        int count = inMetadata.length;
        int max = count - 1;
        String[] expressions = new String[count];
        for (int i = 0; i < max; i++) {
            expressions[i] = inMetadata[i].toString();
        }

        return this.writeMetadata(expressions, inWorkOnCopy);
    }

    /**
     * Utility wrapper for writeMetadata(String[] inExpressions, boolean
     * inWorkOnCopy)
     *
     * @param inExpression
     * @param inWorkOnCopy
     * @return
     * @throws ClientException
     *
     * @since 6.0
     */
    public Blob writeMetadata(String inExpression, boolean inWorkOnCopy)
            throws ClientException {

        String[] expressions = { inExpression };

        return this.writeMetadata(expressions, inWorkOnCopy);
    }
}
