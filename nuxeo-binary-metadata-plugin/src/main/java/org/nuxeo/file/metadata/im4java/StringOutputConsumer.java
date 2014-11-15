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
package org.nuxeo.file.metadata.im4java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.im4java.process.OutputConsumer;

/**
 * Utility class use by exiftool wrappers (mainly), to get the result of a
 * command as a String. im4java already provides an ArrayListOutputConsumer.
 *
 * @since TODO
 */
public class StringOutputConsumer implements OutputConsumer {
    protected String output = "";

    private String charset = null;

    public StringOutputConsumer() {

    }

    public StringOutputConsumer(String inCharset) {
        charset = inCharset;
    }

    public String getOutput() {
        return output;
    }

    public void clear() {
        output = "";
    }

    @Override
    public void consumeOutput(InputStream inStream) throws IOException {
        InputStreamReader isr = null;

        if (charset == null) {
            isr = new InputStreamReader(inStream);
        } else {
            isr = new InputStreamReader(inStream, charset);
        }
        BufferedReader reader = new BufferedReader(isr);
        String line;
        do {
            line = reader.readLine();
            if (line != null) {
                output += line + "\n";
            }
        } while (line != null);

        reader.close();
    }
}
