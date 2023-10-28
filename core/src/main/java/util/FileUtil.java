package util;

import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFileChooser;

public class FileUtil
{
	public static File createNewFile(String filePath, String contents)
	{
		File file = new File(filePath);
		boolean success = false;
		
		try
		{
			success = file.createNewFile();
		}
		catch (IOException ioe)
		{
			Debug.append("Caught " + ioe + " creating file " + filePath);
		}
		
		if (!success)
		{
			return null;
		}
		
		//We have created the empty file, now fill it
		try (FileOutputStream fos = new FileOutputStream(filePath))
		{
			byte[] bytes = contents.getBytes("UTF-8");
			fos.write(bytes);
		}
		catch (IOException ioe)
		{
			Debug.append("Caught " + ioe + " trying to insert bytes into file " + filePath);
			deleteFileIfExists(filePath);
			return null;
		}
		
		Debug.append("Successfully created file " + file);
		return file;
	}
	
	public static String getMd5Crc(String filePath)
	{
		String crc = null;
		
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(Files.readAllBytes(Paths.get(filePath)));
			byte[] digest = md.digest();
			
			crc = EncryptionUtil.base64Interface.encode(digest);
		}
		catch (Throwable t)
		{
			Debug.append("Caught " + t + " trying to get CRC of file");
		}
		
