package robert_neat.banking_backend.collections;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/collections")
public class CollectionsController {

    private final CollectionsService collectionsService;

    public CollectionsController(CollectionsService collectionsService) {
        this.collectionsService = collectionsService;
    }

    @GetMapping
    public List<CollectionsCaseDto> findAll() {
        return collectionsService.findAll();
    }

    @GetMapping("/{id}/actions")
    public List<CollectionsActionDto> findActions(@PathVariable String id) {
        return collectionsService.findActionsByCaseId(id);
    }
}
