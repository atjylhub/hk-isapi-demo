package com.jyl.isapi.controller;

import com.jyl.isapi.config.PtzProperties;
import com.jyl.isapi.ptz.PtzIsapiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ptz")
public class PtzController {
    private final PtzProperties props;
    private final PtzIsapiClient client;

    public PtzController(PtzProperties props, PtzIsapiClient client) {
        this.props = props;
        this.client = client;
    }

    /**
     * 读取当前守望配置
     * @param ch
     * @return
     * @throws Exception
     */
    @GetMapping("/park/get")
    public ResponseEntity<String> getPark(@RequestParam(required = false) Integer ch) throws Exception {
        int channel = (ch != null) ? ch : props.getChannel();
        return ResponseEntity.ok(client.getParkAction(channel));
    }

    /**
     * 设置守望配置（可开/关 + 设置回位时间与目标动作）
     * @param enabled
     * @param parkTime
     * @param actionType
     * @param actionNum
     * @param ch
     * @return
     * @throws Exception
     */
    @PostMapping("/park/set")
    public ResponseEntity<Void> setPark(@RequestParam boolean enabled,
                                        @RequestParam(defaultValue = "300") int parkTime,      // 秒
                                        @RequestParam(defaultValue = "preset") String actionType, // preset 或 patrol
                                        @RequestParam(defaultValue = "1") int actionNum,       // 预置位号或巡航号
                                        @RequestParam(required = false) Integer ch) throws Exception {
        int channel = (ch != null) ? ch : props.getChannel();
        client.setParkAction(channel, enabled, parkTime, actionType, actionNum);
        return ResponseEntity.ok().build();
    }

    // 便捷关闭（测试期最常用）
    @PostMapping("/park/disable")
    public ResponseEntity<Void> disablePark(@RequestParam(required = false) Integer ch) throws Exception {
        int channel = (ch != null) ? ch : props.getChannel();
        // actionType/actionNum 仍需带齐以兼容某些固件要求；这里默认 preset 到 1 号
        client.setParkAction(channel, false, 300, "preset", 1);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/3d/box-smart")
    public ResponseEntity<Void> threeDBoxSmart(@RequestParam double x1, @RequestParam double y1,
                                               @RequestParam double x2, @RequestParam double y2,
                                               @RequestParam(required=false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        client.threeDZoomSmart(channel, x1, y1, x2, y2);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/3d/box")
    public ResponseEntity<Void> threeDBox(@RequestParam double x1, @RequestParam double y1,
                                          @RequestParam double x2, @RequestParam double y2,
                                          @RequestParam(required=false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        client.threeDZoomBox(channel, x1, y1, x2, y2);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/3d/click-in")
    public ResponseEntity<Void> threeDClickIn(@RequestParam double x, @RequestParam double y,
                                              @RequestParam(defaultValue="0.08") double size,
                                              @RequestParam(required=false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        client.threeDZoomInAt(channel, x, y, size);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/3d/click-out")
    public ResponseEntity<Void> threeDClickOut(@RequestParam double x, @RequestParam double y,
                                               @RequestParam(defaultValue="0.30") double size,
                                               @RequestParam(required=false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        client.threeDZoomOutAt(channel, x, y, size);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/absolute")
    public ResponseEntity<Void> absolute(@RequestParam(required=false) Double az,   // 方位角（度）
                                         @RequestParam(required=false) Double el,   // 俯仰角（度）
                                         @RequestParam(required=false) Integer z,   // 变倍
                                         @RequestParam(required=false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        client.absoluteMoveDegrees(channel, az, el, z);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<String> status(@RequestParam(required=false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        return ResponseEntity.ok(client.getStatus(channel));
    }

    @GetMapping("/channels")
    public ResponseEntity<String> channels() throws Exception {
        return ResponseEntity.ok(client.getChannels());
    }

    @GetMapping("/capabilities")
    public ResponseEntity<String> caps(@RequestParam(required = false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        return ResponseEntity.ok(client.getCapabilities(channel));
    }

    @PostMapping("/preset/goto")
    public ResponseEntity<Void> gotoPreset(@RequestParam int preset,
                                           @RequestParam(required = false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        client.gotoPreset(channel, preset);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/move")
    public ResponseEntity<Void> move(@RequestParam double pan,
                                     @RequestParam double tilt,
                                     @RequestParam(defaultValue = "0") double zoom,
                                     @RequestParam(defaultValue = "1000") int ms,
                                     @RequestParam(required = false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        client.continuousMove(channel, pan, tilt, zoom, ms);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stop")
    public ResponseEntity<Void> stop(@RequestParam(required = false) Integer ch) throws Exception {
        int channel = ch != null ? ch : props.getChannel();
        client.stop(channel);
        return ResponseEntity.ok().build();
    }
}
