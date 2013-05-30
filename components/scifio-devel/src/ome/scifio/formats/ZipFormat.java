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

package ome.scifio.formats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.scijava.plugin.Plugin;

import net.imglib2.display.ColorTable;
import ome.scifio.AbstractFormat;
import ome.scifio.AbstractMetadata;
import ome.scifio.AbstractParser;
import ome.scifio.ByteArrayPlane;
import ome.scifio.ByteArrayReader;
import ome.scifio.FormatException;
import ome.scifio.HasColorTable;
import ome.scifio.ImageMetadata;
import ome.scifio.Plane;
import ome.scifio.SCIFIO;
import ome.scifio.io.IRandomAccess;
import ome.scifio.io.RandomAccessInputStream;
import ome.scifio.io.ZipHandle;

/**
 * Reader for Zip files.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/bio-formats/src/loci/formats/in/ZipReader.java">Trac</a>,
 * <a href="http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/bio-formats/src/loci/formats/in/ZipReader.java;hb=HEAD">Gitweb</a></dd></dl>
 */
@Plugin(type = ZipFormat.class)
public class ZipFormat extends AbstractFormat {

  // -- Format API Methods --
  
  /*
   * @see ome.scifio.Format#getFormatName()
   */
  public String getFormatName() {
    return "Zip";
  }

  /*
   * @see ome.scifio.Format#getSuffixes()
   */
  public String[] getSuffixes() {
    return new String[]{"zip"};
  }

  // -- Nested classes --
  
  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Metadata extends AbstractMetadata implements HasColorTable {
    
    // -- Fields --

    private ome.scifio.Metadata metadata;
    
    private List<String> mappedFiles = new ArrayList<String>();
    
    // -- ZipMetadata methods --
    
    public List<String> getMappedFiles() {
      return mappedFiles;
    }
    
    public void setMetadata(ome.scifio.Metadata m) throws IOException {
      if (metadata != null) metadata.close();
      
      metadata = m;
    }
    
    // -- HasColorTable API methods --
    
    /*
     * @see ome.scifio.HasColorTable#getColorTable()
     */
    public ColorTable getColorTable(int imageIndex, int planeIndex) {
      if (HasColorTable.class.isAssignableFrom(metadata.getClass()))
        return ((HasColorTable)metadata).getColorTable(0, 0);
      return null;
    }

    // -- Metadata API Methods -- 
    
    /*
     * @see ome.scifio.Metadata#populateImageMetadata()
     */
    public void populateImageMetadata() {
      // clears existing metadata
      createImageMetadata(0);
      
      // copies the delegate's image metadata
      for (ImageMetadata meta : metadata.getAll())
        add(meta);
    }

    @Override
    public void close(boolean fileOnly) throws IOException {
      for (String name : mappedFiles) {
        IRandomAccess handle = scifio().location().getMappedFile(name);
        scifio().location().mapFile(name, null);
        if (handle != null) {
          handle.close();
        }
      }
      mappedFiles.clear();
      
      super.close(fileOnly);
      
      if (metadata != null) metadata.close(fileOnly);
      if (!fileOnly) metadata = null;
      
      mappedFiles = new ArrayList<String>();
    }
  }

  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Parser extends AbstractParser<Metadata> {

    @Override
    public Metadata parse(RandomAccessInputStream stream, Metadata meta)
      throws IOException, FormatException
    {
      return super.parse(ZipUtilities.getRawStream(scifio(), stream), meta);
    }
    
    @Override
    protected void typedParse(RandomAccessInputStream stream, Metadata meta)
      throws IOException, FormatException
    {
      String baseId = ZipUtilities.unzipId(scifio(), stream, meta.getMappedFiles());
      stream.close();
      
      ome.scifio.Parser p = scifio().format().getFormat(baseId).createParser();
      p.setOriginalMetadataPopulated(isOriginalMetadataPopulated());
      p.setMetadataFiltered(isMetadataFiltered());
      p.setMetadataOptions(getMetadataOptions());
      ome.scifio.Metadata m = p.parse(baseId);
      
      meta.setMetadata(m);
    }
  }
  
  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Reader extends ByteArrayReader<Metadata> {

    // -- Fields --
    
    private ome.scifio.Reader reader;
    
    // -- Reader API Methods --
    
    @Override
    public void setSource(RandomAccessInputStream stream) throws IOException {
      super.setSource(ZipUtilities.getRawStream(scifio(), stream));
      
      if (reader != null) reader.close();
      
      String baseId = ZipUtilities.unzipId(scifio(), stream, null);

      try {
        reader = scifio().format().getFormat(baseId).createReader();
        reader.setSource(baseId);
      } catch (FormatException e) {
        LOGGER.error("Failed to set delegate Reader's source", e);
      }
    }
    
    @Override
    public void setMetadata(Metadata meta) throws IOException {
      super.setMetadata(meta);
      
      if (reader != null) reader.close();
      
      try {
        String baseId = ZipUtilities.unzipId(scifio(), meta.getSource(), null);
        
        reader = scifio().initializer().initializeReader(baseId);
        meta.setMetadata(reader.getMetadata());
      } catch (FormatException e) {
        LOGGER.error("Failed to initialize delegate Reader", e);
      }
    }
    
    /** Specifies whether or not to normalize float data. */
    public void setNormalized(boolean normalize) {
      if (reader != null) reader.setNormalized(normalize);
    }
    
    /*
     * @see ome.scifio.TypedReader#openPlane(int, int, ome.scifio.DataPlane, int, int, int, int)
     */
    public ByteArrayPlane openPlane(int imageIndex, int planeIndex,
      ByteArrayPlane plane, int x, int y, int w, int h) throws FormatException,
      IOException
    {
      Plane p = reader.openPlane(imageIndex, planeIndex, plane, x, y, w, h);
      System.arraycopy(p.getBytes(), 0, plane.getData(), 0, plane.getData().length);
      return plane;
    }
    
    @Override
    public void close(boolean fileOnly) throws IOException {
      super.close(fileOnly);
      if (reader != null) reader.close(fileOnly);
      if (!fileOnly) reader = null;
    }
  }
  