		return crc;
	}
	
	public static long getFileSize(String filePath)
	{
		long fileSize = -1;
		File f = new File(filePath);
		
		try (FileInputStream fis = new FileInputStream(f))
		{
			fileSize = fis.getChannel().size();
		}
		catch (Throwable t)
		{
			Debug.stackTrace(t, "Couldn't obtain file size for path " + filePath);
		}
		
		return fileSize;
	}
	
	public static boolean deleteFileIfExists(String filePath)
	{
		boolean success = false;
		
		try
		{
			Path path = Paths.get(filePath);
			success = Files.deleteIfExists(path);
		}
		catch (Throwable t)
		{
			Debug.stackTrace(t, "Failed to delete file");
		}
		
		return success;
	}
	
	public static String swapInFile(String oldFilePath, String newFilePath)
	{
		boolean success = true;
		
		File oldFile = new File(oldFilePath);
		String oldFileName = oldFile.getName();
		File newFile = new File(newFilePath);
		
		File zzOldFile = new File(oldFile.getParent(), "zz" + oldFileName);
		if (oldFile.exists()
		  && !oldFile.renameTo(zzOldFile))
		{
			return "Failed to rename old out of the way.";
		}
		
		success &= newFile.renameTo(new File(oldFile.getParent(), oldFileName));
		if (!success)
		{
			return "Failed to rename new file to " + oldFileName;
		}
		
		if (zzOldFile.isFile())
		{
			success &= deleteFileIfExists(zzOldFile.getPath());
		}
		else
		{
			success &= deleteDirectoryIfExists(zzOldFile);
		}
		
		if (!success)
		{
			return "Failed to delete zz'd old file: " + oldFile.getPath();
		}
		
		return null;
	}
	
	public static void saveTextToFile(String text, Path destinationPath)
	{
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(destinationPath, charset)) 
		{
			String[] values = text.split("\n");
		    for (String word : values) 
		    {
		        writer.write(word);
		        writer.newLine();
		    }
		}
		catch (IOException x) 
		{
			Debug.stackTrace(x);
		}
	}
	
	public static String getFileContentsAsString(File file)
	{
		String ret = null;
		
		try
		{
			Path path = file.toPath();
			byte[] bytes = Files.readAllBytes(path);
			ret = new String(bytes, "UTF-8");
		}
		catch (Throwable t)
		{
			Debug.stackTrace(t);
		}
		
		return ret;
	}
	
	public static String getBase64DecodedFileContentsAsString(File file)
	{
		try
		{
			Path filePath = file.toPath();
			byte[] bytes = Files.readAllBytes(filePath);
			byte[] decodedBytes = EncryptionUtil.base64Interface.decode(bytes);
			
			return new String(decodedBytes, "UTF-8");
		}
		catch (Throwable t)
		{
			Debug.stackTrace(t, "Failed to decode contents of file " + file);
			return null;
		}
	}

	public static void encodeAndSaveToFile(Path destinationPath, String stringToWrite)
	{
		String encodedStringToWrite = EncryptionUtil.base64Interface.encode(stringToWrite.getBytes());
		saveTextToFile(encodedStringToWrite, destinationPath);
	}
	
	public static Dimension getImageDim(String path) 
	{
	    Dimension result = null;
	    String suffix = getFileSuffix(path);
	    Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
	    if (iter.hasNext()) 
	    {
	        ImageReader reader = iter.next();
	        try (ImageInputStream stream = new FileImageInputStream(new File(path));)
	        {
	            reader.setInput(stream);
	            int width = reader.getWidth(reader.getMinIndex());
	            int height = reader.getHeight(reader.getMinIndex());
	            result = new Dimension(width, height);
	        } 
	        catch (IOException e) 
	        {
	            Debug.stackTrace(e);
	        } 
	        finally 
	        {
	            reader.dispose();
	        }
	    } 
	    else 
	    {
	        Debug.stackTrace("No reader found for file extension: " + suffix + " (full path: " + path + ")");
	    }
	    
	    return result;
	}
	
	private static String getFileSuffix(String path)
	{
		if (path == null
		  || path.lastIndexOf('.') == -1)
		{
			return "";
		}
		
		int dotIndex = path.lastIndexOf('.');
		return path.substring(dotIndex + 1);
	}
	
	/**
	 * Helper to create a file object for a URL, e.g. from a classpath resource.
	 * No longer used
	 */
	/*public static File getForURL(URL url)
	{
		try
		{
			URI uri = url.toURI();
			return new File(uri);
		}
		catch (Throwable t)
		{
			Debug.append("Failed to construct file for URL: " + url);
			Debug.stackTrace(t);
			return null;
		}
	}*/
	
	public static String stripFileExtension(String filename)
	{
		int ix = filename.indexOf('.');
		return filename.substring(0, ix);
	}
	
	public static byte[] getByteArrayForResource(String resourcePath)
	{
		try (InputStream is = FileUtil.class.getResourceAsStream(resourcePath);
		  ByteArrayOutputStream baos = new ByteArrayOutputStream())
	    {
			byte[] b = new byte[4096];
			int n = 0;
			while ((n = is.read(b)) != -1) 
			{
                baos.write(b, 0, n);
            }
			
			return baos.toByteArray();
	    }
		catch (IOException ioe)
		{
			Debug.stackTrace(ioe, "Failed to read classpath resource: " + resourcePath);
			return null;
		}
	}
	
	/**
	 * Delete a whole directory, recursively clearing out the files/subfolders too.
	 */
	public static boolean deleteDirectoryIfExists(File dir)
	{
		if (!dir.exists()
		  || !dir.isDirectory())
		{
			//Just don't do anything
			return true;
		}
		
		boolean success = true;
		File[] files = dir.listFiles();
		for (int i=0; i<files.length; i++)
		{
			File file = files[i];
			if (file.isDirectory())
			{
				deleteDirectoryIfExists(file);
			}
			else
			{
				success = file.delete();
			}
			
			if (!success)
			{
				return false;
			}
		}
		
		return dir.delete();
	}
	
	/**
	 * Copy directory A to a new directory, B. Copies all subfolders and files. 
	 */
	public static boolean copyDirectoryRecursively(File dirFrom, String dirToCreate)
	{
		boolean success = copyFile(dirFrom, dirToCreate);
		if (!success)
		{
			return false;
		}
		
		File[] files = dirFrom.listFiles();
		for (int i=0; i<files.length; i++)
		{
			File file = files[i];
			String dirTo = dirToCreate + "\\" + file.getName();
			if (file.isDirectory())
			{
				success = copyDirectoryRecursively(file, dirTo);
			}
			else
			{
				success = copyFile(file, dirTo);
			}
			
			if (!success)
			{
				return false;
			}
		}
		
		return success;
	}
	
	private static boolean copyFile(File fileFrom, String destinationFile)
	{
		try
		{
			Files.copy(fileFrom.toPath(), Paths.get(destinationFile));
		}
		catch (IOException ioe)
		{
			Debug.append("Caught " + ioe + " copying " + fileFrom + " to " + destinationFile);
			Debug.stackTraceSilently(ioe);
			return false;
		}
		
		return true;
	}
	
	/**
	 * FileChooser
	 */
	public static File chooseDirectory(Component comp)
	{
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		int option = fc.showDialog(comp, "Select");
		if (option != JFileChooser.APPROVE_OPTION)
		{
			Debug.append("Cancelled directory selection");
			return null;
		}
		
		return fc.getSelectedFile();
	}
}
