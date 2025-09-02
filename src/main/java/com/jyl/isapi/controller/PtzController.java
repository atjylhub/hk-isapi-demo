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
