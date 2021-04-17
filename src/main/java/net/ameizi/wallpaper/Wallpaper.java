package net.ameizi.wallpaper;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Wallpaper {

    private static final Path readmePath = Paths.get("README.md");
    private static final Path wallpaperListPath = Paths.get("bing-wallpaper.md");

    // 请求API地址
    private static final String BING_API = "https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&nc=1618537156988&pid=hp&uhd=1&uhdwidth=3840&uhdheight=2160";
    // 最近7天
    private static final String BING_7DAYS_API = "https://cn.bing.com/HPImageArchive.aspx?format=js&idx=%d&n=1&nc=1618537156988&pid=hp&uhd=1&uhdwidth=3840&uhdheight=2160";
    // 图片访问地址
    private static final String BING_URL = "https://cn.bing.com%s";

    public static void main(String[] args) throws Exception {
        // 读取 wallpaper 列表
        List<Image> imageList = readWallPaperList();
        // imageList = last7Days(imageList);
        // 请求网络图片
        String resp = HttpUtil.get(BING_API);
        // 解析成 Image对象
        Image image = fetch(resp);
        // 在imageList下标为0的位置插入image
        imageList.add(0, image);
        // 写入bing-wallpaper.md文件
        writeToWallPaperList(imageList);
        // 写入README.md文件
        writeToReadme(imageList);
    }

    /**
     * 请求最近7天的数据
     *
     * @param imageList
     * @return
     */
    private static List<Image> last7Days(List<Image> imageList) {
        for (int i = 0; i < 8; i++) {
            String resp = HttpUtil.get(String.format(BING_7DAYS_API, i));
            imageList.add(fetch(resp));
        }
        return imageList;
    }

    /**
     * 解析请求体json字符串
     *
     * @param resp
     * @return
     */
    private static Image fetch(String resp) {
        JSONObject jsonObject = JSONUtil.parseObj(resp);
        JSONObject images = jsonObject.getJSONArray("images").get(0, JSONObject.class);

        String url = String.format(BING_URL, images.getStr("url"));
        url = url.substring(0, url.indexOf("&"));
        String desc = images.getStr("copyright");
        String enddate = images.getStr("enddate");

        LocalDate localDate = LocalDate.parse(enddate, DateTimeFormatter.BASIC_ISO_DATE);
        enddate = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        return new Image(enddate, desc, url);
    }

    /**
     * 解析 bing-wallpaper.md
     *
     * @return
     * @throws Exception
     */
    public static List<Image> readWallPaperList() throws Exception {
        List<Image> imageList = new ArrayList<>();
        Files.readAllLines(wallpaperListPath, StandardCharsets.UTF_8).forEach(line -> {
            Pattern pattern = Pattern.compile("(.*)\\s\\|.*\\[(.*)\\]\\((.*)\\)");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                Image image = new Image(matcher.group(1), matcher.group(2), matcher.group(3));
                imageList.add(image);
            }
        });
        return imageList;
    }

    /**
     * 写入 bing-wallpaper.md
     *
     * @param imageList
     * @throws Exception
     */
    public static void writeToWallPaperList(List<Image> imageList) throws Exception {
        File wallpaper = wallpaperListPath.toFile();
        if (!wallpaper.exists()) {
            wallpaper.createNewFile();
        }
        FileUtil.writeUtf8String("## Bing Wallpaper", wallpaper);
        FileUtil.appendUtf8String(System.lineSeparator() + System.lineSeparator(), wallpaper);
        imageList.stream().distinct().collect(Collectors.toList()).forEach(item -> {
            FileUtil.appendUtf8String(item.markdown(), wallpaper);
        });
    }

    /**
     * 写入 README.md
     *
     * @param imageList
     * @throws Exception
     */
    public static void writeToReadme(List<Image> imageList) throws Exception {
        File readme = readmePath.toFile();
        if (!readme.exists()) {
            readme.createNewFile();
        }
        FileUtil.writeUtf8String("## Bing Wallpaper", readme);
        FileUtil.appendUtf8String(System.lineSeparator() + System.lineSeparator(), readme);
        // 取出第一个元素设为首图
        Image image = imageList.get(0);
        String top = String.format("![%s](%s)", image.getDesc(), image.largeUrl()) + System.lineSeparator();
        FileUtil.appendUtf8String(top, readme);
        // 设置描述内容
        String today = String.format("Today: [%s](%s)", image.getDesc(), image.getUrl()) + System.lineSeparator();
        FileUtil.appendUtf8String(today, readme);
        // 拼markdown表头
        FileUtil.appendUtf8String("|      |      |      |" + System.lineSeparator(), readme);
        FileUtil.appendUtf8String("| :--: | :--: | :--: |" + System.lineSeparator(), readme);
        List<Image> images = imageList.stream().distinct().collect(Collectors.toList());
        int i = 1;
        for (Image item : images) {
            // 写入markdown格式字符串
            FileUtil.appendUtf8String("|" + item.toString(), readme);
            // 每行三列，若刚整除，补每行末尾最后一个"|"
            if (i % 3 == 0) {
                FileUtil.appendUtf8String("|" + System.lineSeparator(), readme);
            }
            // 行数加1
            i++;
        }
        if (i % 3 != 1) {
            FileUtil.appendUtf8String("|" + System.lineSeparator(), readme);
        }
    }

}
