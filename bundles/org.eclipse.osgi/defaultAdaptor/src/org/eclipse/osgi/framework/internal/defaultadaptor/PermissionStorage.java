/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.defaultadaptor;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.osgi.framework.adaptor.core.AdaptorMsg;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.reliablefile.*;

/**
 * Class to model permission data storage.
 */

//TODO switch this class over to use MetaData instead of ReliableFile
class PermissionStorage implements org.eclipse.osgi.framework.adaptor.PermissionStorage {
	/** Directory into which permission data files are stored. */
	protected File permissionDir;

	/** List of permission files: String location => File permission file */
	protected Hashtable permissionFiles;

	/** Default permission data. */
	protected File defaultData;

	/** First permission data format version */
	protected static final int PERMISSIONDATA_VERSION_1 = 1;

	/** Current permission data format version */
	protected static final int PERMISSIONDATA_VERSION = PERMISSIONDATA_VERSION_1;

	/**
	 * Constructor.
	 *
	 * @throws IOException If an error occurs initializing the object.
	 */
	protected PermissionStorage(DefaultAdaptor adaptor) throws IOException {
		permissionDir = new File(adaptor.getBundleStoreRootDir(), "permdata");
		permissionFiles = new Hashtable();

		if (!permissionDir.exists() && !permissionDir.mkdirs()) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Unable to create directory: " + permissionDir.getPath());
			}

			throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
		}

		defaultData = new File(permissionDir, ".default");

		loadLocations();
	}

	/**
	 * Returns the locations that have permission data assigned to them,
	 * that is, locations for which permission data
	 * exists in persistent storage.
	 *
	 * @return The locations that have permission data in
	 * persistent storage, or <tt>null</tt> if there is no permission data
	 * in persistent storage.
	 * @throws IOException If a failure occurs accessing peristent storage.
	 */
	public synchronized String[] getLocations() throws IOException {
		int size = permissionFiles.size();

		if (size == 0) {
			return null;
		}

		String[] locations = new String[size];

		Enumeration enum = permissionFiles.keys();

		for (int i = 0; i < size; i++) {
			locations[i] = (String) enum.nextElement();
		}

		return locations;
	}

	/**
	 * Gets the permission data assigned to the specified
	 * location.
	 *
	 * @param location The location whose permission data is to
	 * be returned.
	 *
	 * @return The permission data assigned to the specified
	 * location, or <tt>null</tt> if that location has not been assigned any
	 * permission data.
	 * @throws IOException If a failure occurs accessing peristent storage.
	 */
	public synchronized String[] getPermissionData(String location) throws IOException {
		File file;

		if (location == null) {
			file = defaultData;
		} else {
			file = (File) permissionFiles.get(location);

			if (file == null) {
				return null;
			}
		}

		try {
			return readData(file);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	/**
	 * Assigns the specified permission data to the specified
	 * location.
	 *
	 * @param location The location that will be assigned the
	 *                 permissions.
	 * @param data The permission data to be assigned, or <tt>null</tt>
	 * if the specified location is to be removed from persistent storaqe.
	 * @throws IOException If a failure occurs modifying peristent storage.
	 */
	public synchronized void setPermissionData(String location, String[] data) throws IOException {
		File file;

		if (location == null) {
			file = defaultData;

			if (data == null) {
				ReliableFile.delete(defaultData);
			} else {
				save(defaultData, null, data); /* Save the value in persistent storage */
			}
		} else {
			file = (File) permissionFiles.get(location);

			if (data == null) {
				if (file == null) {
					return;
				}

				permissionFiles.remove(location);

				ReliableFile.delete(file);
			} else {
				file = save(file, location, data); /* Save the value in persistent storage */

				permissionFiles.put(location, file);
			}
		}
	}

	/**
	 * Load the locations for which permission data exists.
	 *
	 * @throws IOException If an error occurs reading the files.
	 */
	protected void loadLocations() throws IOException {
		String list[] = permissionDir.list();

		int len = list.length;

		for (int i = 0; i < len; i++) {
			String name = list[i];

			if (name.endsWith(ReliableFile.newExt)) {
				continue;
			}

			if (name.endsWith(ReliableFile.oldExt)) {
				continue;
			}

			if (name.endsWith(ReliableFile.tmpExt)) {
				continue;
			}

			File file = new File(permissionDir, name);

			try {
				String location = readLocation(file);

				if (location != null) {
					permissionFiles.put(location, file);
				}
			} catch (FileNotFoundException e) {
				/* the file should have been there */
			}
		}
	}

	/**
	 * Read the location from the specified file.
	 *
	 * @param file File to read the location from.
	 * @return Location from the file or null if the file is unknown.
	 * @throws IOException If an error occurs reading the file.
	 * @throws FileNotFoundException if the data file does not exist.
	 */
	private String readLocation(File file) throws IOException {
		DataInputStream in = new DataInputStream(new ReliableFileInputStream(file));
		try {
			int version = in.readInt();

			switch (version) {
				case PERMISSIONDATA_VERSION_1 :
					{
						boolean locationPresent = in.readBoolean();

						if (locationPresent) {
							String location = in.readUTF();

							return location;
						}
						break;
					}
				default :
					{
						throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
					}
			}
		} finally {
			in.close();
		}

		return null;
	}

	/**
	 * Read the permission data from the specified file.
	 *
	 * @param file File to read the permission data from.
	 * @throws IOException If an error occurs reading the file.
	 * @throws FileNotFoundException if the data file does not exist.
	 */
	private String[] readData(File file) throws IOException {
		DataInputStream in = new DataInputStream(new ReliableFileInputStream(file));
		try {
			int version = in.readInt();

			switch (version) {
				case PERMISSIONDATA_VERSION_1 :
					{
						boolean locationPresent = in.readBoolean();

						if (locationPresent) {
							String location = in.readUTF();
						}

						int size = in.readInt();
						String[] data = new String[size];

						for (int i = 0; i < size; i++) {
							data[i] = in.readUTF();
						}

						return data;
					}
				default :
					{
						throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
					}
			}
		} finally {
			in.close();
		}
	}

	/**
	 * Save the permission data for the specified key.
	 * This assumes an attempt has been made to load
	 * the specified key just prior to calling save.
	 *
	 * @param key Key to save the permission data for.
	 */
	protected File save(File file, String location, String[] data) throws IOException {
		if (file == null) /* we need to create a filename */ {
			file = File.createTempFile("perm", "", permissionDir);
			file.delete(); /* delete the empty file */
		}

		int size = data.length;

		DataOutputStream out = new DataOutputStream(new ReliableFileOutputStream(file));

		try {
			out.writeInt(PERMISSIONDATA_VERSION);
			if (location == null) {
				out.writeBoolean(false);
			} else {
				out.writeBoolean(true);
				out.writeUTF(location);
			}
			out.writeInt(size);

			for (int i = 0; i < size; i++) {
				out.writeUTF(data[i]);
			}

		} finally {
			out.close();
		}

		return file;
	}
}