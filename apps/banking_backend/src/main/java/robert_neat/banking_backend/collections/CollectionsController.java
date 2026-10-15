package robert_neat.banking_backend.collections;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/collections")
public class CollectionsController {

    private final CollectionsService collectionsService;
    private final CollectionsActionsService collectionsActionsService;

    public CollectionsController(
            CollectionsService collectionsService, CollectionsActionsService collectionsActionsService) {
        this.collectionsService = collectionsService;
        this.collectionsActionsService = collectionsActionsService;
    }

    @GetMapping
    public List<CollectionsCaseDto> findAll() {
        return collectionsService.findAll();
    }

    @GetMapping("/{id}/actions")
    public List<CollectionsActionDto> findActions(@PathVariable String id) {
        return collectionsService.findActionsByCaseId(id);
    }

    /** F3 — logs a reminder; 409 if the case already reached 'legal'. No stage change. */
    @PostMapping("/{id}/remind")
    public CollectionsCaseDto remind(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return collectionsActionsService.remind(id, jwt.getSubject());
    }

    /** F3 — moves the case to stage 'settlement'. */
    @PostMapping("/{id}/propose-settlement")
    public CollectionsCaseDto proposeSettlement(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return collectionsActionsService.proposeSettlement(id, jwt.getSubject());
    }

    /** F3 — moves the case to stage 'external'; 409 if still 'monitoring'/'reminder'. */
    @PostMapping("/{id}/transfer-external")
    public CollectionsCaseDto transferExternal(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return collectionsActionsService.transferExternal(id, jwt.getSubject());
    }
}
