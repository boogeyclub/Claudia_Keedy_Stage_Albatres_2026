package cm.odigital.serviceconnectmarket.auth.domain;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Builds a password a human can read from an email and type back.
 *
 * Twelve characters drawn from an alphabet without look-alike symbols (no `O`/`0`, `I`/`l`/`1`),
 * always containing at least one lower-case letter, one upper-case letter and one digit. The value
 * is never logged: only the BCrypt hash reaches the database.
 */
@Component
public class SecureTemporaryPasswordGenerator implements TemporaryPasswordGenerator {

    private static final String LOWER_CASE = "abcdefghijkmnpqrstuvwxyz";
    private static final String UPPER_CASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "-!@*";
    private static final String ALL_CHARACTERS = LOWER_CASE + UPPER_CASE + DIGITS + SYMBOLS;
    private static final int PASSWORD_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        List<Character> characters = new ArrayList<>(PASSWORD_LENGTH);
        characters.add(randomFrom(LOWER_CASE));
        characters.add(randomFrom(UPPER_CASE));
        characters.add(randomFrom(DIGITS));
        while (characters.size() < PASSWORD_LENGTH) {
            characters.add(randomFrom(ALL_CHARACTERS));
        }

        Collections.shuffle(characters, secureRandom);
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        characters.forEach(password::append);
        return password.toString();
    }

    private char randomFrom(String characters) {
        return characters.charAt(secureRandom.nextInt(characters.length()));
    }
}
