package com.grad.farm.service;

import com.grad.farm.common.BizException;
import com.grad.farm.common.UserContext;
import com.grad.farm.entity.SysFile;
import com.grad.farm.mapper.SysFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传：落盘到项目根 uploads/yyyyMM/，数据库保存相对路径。
 * 只允许图片与 PDF，单文件不超过 5MB，阻止可执行扩展名。
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private static final List<String> ALLOWED = List.of("jpg", "jpeg", "png", "webp", "pdf");

    private final SysFileMapper fileMapper;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public Map<String, Object> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BizException("文件超过 5MB，请压缩后上传");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext)) {
            throw new BizException("仅支持 jpg/png/webp/pdf，当前为：" + (ext.isEmpty() ? "未知" : ext));
        }
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, month).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name).toFile());
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
        String rel = "/uploads/" + month + "/" + name;
        SysFile f = new SysFile();
        f.setFileName(name);
        f.setOriginalName(original);
        f.setFilePath(rel);
        f.setFileSize(file.getSize());
        f.setContentType(file.getContentType());
        f.setUploader(UserContext.realName());
        fileMapper.insert(f);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", f.getId());
        res.put("path", rel);
        res.put("url", rel);
        res.put("originalName", original);
        res.put("size", file.getSize());
        return res;
    }
}
