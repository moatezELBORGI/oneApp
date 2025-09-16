package be.delomid.oneapp.mschat.mschat.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Slf4j
@Component
public class JwtWebSocketInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");
            
            if (authorization != null && !authorization.isEmpty()) {
                String token = authorization.get(0);
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
                
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    String userId = jwt.getSubject();
                    
                    accessor.setUser(new JwtPrincipal(userId, jwt));
                    log.debug("WebSocket connection authenticated for user: {}", userId);
                    
                } catch (JwtException e) {
                    log.error("JWT validation failed: {}", e.getMessage());
                    return null; // Reject the connection
                }
            } else {
                log.error("No Authorization header found in WebSocket connection");
                return null; // Reject the connection
            }
        }
        
        return message;
    }

    public static class JwtPrincipal implements Principal {
        private final String name;
        private final Jwt jwt;

        public JwtPrincipal(String name, Jwt jwt) {
            this.name = name;
            this.jwt = jwt;
        }

        @Override
        public String getName() {
            return name;
        }

        public Jwt getJwt() {
            return jwt;
        }
    }
}