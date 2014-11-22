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

package org.nuxeo.binary.metadata.test;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.binary.metadata.MetadataReader;
import org.nuxeo.binary.metadata.MetadataWriter;
import org.nuxeo.binary.metadata.ExternalTools.TOOL;
import org.nuxeo.binary.metadata.operations.ExtractBinaryMetadataInDocumentOp;
import org.nuxeo.binary.metadata.operations.WriteMetadataToBlobInDocOp;
import org.nuxeo.binary.metadata.operations.WriteMetadataToBlobOp;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class,
        EmbeddedAutomationServerFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.picture.core", "nuxeo-binary-metadata",
        "org.nuxeo.ecm.platform.commandline.executor" })
public class MetadataWriterTest {

    protected static final Log log = LogFactory.getLog(MetadataWriterTest.class);

    protected static final String IMAGE_GIF = "images/a.gif";

    protected static final String IMAGE_JPEG = "images/a.jpg";

    protected static final String IMAGE_PNG = "images/a.png";

    protected static final String IMAGE_TIF = "images/a.tif";

    protected static final String NUXEO_LOGO = "images/Nuxeo.png";

    protected static final String WITH_XMP = "images/with-xmp.jpg";

    protected static final String KEYWORDS = "kw1, kw2, otherKW";

    protected File filePNG;

    protected File fileGIF;

    protected File fileTIF;

    protected File fileJPEG;

    protected DocumentModel parentOfTestDocs;

    protected DocumentModel docPNG;

    protected DocumentModel docGIF;

    protected DocumentModel docTIF;

    protected DocumentModel docJPEG;

    protected static int graphicsMagickCheck = -1;

    protected static int exifToolCheck = -1;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService service;

    protected void doLog(String what) {
        System.out.println(what);
    }

    // Not sure it's the best way to get the current method name, but at least
    // it works
    protected String getCurrentMethodName(RuntimeException e) {
        StackTraceElement currentElement = e.getStackTrace()[0];
        return currentElement.getMethodName();
    }

    @Before
    public void setUp() {

        // Setup documents if needed, etc.
        filePNG = FileUtils.getResourceFileFromContext(IMAGE_PNG);
        fileGIF = FileUtils.getResourceFileFromContext(IMAGE_GIF);
        fileTIF = FileUtils.getResourceFileFromContext(IMAGE_TIF);
        fileJPEG = FileUtils.getResourceFileFromContext(IMAGE_JPEG);

        // Cleanup repo if needed and create the Picture documents
        // coreSession.removeChildren(coreSession.getRootDocument().getRef());
        parentOfTestDocs = coreSession.createDocumentModel("/",
                "test-pictures", "Folder");
        parentOfTestDocs.setPropertyValue("dc:title", "test-pictures");
        parentOfTestDocs = coreSession.createDocument(parentOfTestDocs);
        parentOfTestDocs = coreSession.saveDocument(parentOfTestDocs);

        docPNG = createPictureDocument(filePNG);
        docGIF = createPictureDocument(fileGIF);
        docTIF = createPictureDocument(fileTIF);
        docJPEG = createPictureDocument(fileJPEG);
        coreSession.save();
    }

    @After
    public void cleanup() {
        coreSession.removeDocument(parentOfTestDocs.getRef());
        coreSession.save();
    }

    protected DocumentModel createPictureDocument(File inFile) {

        DocumentModel pictDoc = coreSession.createDocumentModel(
                parentOfTestDocs.getPathAsString(), inFile.getName(), "Picture");
        pictDoc.setPropertyValue("dc:title", inFile.getName());
        pictDoc.setPropertyValue("file:content", new FileBlob(inFile));
        return coreSession.createDocument(pictDoc);

    }

    @Test
    public void testSetKeywords() throws Exception {

        doLog(getCurrentMethodName(new RuntimeException()) + "...");

        // Check there is no keywords
        MetadataReader mdr = new MetadataReader(fileJPEG.getAbsolutePath());
        assertEquals("", mdr.readOneMetadata("Keywords", TOOL.EXIFTOOL));

        // Add keywords to a copy of the file
        MetadataWriter mdw = new MetadataWriter(fileJPEG.getAbsolutePath());
        FileBlob result = (FileBlob) mdw.writeMetadata(
                "Keywords=" + KEYWORDS, true);
        assertNotNull(result);
        String resultPath = result.getFile().getAbsolutePath();
        mdr = new MetadataReader(resultPath);
        String kws = mdr.readOneMetadata("Keywords", TOOL.EXIFTOOL);
        assertEquals(KEYWORDS, kws);

        // Now, add a new keyword toe the same file (not a copy)
        // Notice: At this step, we are working on the previous copy (we don't
        // want to alter the original test file)
        mdw = new MetadataWriter(resultPath);
        result = (FileBlob) mdw.writeMetadata("Keywords+=yetAnotherOne", false);
        assertNotNull(result);

        mdr = new MetadataReader(resultPath);
        kws = mdr.readOneMetadata("Keywords", TOOL.EXIFTOOL);
        assertEquals(KEYWORDS + ", yetAnotherOne", kws);
    }

    @Test
    public void testWriteMetadataToBlobOperation() throws Exception {

        doLog(getCurrentMethodName(new RuntimeException()) + "...");

        // Check there is no keywords
        MetadataReader mdr = new MetadataReader(fileJPEG.getAbsolutePath());
        assertEquals("", mdr.readOneMetadata("Keywords", TOOL.EXIFTOOL));

        //Add keywords on a copy, using the operation
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        Properties props = new Properties();
        props.put("Keywords", KEYWORDS);
        OperationChain chain = new OperationChain("testChain");
        chain.add(WriteMetadataToBlobOp.ID)
                    .set("properties", props)
                    .set("workOnACopy",  true);

        ctx.setInput(new FileBlob(fileJPEG));
        Blob result = (Blob) service.run(ctx, chain);
        // For the test, we should have a FileBlob
        assertTrue(result instanceof FileBlob);

        mdr = new MetadataReader(result);
        String kws = mdr.readOneMetadata("Keywords", TOOL.EXIFTOOL);
        assertEquals(KEYWORDS, kws);

        // Now, remove the metadata from previous result
        props = new Properties();
        props.put("Keywords", "");
        chain = new OperationChain("testChain");
        chain.add(WriteMetadataToBlobOp.ID)
                    .set("properties", props)
                    .set("workOnACopy",  true);
        ctx.setInput(result);
        result = (Blob) service.run(ctx, chain);
        mdr = new MetadataReader(result);
        kws = mdr.readOneMetadata("Keywords", TOOL.EXIFTOOL);
        assertEquals("", kws);
    }

    @Test
    public void testWriteMetadataToBlobInDocOperation() throws Exception {

        doLog(getCurrentMethodName(new RuntimeException()) + "...");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        Properties props = new Properties();
        props.put("Keywords", KEYWORDS);
        OperationChain chain = new OperationChain("testChain");
        chain.add(WriteMetadataToBlobInDocOp.ID)
                    .set("properties", props)
                    .set("save",  true); // let the default xpath value

        ctx.setInput(docJPEG);
        DocumentModel result = (DocumentModel) service.run(ctx, chain);

        // Test we have the keywords
        props = new Properties();
        props.put("dc:description", "Keywords");
        chain.add(ExtractBinaryMetadataInDocumentOp.ID).set("tool",
                "ExifTool").set("properties", props).set("save", false);// No save
        ctx.setInput(result);
        result = (DocumentModel) service.run(ctx, chain);
        String kws = (String) result.getPropertyValue("dc:description");
        assertEquals(KEYWORDS, kws);

    }
}
