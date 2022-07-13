package com.example.tmalljava;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.extra.compress.CompressUtil;
import cn.hutool.extra.compress.archiver.Archiver;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.eclipse.jetty.util.UrlEncoded;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;
import ru.yandex.qatools.ashot.shooting.cutter.CutStrategy;
import ru.yandex.qatools.ashot.shooting.cutter.FixedCutStrategy;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class TmallService {
    public static final int screenX = 1980;
    public static final int screenY = 1080;

    public static WebDriver getDriver() {
        //参数配置
//        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        ChromeOptions option = new ChromeOptions();
        option.addArguments("no-sandbox");
        option.addArguments(String.format("--window-size=%s,%s", screenX, screenY));
        option.addArguments("--headless");
        WebDriver driver = new ChromeDriver(option);
        return driver;
    }

    public static BufferedImage doShot(WebDriver driver, CutStrategy cutStrategy) {
        //隐藏滚动条
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.body.style.overflow='hidden';");

        ShootingStrategy shootingStrategy = null;
        if (cutStrategy != null) {
            shootingStrategy = ShootingStrategies.cutting(ShootingStrategies.viewportPasting(100), new FixedCutStrategy(cutStrategy.getHeaderHeight(driver), cutStrategy.getFooterHeight(driver)));
        } else {
            shootingStrategy = ShootingStrategies.viewportPasting(100);
        }
        Screenshot screenshot = new AShot().shootingStrategy(shootingStrategy).takeScreenshot(driver);
        return screenshot.getImage();
    }

    public void render(BufferedImage bufferedImage, String fileName, HttpServletResponse response) {
        try {
            fileName = new String(fileName.getBytes("UTF-8"), "iso-8859-1");
            response.addHeader("Content-Disposition", String.format("attachment;fileName=%s", fileName));
            response.addHeader("Content-Type", "image/png");
            response.addHeader("Accept-Ranges", "bytes");
            ImgUtil.writePng(bufferedImage, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void render(File file, HttpServletResponse response) {
        try {
            String fileName = file.getName();
            fileName = new String(fileName.getBytes("UTF-8"), "iso-8859-1");
            response.addHeader("Content-Disposition", String.format("attachment;fileName=%s", fileName));
            response.addHeader("Content-Type", "image/png");
            response.addHeader("Accept-Ranges", "bytes");
            IoUtil.copy(new FileInputStream(file), response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String pngNameByTitle(String title) {
        title = String.format("%s-%s.%s", DateUtil.format(new Date(), "yyyyMMdd-hhmmss"), title, "png");
//        title = Arrays.stream(title.split("-")).filter(e -> !e.contains("天猫")).collect(Collectors.joining());
        return title;
    }

    public void tmallShot(String url, HttpServletResponse response) {
        log.info("网页截图:{}", url);
        WebDriver driver = getDriver();
        driver.get(url);
        WebElement content = driver.findElement(By.id("content"));
        int y = content.getLocation().getY();

        WebElement copyright = driver.findElement(By.id("footer"));
        int footerHeight = copyright.getSize().height;

        String title = pngNameByTitle(driver.getTitle());
        log.info("保存为:{},({},{})", title, y, footerHeight);
        BufferedImage bufferedImage = doShot(driver, new FixedCutStrategy(y, footerHeight));
        render(bufferedImage, title, response);
        driver.close();
    }

    public void fullScreenShot(String url, HttpServletResponse response) {
        log.info("网页截图:{}", url);
        WebDriver driver = getDriver();
        driver.get(url);
        String title = pngNameByTitle(driver.getTitle());
        log.info("保存为:{}", title);
        BufferedImage bufferedImage = doShot(driver, null);
        render(bufferedImage, title, response);
        driver.close();
    }

    public void fullShotMulty(List<String> url, HttpServletResponse response) throws Exception {
        String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        String md5 = DateUtil.format(new Date(), "yyyyMMddHHmmss") + DigestUtil.md5Hex(url.get(0));
        FileUtil.mkdir(path + md5);
        List<File> files = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(url.size());
        ArrayList<Callable<ShotDto>> list = new ArrayList<>();
        for (String s : url) {
            list.add(new Callable<ShotDto>() {
                @Override
                public ShotDto call() throws Exception {
                    log.info("网页截图:{}", s);
                    WebDriver driver = getDriver();
                    driver.get(s);//访问
                    String title = pngNameByTitle(driver.getTitle());
                    BufferedImage bufferedImage = doShot(driver, null);//截图
                    return new ShotDto(title, bufferedImage);

                }
            });
        }
        List<Future<ShotDto>> futures = executorService.invokeAll(list);//模拟浏览器访问，并截图
        log.info("创建目录:{}", path + md5);
        for (Future<ShotDto> future : futures) {
            //阻塞获取
            ShotDto shotDto = future.get();
            final File file = new File(path + md5 + File.separator + shotDto.getName());
            file.setReadable(true);
            file.setWritable(true);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ImgUtil.writePng(shotDto.getBufferedImage(), fileOutputStream);//写到临时文件夹：path + md5 + File.separator + shotDto.getName()
            fileOutputStream.close();
            files.add(file);
        }

        final File cpFile = FileUtil.file(path + md5 + ".zip");
        cpFile.deleteOnExit();
        cpFile.setReadable(true);
        cpFile.setWritable(true);
        log.info("创建压缩包:{}", cpFile.getName());

        Archiver archiver = CompressUtil.createArchiver(CharsetUtil.CHARSET_UTF_8, ArchiveStreamFactory.ZIP, cpFile);//压缩文件
        for (File file : files) {
            archiver.add(file);
        }
        archiver.finish().close();

        render(cpFile, response);//响应前端

        FileUtil.del(path + md5);//删除临时目录
//        FileUtil.del(cpFile);
    }


    public static final String serverUrl = "https://sctapi.ftqq.com/%s.send?title=%s";

    @Value("${serverToken}")
    private String serverToken;

    public String sendMsg(String title) {
        title = UrlEncoded.decodeString(title);
        String content = String.format(serverUrl, serverToken, title);
        log.info("提醒打卡:{}", content);
        String s = HttpUtil.get(content);
        log.info("提醒打卡:{},{}", content, s);
        return s;
    }

    //    public static void main(String[] args) throws Exception {
//        String url = "https://conba.tmall.com";
//        log.info("网页截图:{}", url);
//        WebDriver driver = getDriver();
//        driver.get(url);
//        WebElement content = driver.findElement(By.id("content"));
//        int y = content.getLocation().getY();
//
//        WebElement copyright = driver.findElement(By.id("footer"));
//        int footerHeight = copyright.getSize().height;
//
//        String title = pngNameByTitle(driver.getTitle());
//        log.info("网页截图:{},({},{})", title, y, footerHeight);
//        BufferedImage bufferedImage = doShot(driver, new FixedCutStrategy(y, footerHeight));
//        ImgUtil.writePng(bufferedImage, new FileImageOutputStream(new File("E:\\go\\tmall-java\\tmall-java\\2.png")));
//        driver.close();
//    }
}

