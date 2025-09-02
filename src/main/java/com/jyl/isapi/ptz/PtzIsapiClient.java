package com.jyl.isapi.ptz;

import com.jyl.isapi.config.PtzProperties;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PtzIsapiClient implements AutoCloseable {
    private final PtzProperties props;
    private final CloseableHttpClient client;

    public PtzIsapiClient(PtzProperties props) {
        this.props = props;

        BasicCredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials(
                new AuthScope(props.getHost(), props.getPort()),
                new UsernamePasswordCredentials(props.getUsername(), props.getPassword().toCharArray())
        );

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(5_000))
                .setResponseTimeout(Timeout.ofMilliseconds(10_000))
                .build();


        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(20);

        this.client = HttpClients.custom()
                .setDefaultCredentialsProvider(creds)
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    private String handleText(ClassicHttpResponse resp) throws IOException, ParseException {
        int code = resp.getCode();
        HttpEntity entity = resp.getEntity();
        String body = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";
        if (code / 100 != 2) {
            throw new IOException("HTTP " + code + " -> " + body);
        }
        return body;
    }

    private String url(String path) {
        return props.getBaseUrl() + path;
    }

    /**
     * 列出 PTZ 通道（看看是 1 还是 101）
     */
    public String getChannels() throws IOException {
        HttpGet req = new HttpGet(url("/ISAPI/PTZCtrl/channels"));
        req.addHeader("Accept", "application/xml");
        return client.execute(req, this::handleText);
    }

    /**
     * 查询能力（速度范围等），便于限制 pan/tilt/zoom 速度
     */
    public String getCapabilities(int channel) throws IOException {
        HttpGet req = new HttpGet(url("/ISAPI/PTZCtrl/channels/" + channel + "/capabilities"));
        req.addHeader("Accept", "application/xml");
        return client.execute(req, this::handleText);
    }

    /**
     * 跳转预置位（最稳妥的“回家位”/快速指向）
     */
    public void gotoPreset(int channel, int presetId) throws IOException {
        String xml = "<PTZPreset><id>" + presetId + "</id></PTZPreset>";
        HttpPut put = new HttpPut(url("/ISAPI/PTZCtrl/channels/" + channel + "/presets/" + presetId + "/goto"));
        put.addHeader("Accept", "application/xml");
        put.setEntity(new StringEntity(xml, ContentType.APPLICATION_XML));
        client.execute(put, this::handleText);
    }

    /**
     * 连续移动（速度范围通常在 -1.0 ~ 1.0，具体看 capabilities）
     */
/*    public void continuousMove(int channel, double pan, double tilt, double zoom, int durationMs) throws IOException {
        String xml = ""
                + "<PTZData>"
                + "<pan>" + pan + "</pan>"
                + "<tilt>" + tilt + "</tilt>"
                + "<zoom>" + zoom + "</zoom>"
                + "<timeout>" + durationMs + "</timeout>"
                + "</PTZData>";
        HttpPut put = new HttpPut(url("/ISAPI/PTZCtrl/channels/" + channel + "/continuous"));
        put.addHeader("Accept", "application/xml");
        put.setEntity(new StringEntity(xml, ContentType.APPLICATION_XML));
        client.execute(put, this::handleText);
    }*/

    public void continuousMove(int channel, double pan, double tilt, double zoom, int durationMs) throws IOException {
        int p = (int) Math.round(pan);
        int t = (int) Math.round(tilt);
        int z = (int) Math.round(zoom);

        p = Math.max(-100, Math.min(100, p));
        t = Math.max(-100, Math.min(100, t));
        z = Math.max(-100, Math.min(100, z));

        String xml = "<PTZData>"
                + "<pan>"  + p + "</pan>"
                + "<tilt>" + t + "</tilt>"
                + "<zoom>" + z + "</zoom>"
                + "<timeout>" + durationMs + "</timeout>"
                + "</PTZData>";

        HttpPut put = new HttpPut(url("/ISAPI/PTZCtrl/channels/" + channel + "/continuous"));
        put.addHeader("Accept", "application/xml");
        put.setEntity(new StringEntity(xml, ContentType.APPLICATION_XML));
        client.execute(put, this::handleText);
    }


    /**
     * 停止（同时停止 pan/tilt/zoom）
     */
/*    public void stop(int channel) throws IOException {
        String xml = "<PTZStop><pan>true</pan><tilt>true</tilt><zoom>true</zoom></PTZStop>";
        HttpPut put = new HttpPut(url("/ISAPI/PTZCtrl/channels/" + channel + "/stop"));
        put.addHeader("Accept", "application/xml");
        put.setEntity(new StringEntity(xml, ContentType.APPLICATION_XML));
        client.execute(put, this::handleText);
    }*/

    public void stop(int channel) throws IOException {
        IOException last = null;

        // 方案1：PUT /stop + XML（多数型号支持）
        try {
            String xml = "<PTZStop><pan>true</pan><tilt>true</tilt><zoom>true</zoom></PTZStop>";
            HttpPut put = new HttpPut(url("/ISAPI/PTZCtrl/channels/" + channel + "/stop"));
            put.addHeader("Accept", "application/xml");
            put.setEntity(new StringEntity(xml, ContentType.APPLICATION_XML));
            client.execute(put, this::handleText);
            return;
        } catch (IOException e) { last = e; }

        // 方案2：POST /stop（个别固件需要 POST，无 body）
        try {
            HttpPost post = new HttpPost(url("/ISAPI/PTZCtrl/channels/" + channel + "/stop"));
            post.addHeader("Accept", "application/xml");
            client.execute(post, this::handleText);
            return;
        } catch (IOException e) { last = e; }

        // 方案3：0 速度连续移动，极短超时（通杀）
        try {
            String xml = "<PTZData><pan>0</pan><tilt>0</tilt><zoom>0</zoom><timeout>1</timeout></PTZData>";
            HttpPut put = new HttpPut(url("/ISAPI/PTZCtrl/channels/" + channel + "/continuous"));
            put.addHeader("Accept", "application/xml");
            put.setEntity(new StringEntity(xml, ContentType.APPLICATION_XML));
            client.execute(put, this::handleText);
            return;
        } catch (IOException e) { last = e; }

        throw last; // 三种都失败才抛
    }

    /**
     * 支持绝对移动
     * @param channel
     * @param x
     * @param y
     * @param z
     * @param speed
     * @throws IOException
     */
/*    public void absoluteMove(int channel, Integer x, Integer y, Integer z, Integer speed) throws IOException {
        // 1) 必要性检查 & 限幅（按你的 capability：X 0~3500, Y -49~900, Z 10~40）
        if (x == null && y == null && z == null) throw new IllegalArgumentException("x/y/z 至少给一个");
        Integer X = x != null ? Math.max(0, Math.min(3500, x)) : null;
        Integer Y = y != null ? Math.max(-49, Math.min(900, y)) : null;
        Integer Z = z != null ? Math.max(10, Math.min(40, z)) : null;
        Integer S = speed != null ? Math.max(0, Math.min(100, speed)) : null;

        // 2) 先试格式 A：<PTZAbsolute>
        StringBuilder a = new StringBuilder("<PTZAbsolute>");
        if (X != null) a.append("<pan>").append(X).append("</pan>");
        if (Y != null) a.append("<tilt>").append(Y).append("</tilt>");
        if (Z != null) a.append("<zoom>").append(Z).append("</zoom>");
        if (S != null) a.append("<speed>").append(S).append("</speed>");
        a.append("</PTZAbsolute>");

        String path = "/ISAPI/PTZCtrl/channels/" + channel + "/absolute";
        HttpPut putA = new HttpPut(url(path));
        putA.addHeader("Accept", "application/xml");
        putA.setEntity(new StringEntity(a.toString(), ContentType.APPLICATION_XML));
        try {
            client.execute(putA, this::handleText);
            return;
        } catch (IOException tryB) {
            // 3) 回退格式 B：<PTZData> + absolutePan/absoluteTilt/absoluteZoom
            StringBuilder b = new StringBuilder("<PTZData>");
            if (X != null) b.append("<absolutePan>").append(X).append("</absolutePan>");
            if (Y != null) b.append("<absoluteTilt>").append(Y).append("</absoluteTilt>");
            if (Z != null) b.append("<absoluteZoom>").append(Z).append("</absoluteZoom>");
            if (S != null) b.append("<speed>").append(S).append("</speed>");
            b.append("</PTZData>");

            HttpPut putB = new HttpPut(url(path));
            putB.addHeader("Accept", "application/xml");
            putB.setEntity(new StringEntity(b.toString(), ContentType.APPLICATION_XML));
            client.execute(putB, this::handleText);
        }
    }*/

    /**
     * 绝对移动（按“度”传入），优先 /absolute (AbsoluteHigh, 0.1°刻度)，失败则回退 /absoluteEx（度）
     * @param channel   PTZ 通道
     * @param azimuthDeg    方位角（度，0~350）
     * @param elevationDeg  俯仰角（度，-4.9~90.0）
     * @param zoom          可选，倍率（10~40；按你的能力表）
     */
    public void absoluteMoveDegrees(int channel, Double azimuthDeg, Double elevationDeg, Integer zoom) throws IOException {
        // 你的能力范围（来自 /ptz/capabilities）
        // XRange: 0~3500  => 0.0° ~ 350.0°  （方位 azimuth）
        // YRange: -49~900 => -4.9° ~ 90.0°  （俯仰 elevation）
        // ZRange: 10~40   => 变倍
        Integer az10 = null, el10 = null;

        if (azimuthDeg != null) {
            az10 = (int)Math.round(azimuthDeg * 10.0);
            az10 = clampInt(az10, 0, 3500);
        }
        if (elevationDeg != null) {
            el10 = (int)Math.round(elevationDeg * 10.0);
            el10 = clampInt(el10, -49, 900);
        }
        Integer z = (zoom != null) ? clampInt(zoom, 10, 40) : null;

        // 先试：/absolute + PTZData/AbsoluteHigh（0.1°刻度，绝大多数机型通用）
        IOException last = null;
        try {
            StringBuilder sb = new StringBuilder(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<PTZData version=\"2.0\" xmlns=\"http://www.hikvision.com/ver20/XMLSchema\">\n" +
                            "  <AbsoluteHigh>\n"
            );
            if (el10 != null) sb.append("    <elevation>").append(el10).append("</elevation>\n");
            if (az10 != null) sb.append("    <azimuth>").append(az10).append("</azimuth>\n");
            if (z != null)   sb.append("    <absoluteZoom>").append(z).append("</absoluteZoom>\n");
            sb.append("  </AbsoluteHigh>\n</PTZData>");

            HttpPut put = new HttpPut(url("/ISAPI/PTZCtrl/channels/" + channel + "/absolute"));
            put.addHeader("Accept", "application/xml");
            put.setEntity(new StringEntity(sb.toString(), ContentType.APPLICATION_XML));
            client.execute(put, this::handleText); // 2xx 则返回
            return;
        } catch (IOException e) {
            last = e; // 可能是 badXmlContent / Momentary missing 等历史 schema 差异
        }

        // 回退：/absoluteEx（单位=度，可小数）
        try {
            StringBuilder sb = new StringBuilder(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<PTZAbsoluteEx version=\"2.0\" xmlns=\"http://www.hikvision.com/ver20/XMLSchema\">\n"
            );
            if (elevationDeg != null) sb.append("  <elevation>").append(String.format(java.util.Locale.US,"%.1f", elevationDeg)).append("</elevation>\n");
            if (azimuthDeg != null)   sb.append("  <azimuth>").append(String.format(java.util.Locale.US,"%.1f", azimuthDeg)).append("</azimuth>\n");
            if (z != null)            sb.append("  <absoluteZoom>").append(z).append("</absoluteZoom>\n");
            sb.append("</PTZAbsoluteEx>");

            HttpPut putEx = new HttpPut(url("/ISAPI/PTZCtrl/channels/" + channel + "/absoluteEx"));
            putEx.addHeader("Accept", "application/xml");
            putEx.setEntity(new StringEntity(sb.toString(), ContentType.APPLICATION_XML));
            client.execute(putEx, this::handleText);
            return;
        } catch (IOException e2) {
            last = e2;
        }

        throw last;
    }


    // 把值限定到 [min,max]
    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** 读取 PTZ 状态（可拿到 AbsoluteHigh 当前角度/变倍） */
    public String getStatus(int channel) throws IOException {
        HttpGet get = new HttpGet(url("/ISAPI/PTZCtrl/channels/" + channel + "/status"));
        get.addHeader("Accept", "application/xml");
        return client.execute(get, this::handleText);
    }



    @Override
    public void close() throws Exception {
        client.close();
    }
}

