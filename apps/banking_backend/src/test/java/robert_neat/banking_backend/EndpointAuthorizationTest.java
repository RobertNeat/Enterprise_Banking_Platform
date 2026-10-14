package robert_neat.banking_backend;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Coverage test for the R1 security goal: every registered endpoint except /actuator/health,
 * POST /auth/login and POST /auth/register must reject an unauthenticated request with 401.
 * Walks the real RequestMappingHandlerMapping instead of a hardcoded list so it stays accurate
 * as new controllers are added in later rounds.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:endpoint_auth;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
class EndpointAuthorizationTest {

    private static final Set<String> PERMIT_ALL_PATHS = Set.of("/auth/login", "/auth/register");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @TestFactory
    Stream<DynamicTest> everyProtectedEndpointRejectsRequestsWithoutAToken() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .flatMap(this::expandMappingToRequests)
                .filter(mapped -> !PERMIT_ALL_PATHS.contains(mapped.path()))
                .map(mapped -> DynamicTest.dynamicTest(
                        mapped.method() + " " + mapped.path() + " -> 401 without Authorization",
                        () -> mockMvc.perform(request(mapped.method(), substitutePathVariables(mapped.path())))
                                .andExpect(status().isUnauthorized())));
    }

    private Stream<MappedRequest> expandMappingToRequests(
            java.util.Map.Entry<RequestMappingInfo, HandlerMethod> entry) {
        RequestMappingInfo info = entry.getKey();
        List<String> patterns = info.getPathPatternsCondition() == null
                ? List.of()
                : info.getPathPatternsCondition().getPatterns().stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        // GET is implied when no method is declared (unused here, all controllers declare explicitly).
        List<HttpMethod> httpMethods = methods.isEmpty()
                ? List.of(HttpMethod.GET)
                : methods.stream().map(m -> HttpMethod.valueOf(m.name())).collect(Collectors.toList());
        return patterns.stream()
                .flatMap(pattern -> httpMethods.stream().map(method -> new MappedRequest(method, pattern)));
    }

    /** Replaces {var} placeholders with a dummy value — only the 401 check matters, not routing correctness. */
    private String substitutePathVariables(String pattern) {
        return pattern.replaceAll("\\{[^}]+}", "dummy");
    }

    private record MappedRequest(HttpMethod method, String path) {
    }
}
