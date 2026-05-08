package com.songshanhu.blog.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.songshanhu.blog.dto.response.ArticleActionItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@Import(com.songshanhu.blog.config.MyBatisPlusConfig.class)
@ActiveProfiles("test")
@Sql(statements = {
        "drop table if exists article_collect",
        "drop table if exists article_like",
        "drop table if exists article",
        "create table article (id bigint primary key, title varchar(255), summary varchar(500), cover_image varchar(255), category varchar(50), author_id bigint, author_name varchar(50), views int, comments int, likes int)",
        "create table article_like (id bigint auto_increment primary key, article_id bigint, user_id bigint, create_time timestamp default current_timestamp)",
        "create table article_collect (id bigint auto_increment primary key, article_id bigint, user_id bigint, create_time timestamp default current_timestamp)",
        "insert into article (id,title,summary,cover_image,category,author_id,author_name,views,comments,likes) values (1,'t1','s1',null,'visual',10,'a1',1,2,3)",
        "insert into article_like (article_id,user_id,create_time) values (1,100, current_timestamp)",
        "insert into article_collect (article_id,user_id,create_time) values (1,100, current_timestamp)"
})
class ArticleActionMapperTest {

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ArticleActionMapper mapper;

    @Test
    void selectUserActions_returnsExpectedFields() {
        Page<ArticleActionItem> page = mapper.selectUserActions(new Page<>(1, 10), 100L, true, true);
        assertThat(page.getRecords()).hasSize(1);
        ArticleActionItem it = page.getRecords().get(0);
        assertThat(it.getId()).isNotNull();
        assertThat(it.getTitle()).isNotBlank();
        assertThat(it.getActionTime()).isNotNull();
        assertThat(it.getLikedAt()).isNotNull();
        assertThat(it.getCollectedAt()).isNotNull();
    }
}
