// ...existing code...
package co.com.extractor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import java.awt.Desktop;
import java.net.URI;
import java.awt.GraphicsEnvironment;

@Component
public class SwaggerBrowserLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SwaggerBrowserLauncher.class);

    private final Environment env;

    public SwaggerBrowserLauncher(Environment env) {
        this.env = env;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Property to control automatic opening (default true)
        boolean open = Boolean.parseBoolean(env.getProperty("app.open-browser", "true"));
        if (!open) {
            log.debug("Auto-open browser is disabled (app.open-browser=false)");
            return;
        }

        try {
            // Get actual port from the running web server context
            ServletWebServerApplicationContext ctx = (ServletWebServerApplicationContext) event.getApplicationContext();
            int port = ctx.getWebServer().getPort();
            String contextPath = env.getProperty("server.servlet.context-path", "");
            if (contextPath == null) contextPath = "";
            String url = "http://localhost:" + port + (contextPath.endsWith("/") || contextPath.isEmpty() ? contextPath : contextPath + "/") + "swagger-ui/index.html";

            // Do not attempt to open headless environments
            if (GraphicsEnvironment.isHeadless()) {
                log.info("Server started at {} but environment is headless — not opening browser.", url);
                return;
            }

            // Open in a background thread so it doesn't block the app lifecycle
            final String finalUrl = url;
            new Thread(() -> {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(finalUrl));
                        log.info("Opened default browser to {}", finalUrl);
                    } else {
                        // Fallback for Windows
                        String os = System.getProperty("os.name").toLowerCase();
                        if (os.contains("win")) {
                            Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", finalUrl});
                            log.info("Opened browser via rundll32 to {}", finalUrl);
                        } else if (os.contains("mac")) {
                            Runtime.getRuntime().exec(new String[]{"open", finalUrl});
                            log.info("Opened browser via open to {}", finalUrl);
                        } else {
                            // Linux/other
                            Runtime.getRuntime().exec(new String[]{"xdg-open", finalUrl});
                            log.info("Opened browser via xdg-open to {}", finalUrl);
                        }
                    }
                } catch (Exception e) {
                    log.warn("No se pudo abrir el navegador automáticamente: {}", e.getMessage());
                }
            }, "swagger-browser-launcher").start();

        } catch (Exception e) {
            log.warn("No se pudo determinar la URL para abrir el navegador: {}", e.getMessage());
        }
    }
}
