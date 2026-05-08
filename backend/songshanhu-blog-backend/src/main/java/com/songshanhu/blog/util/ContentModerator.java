package com.songshanhu.blog.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.songshanhu.blog.entity.BizSensitiveWord;
import com.songshanhu.blog.mapper.BizSensitiveWordMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 内容审核工具类
 * 简单实现关键词过滤
 */
@Component
public class ContentModerator {

    private static final List<String> FORBIDDEN_WORDS = Arrays.asList(
            "暴力", "赌博", "毒品", "广告推广", "虚假信息"
    );

    private final BizSensitiveWordMapper bizSensitiveWordMapper;

    public ContentModerator(BizSensitiveWordMapper bizSensitiveWordMapper) {
        this.bizSensitiveWordMapper = bizSensitiveWordMapper;
    }

    /**
     * 检查内容是否符合标准
     * @param content 待检查内容
     * @return 是否合规
     */
    public boolean isCompliant(String content) {
        return moderate(content).getHitLevel() < 3;
    }

    public ModerationResult moderate(String content) {
        if (!StringUtils.hasText(content)) {
            return new ModerationResult(0, List.of());
        }
        try {
            List<BizSensitiveWord> words = bizSensitiveWordMapper.selectList(new LambdaQueryWrapper<BizSensitiveWord>()
                    .eq(BizSensitiveWord::getStatus, 1));
            int hitLevel = 0;
            List<String> hitWords = new ArrayList<>();
            for (BizSensitiveWord w : words) {
                if (w == null || !StringUtils.hasText(w.getWord()) || w.getLevel() == null) {
                    continue;
                }
                boolean hit = false;
                String mt = w.getMatchType();
                if ("regex".equalsIgnoreCase(mt)) {
                    try {
                        hit = Pattern.compile(w.getWord()).matcher(content).find();
                    } catch (Exception ignored) {
                    }
                } else {
                    hit = content.contains(w.getWord());
                }
                if (hit) {
                    hitWords.add(w.getWord());
                    hitLevel = Math.max(hitLevel, w.getLevel());
                }
            }
            return new ModerationResult(hitLevel, hitWords);
        } catch (Exception e) {
            for (String w : FORBIDDEN_WORDS) {
                if (content.contains(w)) {
                    return new ModerationResult(3, List.of(w));
                }
            }
            return new ModerationResult(0, List.of());
        }
    }

    public static class ModerationResult {
        private final int hitLevel;
        private final List<String> hitWords;

        public ModerationResult(int hitLevel, List<String> hitWords) {
            this.hitLevel = hitLevel;
            this.hitWords = hitWords;
        }

        public int getHitLevel() {
            return hitLevel;
        }

        public List<String> getHitWords() {
            return hitWords;
        }
    }
}
