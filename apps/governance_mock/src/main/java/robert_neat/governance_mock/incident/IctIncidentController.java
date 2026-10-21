package robert_neat.governance_mock.incident;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ict-incidents")
public class IctIncidentController {

    private final IctIncidentService incidentService;

    public IctIncidentController(IctIncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IctIncident> create(@RequestBody CreateIncidentRequest request) {
        IctIncident created = incidentService.create(request.source(), request.description(), request.severity());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<IctIncident> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Severity severity) {
        return incidentService.list(status, severity);
    }

    @PostMapping("/{id}/resolve")
    public IctIncident resolve(@PathVariable String id) {
        return incidentService.resolve(id);
    }
}
