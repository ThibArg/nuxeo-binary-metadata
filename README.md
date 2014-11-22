nuxeo-binary-metadata
===========================

This plug-in allows to use ImageMagick, GraphicsMagic or ExifTool to _read_ any binary metadata, and ExifTool to _write_ metadata

## About - Requirements
`nuxeo-binary-metadata` is a plug-in for the `nuxeo platform`. It allows to read/write metadata stored in blobs (basically, a file on disk), letting nuxeo developers to store these information in the document, for easy search, display and reporting. It uses the `ìm4java` tool for this purpose, which, itself, encapsulates calls to `ImageMagick` and, possibly, `ExifTool` and `GraphicsMagick`.

Each tool has a home page and documentation explaining the usage of tags.

*WARNING* Tags are not the same depending on the tool you use.

**Requirements**:
* For __reading__ metadata, the plug-in lets you pickup the tool you want to use, so make sure this tool is installed on your server. It is either `ImageMagick`, `GraphicsMagic` or `ExifTool`. Actually, you can use the 3 of them if you wish, and switch from one to another when reading metadata (for example, maybe one tool can read a very specific metadata that the others cant')
* For __writing__ metadata, only `ExifTool` is used, so if you want to write metadata, you must have `ExifTool` installed on your server.

## Table of Content

* [Operations](#operation)
  * [`Document: Extract Binary Metadata`](#document-extract-binary-metadata)
  * [`Blob: Extract XMP`](#blob-extract-xmp)
  * [`Blob: Write Metadata`](#`blob-write-metadata`)
  * [`Document: Write Blob Metadata`](#document-write-blob-metadata)
* [Build-Install](#build-install)
* [Third Party Tools Used](#third-party-tools-used)
* [License](#license)
* [About Nuxeo](#about-nuxeo)


## Operations
### `Document: Extract Binary Metadata`
Receives a `Document` as input and put the requested metadata in fields, and optionally save the document.

#### Parameters
* `xpath` is the xpath to the binary, in the document. It is set by default to `file:content`, which means the default main binary.
* When the `save` box is checked then the document will be automatically saved. Not checking this box is interesting when the next operations, for example, will also update some fields (we some time want to avoid saving the document in the database, triggering events, etc.)
* `tool` lets you select which tool must be used: ImageMagick (default), GraphicsMagick or ExifTool.
* The `properties` parameter is a list a `key=value` elements (separated by a line), where `key` is the XPATH of a field and `value` is the exact name (case sensitive) of a picture metadata field, as returned by too used.
  * When used with `ImageMagick`/`GraphicsMagick` tool, the plug-in calls `identify -verbose` command. Sub-properties use a colon as separator (`image statistics:Overall:standard deviation` for example).
  * With `ExifTool`, the plug-in calls the `-all` tag.
  * *WARNING* Tags are not the same depending on the tool you use

Here is an example of properties used with ImageMagick:
```
dc:format=Format
a_schema_prefix:a_field=Units
another_schema_prefix:some_field=Number pixels
my_channel:red=Channel depth:red
my_channel:red=Channel depth:green
my_channel:red=Channel depth:blue
... etc ...
```
In this example, the plugin will store in `dc:format` the value of the `Format` field, in `my_channel:red` the value of `Channel depth:red`, etc.

* **Special value**<br/>
 * `all`: If a `value` is set to "all" (`dc:description=all` for example), then all the available values are returned as a raw result, formatted by the tool used. This is a good way to check what kind of values you can expect. The whole values could also be stored in a string field and full-text indexed.


### `Blob: Extract XMP`
Receives a `Blob` as input and fill a context variable which will be filled with all the XMP metadata (as a XML string)

**WARNING**: This operation uses `ExifTool` (no choice in the tool to use).

#### Parameters
* Expects one required parameter, `varName`, which will be filled with the raw XML of the XMP metadata stored in the blob. If the blob has no XMP metadata, the variable is set to the empty string, "".

An example of Automation Chain using this operation would be:

```
Fetch > Context Document(s)
Files > Get Document File
Files > Extract XMP
  varName: theXMP
. . .
```
The `theXMP` context variable be used. For example, an `MVEL` expression could check it is is empty (no XMP in the file) or not: `@{theXMP.isEMpty()}`


### `Blob: Write Metadata`
Receives a `Blob` as input and set the its metadata using a `properties` list.

**WARNING**: This operation uses `ExifTool` (no choice in the tool to use).

#### Parameters
* `workOnACopy`: If `ture`, the blob is first duplicated. The metadata is written in this copy, and it is this copy that is returned by the operation. If set to `false`, then the blob received as input is modified and returned.
* `properties` is a list a `key=value` elements (separated by a line), where `key` is the name of the tag and `value` is, well, the value so set.
  * Notice that setting an empty value removes the tag (`keywords=` for example)
  * An example of use could be:
```
Keywords=nuxeo, awesome, amazing
Title=How cool is nuxeo
Subject=cf. the title
```

### `Document: Write Blob Metadata`
Receives a `Document` as input and sets the metadata of its embedded blob
#### Parameters
* `xpath` if the xpath of the field holding the binary. Default value is `file:content`
* When the `save` box is checked then the document will be automatically saved. Not checking this box is interesting when the next operations, for example, will also update some fields (we some time want to avoid saving the document in the database, triggering events, etc.)
* * `properties` is a list a `key=value` elements (separated by a line), where `key` is the name of the tag and `value` is, well, the value so set.
  * Notice that setting an empty value removes the tag (`keywords=` for example)
* See the example for `Blob: Write Metadata`

## Build-Install

Assuming [`maven`](http://maven.apache.org) (min. 3.2.1) is installed on your computer:
```
# Clone the GitHub repository
cd /path/to/where/you/want/to/clone/this/repository
git clone https://github.com/ThibArg/nuxeo-binary-metadata
# Compile
cd nuxeo-binary-metadata
mvn clean install
```

* The plug-in is in `nuxeo-binary-metadata/nuxeo-binary-metadata-plugin/target/`, its name is `nuxeo-binary-metadata-plugin-{version}.jar`.
* The Marketplace Package is in `nuxeo-binary-metadata/nuxeo-binary-metadata-mp/target`, its name is `nuxeo-binary-metadata-mp-{version}.zip`.

If you want to import the source code in Eclipse, then after the first build, `cd nuxeo-binary-metadata-plugin` and `mvn eclipse:eclipse`. Then, in Eclipse, choose "File" > "Import...", select "Existing Projects into Workspace" navigate to the `nuxeo-binary-metadata-plugins` folder and select this folder.



## Third Party Tools Used
* **`im4java`**<br/>
`im4java` (http://im4java.sourceforge.net) is used as the main tool to extract the metadata from the images. It is a very good and powerful tool which saves a lot of development time by having already everything requested.<br/>
`im4java` is licensed under the LGPL

* Other tools depend up on you: `ImageMagick`, `GraphicsMagic` and `ExifTool`.

## License
(C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
Thibaud Arguillere (https://github.com/ThibArg)

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com) and packaged applications for Document Management, Digital Asset Management and Case Management. Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.
