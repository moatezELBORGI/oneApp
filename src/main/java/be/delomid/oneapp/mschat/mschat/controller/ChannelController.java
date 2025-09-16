package be.delomid.oneapp.mschat.mschat.controller;


import be.delomid.oneapp.mschat.mschat.dto.ChannelDto;
import be.delomid.oneapp.mschat.mschat.dto.CreateChannelRequest;
import be.delomid.oneapp.mschat.mschat.dto.ResidentDto;
import be.delomid.oneapp.mschat.mschat.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    public ResponseEntity<ChannelDto> createChannel(
            @Valid @RequestBody CreateChannelRequest request,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        ChannelDto channel = channelService.createChannel(request, userId);
        return ResponseEntity.ok(channel);
    }

    @GetMapping
    public ResponseEntity<Page<ChannelDto>> getUserChannels(
            Authentication authentication,
            Pageable pageable) {
        
        String userId = getUserId(authentication);
        Page<ChannelDto> channels = channelService.getUserChannels(userId, pageable);
        return ResponseEntity.ok(channels);
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<ChannelDto> getChannel(
            @PathVariable Long channelId,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        ChannelDto channel = channelService.getChannelById(channelId, userId);
        return ResponseEntity.ok(channel);
    }

    @PostMapping("/{channelId}/join")
    public ResponseEntity<ChannelDto> joinChannel(
            @PathVariable Long channelId,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        ChannelDto channel = channelService.joinChannel(channelId, userId);
        return ResponseEntity.ok(channel);
    }

    @PostMapping("/{channelId}/leave")
    public ResponseEntity<Void> leaveChannel(
            @PathVariable Long channelId,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        channelService.leaveChannel(channelId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/direct/{otherUserId}")
    public ResponseEntity<ChannelDto> getOrCreateDirectChannel(
            @PathVariable String otherUserId,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        Optional<ChannelDto> channel = channelService.getOrCreateOneToOneChannel(userId, otherUserId);
        
        return channel.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/building/{buildingId}")
    public ResponseEntity<List<ChannelDto>> getBuildingChannels(
            @PathVariable String buildingId,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        List<ChannelDto> channels = channelService.getBuildingChannels(buildingId, userId);
        return ResponseEntity.ok(channels);
    }

    @GetMapping("/building/{buildingId}/residents")
    public ResponseEntity<List<ResidentDto>> getBuildingResidents(
            @PathVariable String buildingId,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        List<ResidentDto> residents = channelService.getBuildingResidents(buildingId, userId);
        return ResponseEntity.ok(residents);
    }

    @PostMapping("/building/{buildingId}/create")
    public ResponseEntity<ChannelDto> createBuildingChannel(
            @PathVariable String buildingId,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        ChannelDto channel = channelService.createBuildingChannel(buildingId, userId);
        return ResponseEntity.ok(channel);
    }

    private String getUserId(Authentication authentication) {
        return ((Jwt) authentication.getPrincipal()).getSubject();
    }
}