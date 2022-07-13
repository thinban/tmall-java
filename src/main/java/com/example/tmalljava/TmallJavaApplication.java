package com.example.tmalljava;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@SpringBootApplication
@EnableScheduling
@RestController
@Component
@Slf4j
@Api
public class TmallJavaApplication {
    @Resource
    TmallService tmallService;

    public static void main(String[] args) {
        SpringApplication.run(TmallJavaApplication.class, args);
    }

    //工作日18点提醒打卡
    @Scheduled(cron = "0 0 18 * * MON-FRI")
    private void signNotify() {
        tmallService.sendMsg("傻瓜，记得打卡!");
    }

    @GetMapping("/")
    public void index(HttpServletResponse response) throws Exception {
        response.sendRedirect("/swagger-ui/");
    }

    @ApiOperation("发送通知:server酱")
    @GetMapping("send")
    public Object send(@RequestParam @ApiParam("内容") String s) {
        return tmallService.sendMsg(s);
    }

    @ApiOperation("天猫网页截图")
    @GetMapping("tmallShot")
    public Object shot(@RequestParam @ApiParam("天猫网址") String s, HttpServletResponse response) {
        tmallService.tmallShot(s, response);
        return "ok";
    }

    @ApiOperation("网页全屏截图")
    @GetMapping("fullShot")
    public Object fullShot(@RequestParam @ApiParam("网址") String s, HttpServletResponse response) {
        tmallService.fullScreenShot(s, response);
        return "ok";
    }

    @ApiOperation("网页全屏截图（多条）")
    @GetMapping("fullShotMulty")
    public Object fullShotMulty(@RequestParam @ApiParam("网址") List<String> s, boolean fullScreen, HttpServletResponse response) throws Exception {
        tmallService.fullShotMulty(s, response);
        return "ok";
    }

}
