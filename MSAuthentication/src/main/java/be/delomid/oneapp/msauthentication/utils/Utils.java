package be.delomid.oneapp.msauthentication.utils;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.Random;

@RequiredArgsConstructor
public class Utils {
    private static final String CHARACTERS = "0123456789";
    private static final int LENGTH = 6;
    public static String generateRandomString() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }

        return sb.toString();
    }
    public static String getTokenFromAuthorization(HttpServletRequest request)
    {
        return request.getHeader("Authorization").substring(7);
    }
}
