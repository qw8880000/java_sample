import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LVideoDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(LVideoDownloader.class);
    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String VIDEO_OUTPUT_DIR = PROJECT_DIR + File.separator + "video_output";
    private HikariDataSource datasource;

    public LVideoDownloader() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/l_fun");
        config.setUsername("root");
        config.setPassword("huangwang");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.datasource = new HikariDataSource(config);
    }

    public void downloadVideoPageAndParse() {
        try {
            Connection conn = this.datasource.getConnection();
            PreparedStatement ps = conn.prepareStatement("select id,url from l_video_pages where video_is_downloaded = 0 and  TIMESTAMPDIFF(minute, url_parse_date , now()) > 30");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                Thread.sleep(10);  // 加入延时，不要请求太快了，防止网站崩溃


                String url = rs.getString("url");
                int id = rs.getInt("id");


                Document doc = Jsoup.connect(url).get();    // 直接从一个url中获取到document对象

                Element ele_video_src = doc.getElementsByTag("source").first(); // 获取video_src
                Element ele_description = doc.select("meta[name='description']").first();
                if(ele_video_src == null) {continue;}

                String title = ele_description != null ? ele_description.attr("content") : "";
                String video_src = ele_video_src.attr("src").replace("amp;", "");   // 去掉video_src 中的无用字符串 "amp;"

                // LOGGER.info(ele_video_src.toString());
                LOGGER.info(String.format("url parse: src=%s, title=%s", video_src, title));

                PreparedStatement ps1 = conn.prepareStatement("update l_fun.l_video_pages set video_src=?,title=?,url_parse_date=now() where id=?");
                ps1.setString(1, video_src);
                ps1.setString(2, title);
                ps1.setInt(3, id);
                ps1.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int runVLC(String video_src, String video_output) {
        //执行命令
        try {
            Runtime runtime = Runtime.getRuntime(); //获取Runtime实例
            String command = String.format("vlc \"%s\" --sout=\"#standard{access=file,mux=ts,dst='%s'}\" vlc://quit", video_src, video_output);
            Process process = runtime.exec(command);

            // 标准错误流（必须写在 waitFor 之前）
            InputStream errInputStream = process.getErrorStream();
            int proc = process.waitFor();

            if (proc == 0) {
                LOGGER.info("执行成功" + video_src);
            } else {
                LOGGER.warn("vlc执行失败" + video_src);
                InputStreamReader isr = new InputStreamReader(errInputStream, "GBK");
                BufferedReader br = new BufferedReader(isr);
                String line =  br.readLine();
                while (line != null) {
                    LOGGER.warn(line);
                    line =  br.readLine();
                }
            }

            return 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return 1;
        }
    }

    private String generateFilename(String video_src) {
        Pattern pafengli = Pattern.compile("mp4/(.*?)/index.m3u8");
        Matcher matcher = pafengli.matcher(video_src);

        if(matcher.find())
        {
            return matcher.group(1);  //group为捕获组
        }
        else {
            Date day = new Date();
            SimpleDateFormat sdf= new SimpleDateFormat("yyyyMMddHHmmss");
            return sdf.format(day) + ".mp4";
        }
    }

    public void downloadVideo() {
        try {
            Connection conn = this.datasource.getConnection();
            PreparedStatement ps = conn.prepareStatement("select id,video_src from l_fun.l_video_pages where video_is_downloaded = 0");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Thread.sleep(10); // 加入延时，不要请求太快了，防止网站崩溃


                int id = rs.getInt("id");
                String video_src = rs.getString("video_src");

                // download video
                String video_file_name =  this.generateFilename(video_src);
                int res = this.runVLC(video_src, VIDEO_OUTPUT_DIR + File.separator + video_file_name);

                if(res == 0) {
                    PreparedStatement ps1 = conn.prepareStatement("update l_fun.l_video_pages set video_is_downloaded=1,video_file_name=? where id=?");
                    ps1.setString(1, video_file_name);
                    ps1.setInt(2, id);
                    ps1.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)  {
        LVideoDownloader videoDownloader = new LVideoDownloader();

        videoDownloader.downloadVideoPageAndParse();
        videoDownloader.downloadVideo();

    }
}
