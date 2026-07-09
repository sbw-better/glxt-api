package com.citics.glxtapi.common.utils.file;

import java.io.File;
import java.util.UUID;

/**
 * <p>Title: </p>
 * <p>Description: 高频方法合集</p>
 * <p>Copyright: Copyright (c) 2019-08-19 14:51</p>
 * <p>Company: </p>
 * @version 1.0
 * @author zmr
 */
public class ToolUtil {

    /**
     * 获取临时目录
     * @author zmr
     */
    public static String getTempPath() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * 获取当前项目工作目录
     * @return
     * @author zmr
     */
    public static String getUserDir() {
        return System.getProperty("user.dir");
    }

    /**
     * 获取临时下载目录
     * @return
     * @author zmr
     */
    public static String getDownloadPath() {
        String tempPath = getTempPath();
        if (!tempPath.endsWith(File.separator)) {
            tempPath += File.separator;
        }
        String result = tempPath + "download" + File.separator;
        File file = new File(result);
        if (!file.exists()) {
            file.mkdirs();
        }
        return result;
    }

    /**
     * 编码文件名
     */
    public static String encodingFilename(String filename) {
        filename = UUID.randomUUID().toString() + "_" + filename;
        return filename;
    }

}
