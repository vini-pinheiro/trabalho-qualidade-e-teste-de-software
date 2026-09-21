package apoio;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * Corpo de requisição em memória para testar os servlets sem subir o Tomcat.
 */
public final class CorpoRequisicao {

    private CorpoRequisicao() {
    }

    public static ServletInputStream de(String json) {
        final ByteArrayInputStream bytes = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        return new ServletInputStream() {
            @Override
            public int read() {
                return bytes.read();
            }

            @Override
            public boolean isFinished() {
                return bytes.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }

    public static ServletInputStream vazio() {
        return de("");
    }
}