  // -- Helper class --
  
  private static class ZipUtilities {
   
    /**
     * Extracts the String id of the provided stream.
     * 
     * @param scifio - A SCIFIO wrapping the current context
     * @param stream - Stream, built around a .zip file, to extract the actual id from
     * @param mappedFiles - Optional param. If provided, all discovered entries in the
     *                      underlying archive will be added to this list.
     * @return An id of the base entry in the .zip
     * @throws IOException
     */
    public static String unzipId(SCIFIO scifio, RandomAccessInputStream stream,
      List<String> mappedFiles) throws IOException
    {
      ZipInputStream zip = new ZipInputStream(stream);
      ZipEntry ze = null;
      
      while (true) {
        ze = zip.getNextEntry();
        if (ze == null) break;
        ZipHandle handle = new ZipHandle(scifio.getContext(), stream.getFileName(), ze);
        scifio.location().mapFile(ze.getName(), handle);
        if (mappedFiles != null) mappedFiles.add(ze.getName());
      }

      ZipHandle base = new ZipHandle(scifio.getContext(), stream.getFileName());
      String id = base.getEntryName();
      base.close();
      
      return id;
    }
    
    /**
     * Returns a new RandomAccessInputStream around the raw handle underlying the
     * provided stream, instead of using a zip handle.
     * <p>
     * NB: closes the provided stream.
     * </p>
     */
    public static RandomAccessInputStream getRawStream(SCIFIO scifio,
      RandomAccessInputStream stream) throws IOException
    {
      // NB: We need a raw handle on the ZIP data itself, not a ZipHandle.
      String id = stream.getFileName();
      IRandomAccess rawHandle = scifio.location().getHandle(id, false, false);
      stream.close();
      return new RandomAccessInputStream(scifio.getContext(), rawHandle, id);
    }
  }
}