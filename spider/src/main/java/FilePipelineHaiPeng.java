import org.apache.commons.codec.digest.DigestUtils;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

public class FilePipelineHaiPeng extends FilePersistentBase implements Pipeline {

    private static final Logger logger = LoggerFactory.getLogger(FilePipelineHaiPeng.class);
    private PrintWriter printWriter = null;
    private String path = null;

    public FilePipelineHaiPeng(String path) {
        this.path = path + PATH_SEPERATOR;
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        try {
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.getFile(this.path + "output" + ".html")), "UTF-8"));
            Iterator var5 = resultItems.getAll().entrySet().iterator();

            while(true) {
                while(var5.hasNext()) {
                    Map.Entry<String, Object> entry = (Map.Entry)var5.next();
                    if (entry.getValue() instanceof Iterable) {
                        Iterable value = (Iterable)entry.getValue();
                        Iterator var8 = value.iterator();

                        while(var8.hasNext()) {
                            Object o = var8.next();
                            printWriter.println(o);
                        }
                    } else {
                        printWriter.println(entry.getValue());
                    }

                    printWriter.close();
                }

                //printWriter.close();
                break;
            }
        } catch (IOException var10) {
            this.logger.warn("write file error", var10);
        }
    }
}
