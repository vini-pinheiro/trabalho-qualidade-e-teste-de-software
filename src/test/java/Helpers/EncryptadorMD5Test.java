package Helpers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Os valores esperados vêm da suíte de testes da RFC 1321 e do banco.sql,
 * e não do próprio EncryptadorMD5.
 */
class EncryptadorMD5Test {

    private final EncryptadorMD5 encryptador = new EncryptadorMD5();

    @ParameterizedTest(name = "MD5(\"{0}\") = {1}")
    @CsvSource({
        "'',               d41d8cd98f00b204e9800998ecf8427e",
        "abc,              900150983cd24fb0d6963f7d28e17f72",
        "message digest,   f96b697d7cb7938d525a2f31aaf161d0",
        "admin,            21232f297a57a5a743894a0e4a801fc3",
        "123456,           e10adc3949ba59abbe56e057f20f883e"
    })
    @DisplayName("Gera o mesmo hash dos vetores de referência")
    void deveGerarHashDosVetoresDeReferencia(String texto, String hashEsperado) {
        assertEquals(hashEsperado, encryptador.encryptar(texto));
    }

    @Test
    @DisplayName("Hash iniciado por zero mantém os 32 caracteres")
    void deveManterZeroAEsquerda() {
        // MD5("a") começa com 0; sem o preenchimento o BigInteger devolveria 31 caracteres
        String hash = encryptador.encryptar("a");

        assertEquals("0cc175b9c0f1b6a831c399e269772661", hash);
        assertEquals(32, hash.length());
    }

    @Test
    @DisplayName("Texto nulo devolve null em vez de lançar exceção")
    void caracterizacaoTextoNuloDevolveNull() {
        assertNull(encryptador.encryptar(null));
    }
}
