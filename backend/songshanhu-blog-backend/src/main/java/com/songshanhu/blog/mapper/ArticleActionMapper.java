package com.songshanhu.blog.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.songshanhu.blog.dto.response.ArticleActionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleActionMapper {

    Page<ArticleActionItem> selectUserActions(Page<ArticleActionItem> page,
                                             @Param("userId") Long userId,
                                             @Param("liked") boolean liked,
                                             @Param("collected") boolean collected);
}
