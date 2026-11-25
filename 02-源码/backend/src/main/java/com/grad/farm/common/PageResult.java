package com.grad.farm.common;

import lombok.Data;
import java.util.List;

/** 服务端分页统一结构，前端只负责渲染页码。 */
@Data
public class PageResult<T> {
    private long total;
    private long page;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(long total, long page, long size, List<T> records) {
        PageResult<T> p = new PageResult<>();
        p.total = total; p.page = page; p.size = size; p.records = records;
        return p;
    }
}
