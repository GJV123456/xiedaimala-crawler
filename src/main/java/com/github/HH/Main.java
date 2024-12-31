package com.github.HH;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {

        // 待处理的链接池
        List<String> linkPool = new ArrayList<>();
        // 已经处理过的连接池
        Set<String> preocessedLink = new HashSet<>();

        linkPool.add("https://sina.cn/?from=sinacom");

        while (true) {
            // 测试
            // 链接全部处理完毕
            if (linkPool.isEmpty()) {
                break;
            }

            // 获取链接；ArrayList从尾部删除最有效率,然而remove会返回删除的数据
            String link = linkPool.remove(linkPool.size() - 1);

            // 判断是否有处理过该链接
            if (preocessedLink.contains(link)) {
                continue;
            }

            // 凡事以sina.cn结尾的链接均为需要处理的，排除那些广告
            if (isNeedLink(link)) {
                // 只处理相关信息
                // 若出现//news.sina.cn/...这种类似链接导致无法识别，拼接上https：
                if (link.startsWith("//")) {
                    link = "https:" + link;
                    System.out.println(link);
                }

                // 调用 httpGetAndParseHtml 方法进行请求和解析
                Document doc = httpGetAndParseHtml(link);

                // 选择所有的a标签
                ArrayList<Element> links = doc.select("a");

                // 获取href属性并丢到待处理的链接池中
                links.stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);

                // 若为新闻页面则存入数据库
                storeIntoDatabaseIfItIsNewsPage(doc);

                // 将处理后的链接丢入已处理过的链接池
                preocessedLink.add(link);

            } else {
                // 不需要处理的
                continue;
            }
        }
    }

    // http的get请求与页面解析方法抽取
    private static Document httpGetAndParseHtml(String link) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            ClassicHttpRequest httpGet = ClassicRequestBuilder.get(link)
                    .build();
            httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

            // 执行请求并解析HTML
            return httpclient.execute(httpGet, response -> {
                System.out.println(response.getCode() + " " + response.getReasonPhrase());
                final HttpEntity entity = response.getEntity();

                // 获取响应的内容
                String html = EntityUtils.toString(entity);

                // 使用Jsoup解析HTML并返回Document对象
                return Jsoup.parse(html);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            // 非空则进行操作
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }
    }

    private static boolean isNeedLink(String link) {
        return isNotLoginPage(link) && isNewsPage(link) || isIndexPage(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn/?from=sinacom".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}
