package com.gcs.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件工具类
 * 提供文件操作相关的通用方法
 * @author 
 * @date 2026-04-16
 */
@Slf4j
public class FileUtil {
    
    private static final int BUFFER_SIZE = 8192; // 8KB缓冲区
    
    /**
     * 将文件转换为字节数组
     *
     * @param file 文件对象
     * @return 文件字节数组
     * @throws IOException IO异常
     */
    public static byte[] fileToByte(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("文件对象不能为空");
        }
        
        if (!file.exists()) {
            throw new IOException("文件不存在: " + file.getAbsolutePath());
        }
        
        if (!file.isFile()) {
            throw new IOException("指定路径不是文件: " + file.getAbsolutePath());
        }
        
        try (InputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("文件转换为字节数组失败: {}", file.getAbsolutePath(), e);
            throw e;
        }
    }
    
    /**
     * 将字节数组保存为文件
     *
     * @param bytes 字节数组
     * @param filePath 文件路径
     * @throws IOException IO异常
     */
    public static void byteToFile(byte[] bytes, String filePath) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("字节数组不能为空");
        }
        
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        
        Path path = Path.of(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
    }
    
    /**
     * 获取文件大小（人类可读格式）
     *
     * @param fileSize 文件大小（字节）
     * @return 格式化后的文件大小
     */
    public static String getReadableFileSize(long fileSize) {
        if (fileSize <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1024));
        
        return String.format("%.1f %s", fileSize / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    /**
     * 验证文件是否存在且可读
     *
     * @param filePath 文件路径
     * @return 是否可读
     */
    public static boolean isFileReadable(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        
        File file = new File(filePath);
        return file.exists() && file.isFile() && file.canRead();
    }
}
