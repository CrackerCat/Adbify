package com.adbify.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class FileUtils {
    public static String copyUriToPath(Context context, Uri uri, String dest) {
        String filename =
                String.format(
                        "%s.%s", getFileBaseName(uri.getPath()), getFileExtension(uri.getPath()));
        try {
            DocumentFile docFile = DocumentFile.fromSingleUri(context, uri);
            String name = null;
            if (docFile != null) name = docFile.getName();
            if (name != null) filename = name;
        } catch (Exception e) {
            // ignore
        }

        FileUtils.makeDir(dest);
        File destFile = new File(dest, filename);
        FileUtils.createNewFile(destFile.getAbsolutePath());

        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(destFile));
            bis = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
            byte[] buffer = new byte[65536];
            int numBytes;
            while ((numBytes = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, numBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            AndroidUtilities.closeQuietly(bos);
            AndroidUtilities.closeQuietly(bis);
        }
        return destFile.getAbsolutePath();
    }

    public static String relativePath(String _file, String _dir) {
        File dir = new File(_dir);
        File file = new File(_file);
        return dir.toURI().relativize(file.toURI()).getPath();
    }

    public static String relativePath(File file, File dir) {
        return dir.toURI().relativize(file.toURI()).getPath();
    }

    public static String readFile(String path) {
        if (isExistFile(path) && isFile(path)) {
            File file = new File(path);
            String output = "";
            try {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (sb.length() == 0) {
                        sb.append(line);
                    } else {
                        sb.append('\n');
                        sb.append(line);
                    }
                }
                reader.close();
                fis.close();
                output = sb.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return output;
        } else {
            return "";
        }
    }

    public static void createNewFile(String path) {
        int lastSep = path.lastIndexOf(File.separator);
        if (lastSep > 0) {
            String dirPath = path.substring(0, lastSep);
            makeDir(dirPath);
        }
        File file = new File(path);
        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(InputStream source, String destPath) {
        createNewFile(destPath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(destPath, false);
            byte[] buff = new byte[1024];
            int length;
            while ((length = source.read(buff)) > 0) {
                fos.write(buff, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void copyFile(String sourcePath, String destPath) {
        if (!isExistFile(sourcePath)) return;
        createNewFile(destPath);
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(sourcePath);
            fos = new FileOutputStream(destPath, false);
            byte[] buff = new byte[1024];
            int length;
            while ((length = fis.read(buff)) > 0) {
                fos.write(buff, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        if (!file.exists()) return;
        if (file.isFile()) {
            file.delete();
            return;
        }
        File[] fileArr = file.listFiles();
        if (fileArr != null) {
            for (File subFile : fileArr) {
                if (subFile.isDirectory()) {
                    deleteFile(subFile.getAbsolutePath());
                }
                if (subFile.isFile()) {
                    subFile.delete();
                }
            }
        }
        file.delete();
    }

    public static String getFileExtension(final String filename) {
        if (filename == null) {
            return null;
        }
        final String name = new File(filename).getName();
        final int extensionPosition = name.lastIndexOf('.');
        if (extensionPosition < 0) {
            return "";
        }
        return name.substring(extensionPosition + 1);
    }

    public static String getFileBaseName(final String filename) {
        if (filename == null) {
            return null;
        }
        final String name = new File(filename).getName();
        final int extensionPosition = name.lastIndexOf('.');
        if (extensionPosition < 0) {
            return name;
        }
        return name.substring(0, extensionPosition);
    }

    public static boolean canReadFile(String path) {
        File file = new File(path);
        return file.canRead();
    }

    public static boolean canWriteFile(String path) {
        File file = new File(path);
        return file.canWrite();
    }

    public static boolean canExecuteFile(String path) {
        File file = new File(path);
        return file.canWrite();
    }

    public static boolean isExistFile(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void makeDir(String path) {
        if (!isExistFile(path)) {
            File file = new File(path);
            file.mkdirs();
        }
    }

    public static void listDir(String path, ArrayList<String> list) {
        File dir = new File(path);
        if (!dir.exists() || dir.isFile()) return;
        File[] listFiles = dir.listFiles();
        if (listFiles == null || listFiles.length <= 0) return;
        if (list == null) return;
        list.clear();
        for (File file : listFiles) {
            list.add(file.getAbsolutePath());
        }
    }

    public static boolean isDirectory(String path) {
        if (!isExistFile(path)) return false;
        return new File(path).isDirectory();
    }

    public static boolean isFile(String path) {
        if (!isExistFile(path)) return false;
        return new File(path).isFile();
    }

    public static long getFileLength(String path) {
        if (!isExistFile(path)) return 0;
        return new File(path).length();
    }

    public static String getPublicDir(String type) {
        return Environment.getExternalStoragePublicDirectory(type).getAbsolutePath();
    }

    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri =
                            ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"),
                                    Long.parseLong(id));
                    return getDataColumn(context, contentUri, null, null);
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "audio":
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            break;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    public static String getDataColumn(
            Context context, Uri uri, String selection, String[] selectionArgs) {
        final String column = "_data";
        final String[] projection = {column};
        try {
            @SuppressLint("Recycle") Cursor cursor =
                    context.getContentResolver()
                            .query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                String value = cursor.getString(column_index);
                if (value.startsWith("content://")
                        || !value.startsWith("/") && !value.startsWith("file://")) {
                    return null;
                }
                return value;
            }
        } catch (Exception ignore) {

        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
