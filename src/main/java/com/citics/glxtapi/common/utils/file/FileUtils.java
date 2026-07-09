package com.citics.glxtapi.common.utils.file;

import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件处理工具类
 *
 * @author ruoyi
 */
public class FileUtils {

    public static String FILENAME_PATTERN = "[a-zA-Z0-9_\\-\\|\\.\\u4e00-\\u9fa5]+";

    /**
     * 输出指定文件的byte数组
     *
     * @param filePath 文件路径
     * @param os       输出流
     * @throws IOException
     */
    public static void writeBytes(String filePath, OutputStream os) throws IOException {
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException(filePath);
            }
            fis = new FileInputStream(file);
            byte[] b = new byte[1024];
            int length;
            while ((length = fis.read(b)) > 0) {
                os.write(b, 0, length);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 输入流写入文件
     */
    public static void writeFile(InputStream is, String filepath) throws IOException {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            File file = new File(filepath);
            File fileParent = file.getParentFile();
            if (!fileParent.exists()) {
                fileParent.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            in = new BufferedInputStream(is);
            out = new BufferedOutputStream(new FileOutputStream(filepath));
            int len = -1;
            byte[] b = new byte[1024];
            while ((len = in.read(b)) > 0) {
                out.write(b, 0, len);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除文件夹
     *
     * @param folderPath 文件夹
     */
    public static boolean deleteFolder(String folderPath) {
        boolean flag = false;
        File folder = new File(folderPath);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file.getPath()); // 递归删除子文件夹
                    } else {
                        file.delete(); // 删除子文件
                    }
                }
            }
            folder.delete(); // 删除当前文件夹
            flag = true;
        }
        return flag;
    }

    /**
     * 删除文件
     *
     * @param filePath 文件
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * 文件名称验证
     *
     * @param filename 文件名称
     * @return true 正常 false 非法
     */
    public static boolean isValidFilename(String filename) {
        return filename.matches(FILENAME_PATTERN);
    }

    /**
     * window操作系统文件名不能含有：/"\<>*:
     * mac操作系统文件名不能以.开头
     * linux和Mac基本一致；
     */
    public static String fixValidFilename(String fileName, String replacement) {
        Pattern pattern = Pattern.compile("[\\\\/:*?\"<>|]");
        Matcher matcher = pattern.matcher(fileName);
        fileName = matcher.replaceAll(replacement);
        return fileName;
    }

    /**
     * 下载文件名重新编码
     *
     * @param request 请求对象
     * @param fileName 文件名
     * @return 编码后的文件名
     */
    public static String setFileDownloadHeader(HttpServletRequest request, String fileName) throws UnsupportedEncodingException {
        final String agent = request.getHeader("USER-AGENT");
        String filename = fileName;
        if (agent.contains("MSIE")) {
            // IE浏览器
            filename = UriUtils.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        } else if (agent.contains("Firefox")) {
            // 火狐浏览器
            filename = new String(fileName.getBytes(), "ISO8859-1");
        } else if (agent.contains("Chrome")) {
            // google浏览器
            filename = UriUtils.encode(filename, "utf-8");
        } else {
            // 其它浏览器
            filename = UriUtils.encode(filename, "utf-8");
        }
        return filename;
    }

    /**
     * 获取系统临时目录
     */
    public static String getTemp() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * 创建临时文件
     */
    public static File createTempFile(String filePath, byte[] data) throws IOException {
        String temp = getTemp() + filePath;
        File file = new File(temp);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data, 0, data.length);
        fos.flush();
        fos.close();
        return file;
    }

    /**
     * 复制文件
     */
    public static void copyFileUsingFileChannels(File source, File dest) throws IOException {
        try (FileChannel inputChannel = new FileInputStream(source).getChannel();
             FileChannel outputChannel = new FileOutputStream(dest).getChannel()) {
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        }
    }

}
