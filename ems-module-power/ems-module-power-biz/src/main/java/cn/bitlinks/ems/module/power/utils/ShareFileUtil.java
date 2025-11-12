package cn.bitlinks.ems.module.power.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/09/29 17:16
 **/

@Slf4j
public class ShareFileUtil {

    public static InputStream getRemoteFile(String uncFilePath) {
        // Windows 共享文件夹中的文件 UNC 路径
        uncFilePath = "\\\\192.168.110.20\\share\\测试导入.xlsx";

        // 或者，如果已映射为网络驱动器，比如 Z:
        // String filePath = "Z:\\data.xlsx";

        File remoteFile = new File(uncFilePath);
        if (!remoteFile.exists()) {
            log.error("文件不存在: " + uncFilePath);
        }

        // 示例：将文件复制到当前服务器的本地目录，比如 D:/uploads/data.xlsx
        Path targetPath = Paths.get("D:/uploads/测试导入.xlsx"); // 你可以改成任何你希望保存的路径

        try {
            // 确保目标目录存在
            Files.createDirectories(targetPath.getParent());
            // 将共享文件拷贝到本地服务器目录
            Files.copy(remoteFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件已成功拷贝到本地服务器: " + targetPath);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("拷贝文件失败: " + e.getMessage());
        }

        return null;

    }

    /**
     * 从内网 Windows 共享文件夹中查找指定的 Excel 文件，并返回其 InputStream
     *
     * @param uncSharePath  共享文件夹的 UNC 路径，如 \\192.168.1.100\share\
     * @param excelFileName 要查找的 Excel 文件名，如 data.xlsx
     * @return Excel 文件的 InputStream，如果找不到则返回 null
     */
    public static InputStream findAndReturnExcelInputStream(String uncSharePath, String excelFileName) {
        // 确保路径以 \ 结尾（可选，但推荐统一格式）
        String normalizedPath = uncSharePath.endsWith("\\") ? uncSharePath : uncSharePath + "\\";

        File sharedFolder = new File(normalizedPath);

        log.info("尝试访问共享文件夹: " + sharedFolder.getAbsolutePath());

        if (!sharedFolder.exists() || !sharedFolder.isDirectory()) {
            log.error("共享文件夹不存在或不是一个目录: " + normalizedPath);
            return null;
        }

        // 获取该目录下所有文件
        File[] files = sharedFolder.listFiles();
        if (files == null) {
            log.error("无法读取共享文件夹内容，可能没有权限。路径: " + normalizedPath);
            return null;
        }

        // 遍历查找目标 Excel 文件
        for (File file : files) {
            if (file.isFile() && file.getName().equalsIgnoreCase(excelFileName)) {
                log.info("找到目标 Excel 文件: " + file.getAbsolutePath());
                try {
                    // 返回该文件的 InputStream
                    return Files.newInputStream(file.toPath());
                } catch (IOException e) {
                    log.error("无法打开文件为输入流: " + file.getAbsolutePath());
                    e.printStackTrace();
                    return null;
                }
            }
        }

        log.info("未找到指定的 Excel 文件: " + excelFileName + " 在目录: " + normalizedPath);
        return null;
    }


    /**
     * 获取指定 UNC 路径下所有 Excel 文件（.xlsx, .xls）的 InputStream 列表
     */
    public static List<InputStream> getExcelFilesAsStreams(String uncFolderPath) {
        List<InputStream> streams = new ArrayList<>();
        File folder = new File(uncFolderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            log.error("目标文件夹不存在或不是一个目录: " + uncFolderPath);
            return streams;
        }

        File[] files = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".xlsx") || lower.endsWith(".xls");
        });

        if (files == null || files.length == 0) {
            log.info("在路径 " + uncFolderPath + " 下未找到 Excel 文件");
            return streams;
        }

        for (File excelFile : files) {
            try {
                InputStream is = Files.newInputStream(excelFile.toPath());
                streams.add(is);
            } catch (Exception e) {
                log.error("无法读取文件 " + excelFile.getName() + ": " + e.getMessage());
            }
        }

        return streams;
    }

    /**
     * 递归扫描指定 UNC 路径及其所有子目录，找出所有 Excel 文件，并返回它们的 InputStream 列表
     */
    public static List<File> scanAndCollectExcelFiles(String rootUncPath) {
        List<File> excelFiles = new ArrayList<>();
        File rootDir = new File(rootUncPath);

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            log.error("目标路径不存在或不是一个目录: " + rootUncPath);
            return excelFiles;
        }

        // 开始递归扫描
        scanDirectoryRecursively(rootDir, excelFiles);

        return excelFiles;
    }

    /**
     * 递归遍历目录，找出所有 .xlsx 和 .xls 文件，并获取其 InputStream
     */
    private static void scanDirectoryRecursively(File dir, List<File> excelFiles) {
        File[] files = dir.listFiles();
        if (files == null) {
            log.error("无法读取目录内容: " + dir.getAbsolutePath());
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 如果是目录，继续递归深入
                scanDirectoryRecursively(file, excelFiles);
            } else {
                // 如果是文件，判断是否为 Excel 文件,忽略带有括号的文件，只读取一天的不然老是重复读取
                String fileName = file.getName().toLowerCase();
                if ((fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) && !fileName.startsWith("~$") ) {

                    // ✅ 新增：检查文件大小是否大于 0
                    if (file.length() > 0) {
                        excelFiles.add(file);
                    } else {
                        log.info("⚠️ 忽略空文件（0字节）: " + file.getName());
                    }
                }
            }
        }
    }
}
