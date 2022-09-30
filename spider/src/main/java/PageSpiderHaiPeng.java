
import us.codecraft.webmagic.*;
import us.codecraft.webmagic.pipeline.FilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PageSpiderHaiPeng implements PageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PageSpiderHaiPeng.class);
    private Site site = Site.me().setRetryTimes(3).setSleepTime(10000).setTimeOut(10000);

    @Override
    public void process(Page page) {
        page.putField("page", page.getHtml());
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider.create(new PageSpiderHaiPeng())
                .addUrl("http://xxx")
                .addPipeline(new FilePipelineHaiPeng("D:\\王锦乐\\java_sample\\spider\\output"))
                .thread(1)
                .run();
    }
}
