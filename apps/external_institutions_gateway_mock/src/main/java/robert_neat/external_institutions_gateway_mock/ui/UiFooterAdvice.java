package robert_neat.external_institutions_gateway_mock.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Injects the server port into every operator-UI view, used by fragments/layout.html's footer fragment. */
@ControllerAdvice(basePackageClasses = UiController.class)
public class UiFooterAdvice {

    @Value("${server.port:10523}")
    private String serverPort;

    @ModelAttribute("serverPort")
    public String serverPort() {
        return serverPort;
    }
}
