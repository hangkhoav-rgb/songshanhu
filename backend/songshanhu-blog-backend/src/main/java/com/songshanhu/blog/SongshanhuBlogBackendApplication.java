package com.songshanhu.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.songshanhu.blog.mapper")
public class SongshanhuBlogBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SongshanhuBlogBackendApplication.class, args);
	}

}
