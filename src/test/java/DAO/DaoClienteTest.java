package DAO;

import Model.Cliente;
import Model.Endereco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DaoClienteTest {

    // Hashes conhecidos, para não depender do EncryptadorMD5 ao montar o resultado esperado
    private static final String MD5_DE_123456 = "e10adc3949ba59abbe56e057f20f883e";

    @Mock
    private Connection conexao;

    @Mock
    private PreparedStatement comando;

    @Mock
    private ResultSet resultado;

    @Mock
    private DaoEndereco enderecoDao;

    private DaoCliente dao;

    @BeforeEach
    void setUp() {
        dao = new DaoCliente(conexao, enderecoDao);
    }

    private void bancoRespondeUmaLinha() throws SQLException {
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        when(comando.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(true, false);
    }

    private void bancoRespondeClienteDoLogin(String usuario, String hashSenha, int ativo) throws SQLException {
        bancoRespondeUmaLinha();
        when(resultado.getString("usuario")).thenReturn(usuario);
        when(resultado.getString("senha")).thenReturn(hashSenha);
        when(resultado.getInt("fg_ativo")).thenReturn(ativo);
    }

    private Cliente tentativaDeLogin(String usuario, String senha) {
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setSenha(senha);
        return cliente;
    }

    // ---------- login ----------

    @Test
    void testeLoginComSucesso() throws Exception {
        bancoRespondeClienteDoLogin("admin", MD5_DE_123456, 1);

        boolean logou = dao.login(tentativaDeLogin("admin", "123456"));

        assertTrue(logou, "O login deveria ser aprovado com usuário e senha corretos");
        verify(comando).setString(1, "admin");
    }

    @Test
    void testeLoginSenhaIncorreta() throws Exception {
        bancoRespondeClienteDoLogin("admin", MD5_DE_123456, 1);

        boolean logou = dao.login(tentativaDeLogin("admin", "senha_errada"));

        assertFalse(logou, "O login não deve ser permitido com senha errada");
    }

    @Test
    @DisplayName("Cliente inativo não entra, mesmo com a senha correta")
    void deveRecusarClienteInativo() throws Exception {
        bancoRespondeClienteDoLogin("carlos.silva", MD5_DE_123456, 0);

        assertFalse(dao.login(tentativaDeLogin("carlos.silva", "123456")));
    }

    @Test
    @DisplayName("Usuário inexistente não entra")
    void deveRecusarUsuarioInexistente() throws Exception {
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        when(comando.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(false);

        assertFalse(dao.login(tentativaDeLogin("ninguem", "123456")));
    }

    @Test
    @DisplayName("A comparação da senha diferencia maiúsculas de minúsculas")
    void deveDiferenciarMaiusculasNaSenha() throws Exception {
        bancoRespondeClienteDoLogin("admin", "0cc175b9c0f1b6a831c399e269772661", 1); // MD5 de "a"

        assertFalse(dao.login(tentativaDeLogin("admin", "A")));
    }

    @Test
    @DisplayName("Senha em texto puro gravada no banco não autentica")
    void deveRecusarQuandoBancoGuardaSenhaSemHash() throws Exception {
        bancoRespondeClienteDoLogin("admin", "123456", 1);

        assertFalse(dao.login(tentativaDeLogin("admin", "123456")));
    }

    @Test
    @DisplayName("Falha de SQL no login é tratada como acesso negado")
    void deveNegarLoginQuandoBancoFalha() throws Exception {
        when(conexao.prepareStatement(anyString())).thenThrow(new SQLException("banco fora do ar"));

        assertFalse(dao.login(tentativaDeLogin("admin", "123456")));
    }

    @Test
    @DisplayName("Login sem senha (null) lança NullPointerException")
    void caracterizacaoLoginComSenhaNula() throws Exception {
        bancoRespondeClienteDoLogin("admin", MD5_DE_123456, 1);

        assertThrows(NullPointerException.class, () -> dao.login(tentativaDeLogin("admin", null)));
    }

    // ---------- salvar ----------

    private Cliente clienteParaCadastro() {
        Endereco endereco = new Endereco();
        endereco.setRua("Rua XV de Novembro");
        endereco.setNumero(100);
        endereco.setBairro("Centro");
        endereco.setCidade("Curitiba");
        endereco.setEstado("PR");

        Cliente cliente = new Cliente();
        cliente.setNome("Ana");
        cliente.setSobrenome("D'Ávila");
        cliente.setTelefone("41999990000");
        cliente.setUsuario("ana.davila");
        cliente.setSenha("123456");
        cliente.setFg_ativo(1);
        cliente.setEndereco(endereco);
        return cliente;
    }

    @Test
    @DisplayName("Cadastro com endereço novo grava o endereço antes e usa o id gerado")
    void deveGravarEnderecoNovoAntesDoCliente() throws Exception {
        Cliente cliente = clienteParaCadastro();
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        when(enderecoDao.validaEndereco(cliente.getEndereco())).thenReturn(0, 12);

        dao.salvar(cliente);

        verify(enderecoDao).salvar(cliente.getEndereco());
        verify(comando).setInt(7, 12);
        verify(comando).execute();
    }

    @Test
    @DisplayName("Cadastro com endereço já existente reaproveita o id e não duplica o endereço")
    void deveReaproveitarEnderecoExistente() throws Exception {
        Cliente cliente = clienteParaCadastro();
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        when(enderecoDao.validaEndereco(cliente.getEndereco())).thenReturn(7);

        dao.salvar(cliente);

        verify(enderecoDao, never()).salvar(any());
        verify(comando).setInt(7, 7);
    }

    @Test
    @DisplayName("A senha vai em texto para o banco, que aplica MD5 no próprio INSERT")
    void deveDelegarHashDaSenhaAoBanco() throws Exception {
        Cliente cliente = clienteParaCadastro();
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        when(enderecoDao.validaEndereco(any())).thenReturn(7);

        dao.salvar(cliente);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexao).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("MD5(?)"));
        verify(comando).setString(2, "D'Ávila");
        verify(comando).setString(5, "123456");
        verify(comando).setInt(6, 1);
    }

    @Test
    @DisplayName("Falha ao gravar o cliente vira RuntimeException")
    void deveRepassarFalhaAoSalvar() throws Exception {
        when(conexao.prepareStatement(anyString())).thenThrow(new SQLException("violação de NOT NULL"));

        assertThrows(RuntimeException.class, () -> dao.salvar(clienteParaCadastro()));
    }

    // ---------- consultas ----------

    @Test
    void testePesquisaPorUsuarioComSucesso() throws Exception {
        Cliente parametroPesquisa = new Cliente();
        parametroPesquisa.setUsuario("carlos.silva");

        bancoRespondeUmaLinha();
        when(resultado.getInt("id_cliente")).thenReturn(15);
        when(resultado.getString("nome")).thenReturn("Carlos");
        when(resultado.getString("sobrenome")).thenReturn("Silva");
        when(resultado.getString("telefone")).thenReturn("988887777");
        when(resultado.getString("usuario")).thenReturn("carlos.silva");
        when(resultado.getString("senha")).thenReturn("hash123");

        Cliente encontrado = dao.pesquisaPorUsuario(parametroPesquisa);

        assertNotNull(encontrado, "O cliente retornado não deve ser nulo");
        assertEquals(15, encontrado.getId_cliente());
        assertEquals("Carlos", encontrado.getNome());
        assertEquals("Silva", encontrado.getSobrenome());
        assertEquals("carlos.silva", encontrado.getUsuario());
        assertEquals(1, encontrado.getFg_ativo());
    }

    @Test
    @DisplayName("pesquisaPorUsuario concatena o usuário no SQL: apóstrofo quebra o comando")
    void caracterizacaoPesquisaPorUsuarioConcatenaSql() throws Exception {
        Cliente parametroPesquisa = new Cliente();
        parametroPesquisa.setUsuario("o'brien");
        bancoRespondeUmaLinha();

        dao.pesquisaPorUsuario(parametroPesquisa);

        // No PostgreSQL esse comando é inválido: o login de um usuário com apóstrofo quebra (DEF-04)
        verify(conexao).prepareStatement("SELECT * FROM tb_clientes WHERE usuario='o'brien'");
    }

    @Test
    void testePesquisaPorIDComSucesso() throws Exception {
        bancoRespondeUmaLinha();
        when(resultado.getInt("id_cliente")).thenReturn(42);
        when(resultado.getString("nome")).thenReturn("Ana");
        when(resultado.getString("sobrenome")).thenReturn("Souza");
        when(resultado.getString("telefone")).thenReturn("911112222");

        Cliente encontrado = dao.pesquisaPorID("42");

        assertNotNull(encontrado);
        assertEquals(42, encontrado.getId_cliente());
        assertEquals("Ana", encontrado.getNome());
        assertEquals("Souza", encontrado.getSobrenome());
        assertEquals("911112222", encontrado.getTelefone());
        assertEquals(1, encontrado.getFg_ativo());
    }

    @Test
    void testePesquisaPorIDNaoEncontrado() throws Exception {
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        when(comando.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(false);

        Cliente encontrado = dao.pesquisaPorID("999");

        // Sem linhas, o DAO devolve um Cliente vazio em vez de null
        assertNotNull(encontrado);
        assertEquals(0, encontrado.getId_cliente());
        assertNull(encontrado.getNome());
    }

    @Test
    @DisplayName("Falha de SQL nas consultas vira RuntimeException")
    void deveRepassarFalhaNasConsultas() throws Exception {
        when(conexao.prepareStatement(anyString())).thenThrow(new SQLException("banco fora do ar"));

        assertThrows(RuntimeException.class, () -> dao.pesquisaPorID("1"));
        assertThrows(RuntimeException.class, () -> dao.listarTodos());
    }
}
