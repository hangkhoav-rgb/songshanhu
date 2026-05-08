package com.songshanhu.blog.controller;

import com.songshanhu.blog.dto.request.ImageErrorReportRequest;
import com.songshanhu.blog.dto.request.EventReportRequest;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/monitor")
@Tag(name = "监控上报", description = "前端监控与异常上报")
public class MonitorController {

    @PostMapping("/image-error")
    @Operation(summary = "图片加载失败上报")
    public Result<?> reportImageError(@Valid @RequestBody ImageErrorReportRequest req) {
        log.warn("IMAGE_LOAD_FAIL url={} page={} ua={} ref={} msg={}",
                req.getUrl(), req.getPage(), req.getUserAgent(), req.getReferrer(), req.getMessage());
        return Result.success(null);
    }

    @PostMapping("/event")
    @Operation(summary = "埋点事件上报")
    public Result<?> reportEvent(@Valid @RequestBody EventReportRequest req) {
        log.info("FE_EVENT event={} page={} props={}", req.getEvent(), req.getPage(), req.getProps());
        return Result.success(null);
    }
}
