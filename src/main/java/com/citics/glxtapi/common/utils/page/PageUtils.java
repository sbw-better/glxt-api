package com.citics.glxtapi.common.utils.page;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.PageUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citics.glxtapi.common.page.PageResult;
import com.github.pagehelper.PageInfo;
import java.util.List;

public class PageUtils {

    /**
     * 将分页信息封装到统一的接口
     */
    public static PageResult getPageResult(PageInfo<?> pageInfo, List<?> list) {
        PageResult pageResult = new PageResult();
        pageResult.setPageNum(pageInfo.getPageNum());
        pageResult.setPageSize(pageInfo.getPageSize());
        pageResult.setTotalSize(pageInfo.getTotal());
        pageResult.setTotalPages(pageInfo.getPages());
        pageResult.setContent(list);
        return pageResult;
    }

    public static PageResult getPageResult(PageInfo<?> pageInfo) {
        PageResult pageResult = new PageResult();
        pageResult.setPageNum(pageInfo.getPageNum());
        if (pageInfo.getPageSize() == 0) {
            pageResult.setPageSize(new Long(pageInfo.getTotal()).intValue());
        } else {
            pageResult.setPageSize(pageInfo.getPageSize());
        }
        pageResult.setTotalSize(pageInfo.getTotal());
        pageResult.setTotalPages(pageInfo.getPages());
        pageResult.setContent(pageInfo.getList());
        return pageResult;
    }

    public static PageResult getPageResult(Page page) {
        PageResult pageResult = new PageResult();
        pageResult.setPageNum(Convert.toInt(page.getCurrent()));
        pageResult.setPageSize(Convert.toInt(page.getSize()));
        pageResult.setTotalSize(Convert.toInt(page.getTotal()));
        pageResult.setTotalPages(PageUtil.totalPage(Convert.toInt(page.getTotal()), Convert.toInt(page.getSize())));
        pageResult.setContent(page.getRecords());
        return pageResult;
    }

    public static PageResult getPageResult(long pageNum, long pageSize, long total, List<?> list) {
        PageResult pageResult = new PageResult();
        pageResult.setPageNum(Convert.toInt(pageNum));
        pageResult.setPageSize(Convert.toInt(pageSize));
        pageResult.setTotalSize(Convert.toInt(total));
        pageResult.setTotalPages(PageUtil.totalPage(Convert.toInt(total), Convert.toInt(pageSize)));
        pageResult.setContent(list);
        return pageResult;
    }
}