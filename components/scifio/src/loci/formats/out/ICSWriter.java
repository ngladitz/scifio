/*
 * #%L
 * OME SCIFIO package for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2005 - 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package loci.formats.out;

import java.io.IOException;

import ome.scifio.formats.ICSFormat;

import loci.formats.FormatException;
import loci.formats.SCIFIOFormatWriter;
import loci.legacy.context.LegacyContext;

/**
 * ICSWriter is the file format writer for ICS files.  It writes ICS version 1
 * and 2 files.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/bio-formats/src/loci/formats/out/ICSWriter.java">Trac</a>,
 * <a href="http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/bio-formats/src/loci/formats/out/ICSWriter.java;hb=HEAD">Gitweb</a></dd></dl>
 * 
 * @deprecated Use ome.scifio.formats.ICSFormat instead.
 */
@Deprecated
public class ICSWriter extends SCIFIOFormatWriter {

  // -- Fields --

  // -- Constructor --

  public ICSWriter() {
    super("Image Cytometry Standard", new String[] {"ids", "ics"});
    format = LegacyContext.getSCIFIO().formats().getFormatFromClass(ICSFormat.class);
    try {
      writer = format.createWriter();
    }
    catch (ome.scifio.FormatException e) {
      LOGGER.warn("Failed to create APNGFormat components");
    }
  }

  // -- ICSWriter API methods --

  /**
   * Set the order in which dimensions should be written to the file.
   * Valid values are specified in the documentation for
   * {@link loci.formats.IFormatReader#getDimensionOrder()}
   *
   * By default, the ordering is "XYZTC".
   */
  public void setOutputOrder(String outputOrder) {
    ((ICSFormat.Writer)writer).setOutputOrder(outputOrder);
  }

  // -- IFormatWriter API methods --

  /*
   * @see loci.formats.IFormatWriter#saveBytes(int, byte[], int, int, int, int)
   */
  public void saveBytes(int no, byte[] buf, int x, int y, int w, int h)
    throws FormatException, IOException
  {
    try {
      writer.savePlane(getSeries(), no, planeCheck(buf, x, y, w, h), x, y, w, h);
    } catch (ome.scifio.FormatException e) {
      throw (FormatException)e;
    }
  }

  /* @see loci.formats.IFormatWriter#canDoStacks() */
  public boolean canDoStacks() { return writer.canDoStacks(); }

  /* @see loci.formats.IFormatWriter#getPixelTypes(String) */
  public int[] getPixelTypes(String codec) {
    return writer.getPixelTypes();
  }

  // -- IFormatHandler API methods --

  /* @see loci.formats.IFormatHandler#close() */
  public void close() throws IOException {
    super.close();
    ((ICSFormat.Writer)writer).close(getSeries());
  }

}
