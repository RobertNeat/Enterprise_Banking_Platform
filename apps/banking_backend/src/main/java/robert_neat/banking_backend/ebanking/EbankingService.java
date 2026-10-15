package robert_neat.banking_backend.ebanking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import robert_neat.banking_backend.common.BankTimeZone;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EbankingService {

    private static final DateTimeFormatter LAST_LOGIN_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(BankTimeZone.ZONE);

    private final EbankingUserRepository ebankingUsers;

    public EbankingService(EbankingUserRepository ebankingUsers) {
        this.ebankingUsers = ebankingUsers;
    }

    public List<EbankingUserDto> findAll() {
        return ebankingUsers.findAll().stream().map(this::toDto).toList();
    }

    public EbankingUserDto toDto(EbankingUser u) {
        return new EbankingUserDto(
                u.getId(),
                u.getUsername(),
                u.getCustomer().getFullName(),
                u.getChannel(),
                u.getAccessStatus(),
                u.getDailyLimit(),
                u.getLastLogin() == null ? null : LAST_LOGIN_FORMAT.format(u.getLastLogin()),
                splitPermissions(u.getPermissions()));
    }

    private static List<String> splitPermissions(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
