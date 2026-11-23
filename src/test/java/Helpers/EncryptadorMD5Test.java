package Helpers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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

    //  Trabalho parte 2 - novos casos de teste.

    @ParameterizedTest(name = "\"{0}\" gera 32 dígitos hexadecimais minúsculos")
    @ValueSource(strings = {"a", "0", " ", "senha com espaços", "UMA SENHA LONGA COM MAIS DE SESSENTA E QUATRO CARACTERES PARA PASSAR DE UM BLOCO"})
    @DisplayName("O hash sempre tem 32 caracteres hexadecimais minúsculos")
    void deveGerarSempreHexMinusculoDe32(String texto) {
        assertTrue(encryptador.encryptar(texto).matches("[0-9a-f]{32}"));
    }

    @Test
    @DisplayName("Mesmo texto gera sempre o mesmo hash, inclusive em instâncias diferentes")
    void deveSerDeterministico() {
        assertEquals(encryptador.encryptar("123456"), new EncryptadorMD5().encryptar("123456"));
        assertEquals(encryptador.encryptar("123456"), encryptador.encryptar("123456"));
    }

    @Test
    @DisplayName("A senha diferencia maiúsculas de minúsculas e espaços")
    void deveDiferenciarMaiusculasEEspacos() {
        assertEquals("28a0cd7cfee0b846ea882e7d1fe760bb", encryptador.encryptar("Senha Forte"));
        assertEquals("eb48536c68dca4173c949b5ca5f3bb5c", encryptador.encryptar("senha forte"));
        assertNotEquals(encryptador.encryptar("123456"), encryptador.encryptar("123456 "));
    }

    @Test
    @DisplayName("Com charset padrão UTF-8, senha acentuada gera o mesmo hash que o MD5() do PostgreSQL")
    void deveCoincidirComMd5DoBancoParaAcentos() {
        // O cadastro aplica MD5() no banco (bytes UTF-8); o login usa String.getBytes() com o charset da JVM.
        // Hash de referência calculado fora do Java: printf 'ção' | md5sum
        assumeTrue(Charset.defaultCharset().equals(StandardCharsets.UTF_8), "requer charset padrão UTF-8");

        assertEquals("28452896dc8896b5ec91899a8f0c4143", encryptador.encryptar("ção"));
    }
}
