package org.apache.coyote.http11;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import nextstep.jwp.exception.UncheckedServletException;
import nextstep.jwp.handler.LoginHandler;
import org.apache.coyote.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Http11Processor implements Runnable, Processor {

    private static final Logger log = LoggerFactory.getLogger(Http11Processor.class);

    private static final String CRLF = "\r\n";
    private static final Handler DEFAULT_HANDLER = new FileHandler();
    private static final Map<String, Handler> PREDEFINED_HANDLERS = Map.of(
            "/", httpRequest -> new HttpResponse("Hello world!", "text/html"),
            "/login", new LoginHandler()
    );

    private final Socket connection;

    public Http11Processor(final Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.info("connect host: {}, port: {}", connection.getInetAddress(), connection.getPort());
        process(connection);
    }

    @Override
    public void process(final Socket connection) {
        try (final var inputStream = connection.getInputStream();
             final var outputStream = connection.getOutputStream()) {
            HttpRequest httpRequest = HttpRequest.from(inputStream);

            HttpResponse httpResponse = handle(httpRequest);
            String responseBody = httpResponse.getBody();
            String contentType = httpResponse.getContentType();

            final var response = String.join(CRLF,
                    "HTTP/1.1 200 OK ",
                    "Content-Type: " + contentType + ";charset=utf-8 ",
                    "Content-Length: " + responseBody.getBytes().length + " ",
                    "",
                    responseBody);

            outputStream.write(response.getBytes());
            outputStream.flush();
        } catch (IOException | UncheckedServletException e) {
            log.error(e.getMessage(), e);
        }
    }

    private HttpResponse handle(final HttpRequest httpRequest) throws IOException {
        if (PREDEFINED_HANDLERS.containsKey(httpRequest.getTarget())) {
            return PREDEFINED_HANDLERS.get(httpRequest.getTarget()).handle(httpRequest);
        }
        return DEFAULT_HANDLER.handle(httpRequest);
    }
}
