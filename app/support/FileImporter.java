package support;

import models.Transformer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import play.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public class FileImporter {

    private String EXTENSION = ".csv";
    private String contentTypeFile = "UTF-8";


    private BufferedReader bf = null;
    private DataInputStream in = null;
    private boolean fileIsOpen = false;
    private File currentFile = null;
    private long lineNumber = 0;
    private long numberOfLinesInFile = 0;
    private final String importPath;

    public FileImporter(Transformer transformer) {
        this.importPath = transformer.importPath;
        this.EXTENSION = transformer.importFileExtension;
        this.contentTypeFile = transformer.importFilecontentType;
    }


    public long getNrOfLines() {
        return numberOfLinesInFile;
    }

    public String getCurrentFileName() {
        return currentFile.getAbsolutePath();
    }


    /**
     * Reads lines from all the files in the importPath
     * When finished reading a file, it will be renamed
     *
     * @return the next line of the file, or null in case of an error / EOF
     */
    public synchronized String getNextLine() {

        if(!fileIsOpen) {
            currentFile = getNextFile(this.importPath);
            if (currentFile == null) {
                Logger.debug("No next file");
                return null;
            }
            try {
                openFile(currentFile.getAbsolutePath());
                if (!fileIsOpen) {
                    Logger.debug("Could not open file");
                    return null;
                }
            } catch (Exception e) {
                Logger.debug("Could not open file");
                return null;
            }
        }
        try {
            String line = bf.readLine();
            if (line != null) {
                lineNumber++;
                Logger.trace(lineNumber + "/" + numberOfLinesInFile);
                return line;
            } else {
                closeFile();
                String  date = new DateTime().toString("yyyyMMdd-HHmmss");
                currentFile = changeFileExtension(currentFile, "imported_" + date);
                return getNextLine();
            }
        } catch (Exception e) {
            Logger.error(
                    String.format("%s -> %s",
                            e.getStackTrace()[0].getMethodName(), e.getMessage()));
            return null;
        }
    }




    /**
     * Opens the next file in the directory with the correct file extension
     * The opened file is renamed, to prevent another process from opening it.
     *
     * @param importDirectory   Name of the directory in which to look
     * @return                  File found in the directory
     */
    private File getNextFile(String importDirectory) {
        File folder = new File(importDirectory.replace("\\", "\\\\"));
        File[] files = folder.listFiles();
        if (files == null) {
            Logger.warn(
                    "No files found. Does directory "
                            + folder
                            + " exist and does it contain a file with the extension "
                            + EXTENSION + " ?");
            return null;
        }
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.valueOf(f1.lastModified()).compareTo(
                        f2.lastModified());
            }
        });
        for (File file1 : files) {
            if (file1.isFile()) {
                if (file1.getName().toLowerCase()
                        .endsWith(EXTENSION.toLowerCase())) {
                    File file = file1;
                    try {
                        String date = new DateTime().toString("yyyyMMdd-HHmmss");
                        file = changeFileExtension(file, "busy_" + date);
                        numberOfLinesInFile = countLinesInFile(file.getAbsolutePath());
                        openFile(file.getAbsolutePath());
                        lineNumber = 0;
                        return file;
                    } catch (Exception e) {
                        Logger.error("Failed to rename " + file.getAbsolutePath());
                        return null;
                    }
                }
            }
        }
        return null;
    }



     /**
     * Count the number of lines in the text file
     * @param filename  path of the file
     * @return          number of lines in the file
     */
    private long countLinesInFile(String filename) throws IOException {
        Logger.debug("Counting lines in " + filename);
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];
            long count = 0;
            int readChars;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            Logger.debug("File " + filename + " contains " + count + " lines.");
            return (count == 0 && !empty) ? 1 : count;
        } finally {
            is.close();
        }
    }



    /**
     * Open the file
     *
     * @param filePath      file path of the file to be opened
     *
     * @throws Exception
     */
    private void openFile(String filePath) throws Exception {
        //currentFile = filePath;
        try {
            FileInputStream fr = new FileInputStream(filePath);
            in = new DataInputStream(fr);
            bf = new BufferedReader(new InputStreamReader(in, contentTypeFile));
            fileIsOpen = true;
        } catch (Exception e) {
            try {
                if (in != null)
                    in.close();
                if (bf != null)
                    bf.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            throw new Exception(String.format("%s -> %s",
                    e.getStackTrace()[0].getMethodName(), e.getMessage()));
        }
    }

    /**
     * Close the file
     */
    private void closeFile() {
        fileIsOpen  = false;
        try {
            if (in != null)
                in.close();
            if (bf != null)
                bf.close();
        } catch (IOException e) {
            Logger.error("Failed to closeFile" + e.getMessage());
        }
    }


    /**
     * Change the file extension
     *
     * @param file          Name of the file
     * @param extension     The new file extension
     * @return              File reference of the renamed file
     * @throws Exception
     */
    private static File changeFileExtension(File file, String extension)
            throws Exception {
        try {
            File newfile = new File(
                    String.format(
                            "%s.%s",
                            file.getAbsolutePath().substring(0,
                                    file.getAbsolutePath().lastIndexOf(".")),
                            extension));
            if (!file.renameTo(newfile)) {
                throw new Exception("Rename failed.");
            } else

                return newfile;
        } catch (Exception e) {
            throw new Exception(String.format("%s -> %s",
                    e.getStackTrace()[0].getMethodName(), e.getMessage()));
        }
    }


    /**
     * Converts a timestamp string to a Java Date
     * <p/>
     * format of the string: 2013-03-08-09.39.20.264000
     *
     * @param value   String containing a timestamp
     * @return        Date object with the converted timestamp string
     */
    protected Date convertStringToDate(String value) {

        DateTime dateTime = DateTime.parse(value, DateTimeFormat.forPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS"));
        return dateTime.toDate();

    }


}
