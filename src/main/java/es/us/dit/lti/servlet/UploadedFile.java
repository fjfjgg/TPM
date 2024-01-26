/*
    This file is part of Tool Provider Manager - Manager of LTI Tool Providers
    for learning platforms.
    Copyright (C) 2022  Francisco José Fernández Jiménez.

    Tool Provider Manager is free software: you can redistribute it and/or
    modify it under the terms of the GNU General Public License as published
    by the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Tool Provider Manager is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
    Public License for more details.

    You should have received a copy of the GNU General Public License along
    with Tool Provider Manager. If not, see <https://www.gnu.org/licenses/>.
*/

package es.us.dit.lti.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.http.Part;

/**
 * Data of uploaded file to be written.
 *
 * @author Francisco José Fernández Jiménez
 */
public class UploadedFile {
	
	/**
	 * Servlet Part.
	 */
	private Part part;
	
	/**
	 * Generic InputStream.
	 */
	private InputStream is;
	
	/**
	 * Constructor for servlet Part.
	 * 
	 * @param uf Part with data
	 */
	public UploadedFile(Part uf) {
		part = uf;
	}
	
	/**
	 * Constructor for input stream.
	 * 
	 * @param uf the input stream
	 */
	public UploadedFile(InputStream uf) {
		is = uf;
	}
	
	/**
	 * A convenience method to write this uploaded item to disk.
	 * 
	 * <p>This method is not guaranteed to succeed if called more than once for
	 * the same part/output stream.
	 * 
	 * @param fileName The location into which the uploaded part should be stored
	 * @throws IOException if an error occurs
	 */
	public void write(String fileName) throws IOException {
		if (part != null) {
			part.write(fileName);
		} else {
			File f = new File(fileName);
			try (FileOutputStream fos = new FileOutputStream(f)) {
				is.transferTo(fos);
				is.close();
			}
		}
	}
	
}
