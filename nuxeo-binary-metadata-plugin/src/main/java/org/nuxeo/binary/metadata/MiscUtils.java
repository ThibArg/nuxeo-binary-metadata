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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * As its name states: Miscellaneous utilities. When the same code was used
 * (copy/paste) in different class, it was safer to move this code in a single
 * place.
 *
 * @since 6.0
 */
public class MiscUtils {

    /**
     * Get the blob from the document. Throws an error only if the xpath is invalid
     *
     * @param inDoc
     * @param inXPath
     * @return
     *
     * @since 6.0
     */
    public static Blob getDocumentBlob(DocumentModel inDoc, String inXPath) throws ClientException {

        Blob theBlob = null;
        try {
            theBlob = (Blob) inDoc.getPropertyValue(inXPath);
        } catch (PropertyException e) {
            if (e.getClass().getSimpleName().equals("PropertyNotFoundException")) {
                throw new ClientException(e);
            }
        }
        return theBlob;
    }
}
