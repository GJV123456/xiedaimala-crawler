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
            if (link.contains("sina.cn") && !link.contains("passport.sina.cn") && link.contains("news.sina.cn") || "https://sina.cn/?from=sinacom".equals(link)) {
                // 只处理相关信息
                System.out.println(link);

                // 若出现//news.sina.cn/...这种类似链接导致无法识别，拼接上https：
                if (link.startsWith("//")) {
                    link = "https:" + link;
                    System.out.println(link);
                }

                try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                    ClassicHttpRequest httpGet = ClassicRequestBuilder.get(link)
                            .build();
                    httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
                    httpclient.execute(httpGet, response -> {

                        System.out.println(response.getCode() + " " + response.getReasonPhrase());
                        final HttpEntity entity1 = response.getEntity();

                        String html = EntityUtils.toString(entity1);

                        Document doc = Jsoup.parse(html);

                        // 选择所有的a标签
                        ArrayList<Element> links = doc.select("a");

                        // 获取href属性并丢到待处理的链接池中
                        for (Element aTag : links) {
                            String href = aTag.attr("href");
                            linkPool.add(href);
                        }

                        // 若为新闻页面则存入数据库
                        ArrayList<Element> articleTags = doc.select("article");
                        if (!articleTags.isEmpty()) {
                            // 非空则进行操作
                            for (Element articleTag : articleTags) {
                                String title = articleTags.get(0).child(0).text();
                                System.out.println(title);
                            }
                        }


                        return null;
                    });
                }
            } else {
                // 不需要处理的
                continue;
            }
            preocessedLink.add(link);

        }
    }
}
