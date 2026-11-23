package DAO;


import Helpers.EncryptadorMD5;
import Model.Cliente;
import Model.Endereco;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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


    // Trabalho parte 2 — novos casos de teste


    @Nested
    @DisplayName("login() com o EncryptadorMD5 isolado")
    class LoginIsolado {

        @Mock
        private EncryptadorMD5 md5;

        private DaoCliente daoIsolado;

        @BeforeEach
        void prepararDaoIsolado() {
            daoIsolado = new DaoCliente(conexao, enderecoDao, md5);
        }

        @Test
        @DisplayName("Compara o hash calculado pelo encryptador com o hash gravado no banco")
        void deveUsarHashDoEncryptadorInjetado() throws Exception {
            when(md5.encryptar("segredo")).thenReturn("HASH-X");
            bancoRespondeClienteDoLogin("bruna", "HASH-X", 1);

            assertTrue(daoIsolado.login(tentativaDeLogin("bruna", "segredo")));
            verify(md5, atLeastOnce()).encryptar("segredo");
        }

        @Test
        @DisplayName("Hash diferente do gravado nega o acesso")
        void deveNegarQuandoHashDiferente() throws Exception {
            when(md5.encryptar("segredo")).thenReturn("HASH-X");
            bancoRespondeClienteDoLogin("bruna", "HASH-Y", 1);

            assertFalse(daoIsolado.login(tentativaDeLogin("bruna", "segredo")));
        }

        @ParameterizedTest(name = "fg_ativo = {0} nega o acesso")
        @ValueSource(ints = {0, 2, -1})
        @DisplayName("Somente fg_ativo igual a 1 libera o acesso (valores limite)")
        void deveExigirFgAtivoIgualAUm(int fgAtivo) throws Exception {
            when(md5.encryptar("segredo")).thenReturn("HASH-X");
            bancoRespondeClienteDoLogin("bruna", "HASH-X", fgAtivo);

            assertFalse(daoIsolado.login(tentativaDeLogin("bruna", "segredo")));
        }

        @Test
        @DisplayName("Consulta parametrizada: o usuário não é concatenado no SQL do login")
        void deveUsarConsultaParametrizadaNoLogin() throws Exception {
            when(md5.encryptar(anyString())).thenReturn("HASH-X");
            bancoRespondeClienteDoLogin("o'brien", "HASH-X", 1);

            daoIsolado.login(tentativaDeLogin("o'brien", "segredo"));

            verify(conexao).prepareStatement("SELECT usuario, senha, fg_ativo FROM tb_clientes WHERE usuario = ?");
            verify(comando).setString(1, "o'brien");
        }

        @Test
        @DisplayName("Fecha ResultSet e PreparedStatement depois da consulta")
        void deveFecharRecursosNoLogin() throws Exception {
            when(md5.encryptar(anyString())).thenReturn("HASH-X");
            bancoRespondeClienteDoLogin("bruna", "HASH-X", 1);

            daoIsolado.login(tentativaDeLogin("bruna", "segredo"));

            InOrder ordem = inOrder(resultado, comando);
            ordem.verify(resultado).close();
            ordem.verify(comando).close();
        }

        @Test
        @DisplayName("Falha ao executar a consulta (executeQuery) também nega o acesso")
        void deveNegarQuandoExecuteQueryFalha() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenThrow(new SQLException("timeout"));

            assertFalse(daoIsolado.login(tentativaDeLogin("bruna", "segredo")));
            verifyNoInteractions(md5);
        }

        @Test
        @DisplayName("Caracterização: usuário duplicado no banco — vale a última linha lida (achado do CT-03)")
        void caracterizacaoUsuarioDuplicadoValeUltimaLinha() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenReturn(resultado);
            when(resultado.next()).thenReturn(true, true, false, true, true, false);
            when(resultado.getString("usuario")).thenReturn("bruna");
            when(resultado.getString("senha")).thenReturn("HASH-PRIMEIRO", "HASH-ULTIMO", "HASH-PRIMEIRO", "HASH-ULTIMO");
            when(resultado.getInt("fg_ativo")).thenReturn(1);
            when(md5.encryptar("senhaDoPrimeiro")).thenReturn("HASH-PRIMEIRO");
            when(md5.encryptar("senhaDoUltimo")).thenReturn("HASH-ULTIMO");

            // O primeiro cliente cadastrado perde o acesso; só a senha do último funciona
            assertFalse(daoIsolado.login(tentativaDeLogin("bruna", "senhaDoPrimeiro")));
            assertTrue(daoIsolado.login(tentativaDeLogin("bruna", "senhaDoUltimo")));
        }

        @Test
        @DisplayName("Caracterização: se o encryptador devolver null, login lança NullPointerException")
        void caracterizacaoEncryptadorDevolvendoNull() throws Exception {
            when(md5.encryptar(anyString())).thenReturn(null);
            bancoRespondeClienteDoLogin("bruna", "HASH-X", 1);

            // O EncryptadorMD5 devolve null quando falha; o DAO não trata esse retorno
            assertThrows(NullPointerException.class, () -> daoIsolado.login(tentativaDeLogin("bruna", "segredo")));
        }
    }

    @Nested
    @DisplayName("salvar() — parâmetros e falhas")
    class SalvarEntrega2 {

        @Test
        @DisplayName("Vincula os sete parâmetros do INSERT na ordem das colunas")
        void deveVincularTodosOsParametrosNaOrdem() throws Exception {
            Cliente cliente = clienteParaCadastro();
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(enderecoDao.validaEndereco(cliente.getEndereco())).thenReturn(3);

            dao.salvar(cliente);

            InOrder ordem = inOrder(comando);
            ordem.verify(comando).setString(1, "Ana");
            ordem.verify(comando).setString(2, "D'Ávila");
            ordem.verify(comando).setString(3, "41999990000");
            ordem.verify(comando).setString(4, "ana.davila");
            ordem.verify(comando).setString(5, "123456");
            ordem.verify(comando).setInt(6, 1);
            ordem.verify(comando).setInt(7, 3);
            ordem.verify(comando).execute();
            ordem.verify(comando).close();
        }

        @Test
        @DisplayName("O SQL do INSERT tem as colunas na ordem esperada")
        void deveMontarInsertComColunasEsperadas() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(enderecoDao.validaEndereco(any())).thenReturn(3);

            dao.salvar(clienteParaCadastro());

            verify(conexao).prepareStatement("INSERT INTO tb_clientes(nome, sobrenome, telefone, usuario, senha, fg_ativo, id_endereco) "
                    + "VALUES(?,?,?,?, MD5(?),?,?)");
        }

        @Test
        @DisplayName("Cliente inativo (fg_ativo = 0) é gravado com a flag recebida")
        void deveRepassarFgAtivoRecebido() throws Exception {
            Cliente cliente = clienteParaCadastro();
            cliente.setFg_ativo(0);
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(enderecoDao.validaEndereco(any())).thenReturn(3);

            dao.salvar(cliente);

            verify(comando).setInt(6, 0);
        }

        @Test
        @DisplayName("Endereço novo: grava o endereço e consulta o id duas vezes (antes e depois)")
        void deveConsultarEnderecoAntesEDepoisDeGravar() throws Exception {
            Cliente cliente = clienteParaCadastro();
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(enderecoDao.validaEndereco(cliente.getEndereco())).thenReturn(0, 12);

            dao.salvar(cliente);

            InOrder ordem = inOrder(enderecoDao, comando);
            ordem.verify(enderecoDao).validaEndereco(cliente.getEndereco());
            ordem.verify(enderecoDao).salvar(cliente.getEndereco());
            ordem.verify(enderecoDao).validaEndereco(cliente.getEndereco());
            ordem.verify(comando).execute();
        }

        @Test
        @DisplayName("Caracterização: endereço existente é consultado duas vezes (consulta redundante)")
        void caracterizacaoConsultaRedundanteDoEndereco() throws Exception {
            Cliente cliente = clienteParaCadastro();
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(enderecoDao.validaEndereco(cliente.getEndereco())).thenReturn(7);

            dao.salvar(cliente);

            verify(enderecoDao, times(2)).validaEndereco(cliente.getEndereco());
        }

        @Test
        @DisplayName("Falha ao gravar o endereço impede a gravação do cliente")
        void naoDeveGravarClienteSeEnderecoFalhar() throws Exception {
            Cliente cliente = clienteParaCadastro();
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(enderecoDao.validaEndereco(any())).thenReturn(0);
            doThrow(new RuntimeException("falha no INSERT do endereço")).when(enderecoDao).salvar(any());

            RuntimeException erro = assertThrows(RuntimeException.class, () -> dao.salvar(cliente));

            assertEquals("falha no INSERT do endereço", erro.getCause().getMessage());
            verify(comando, never()).execute();
        }

        @Test
        @DisplayName("Falha no execute() do INSERT vira RuntimeException com a SQLException como causa")
        void deveEmbrulharSQLExceptionDoExecute() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(enderecoDao.validaEndereco(any())).thenReturn(7);
            when(comando.execute()).thenThrow(new SQLException("duplicate key"));

            RuntimeException erro = assertThrows(RuntimeException.class, () -> dao.salvar(clienteParaCadastro()));

            assertTrue(erro.getCause() instanceof SQLException);
            assertEquals("duplicate key", erro.getCause().getMessage());
        }
    }

    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("Mapeia todas as linhas na ordem devolvida pelo banco")
        void deveMapearTodasAsLinhas() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenReturn(resultado);
            when(resultado.next()).thenReturn(true, true, false);
            when(resultado.getInt("id_cliente")).thenReturn(1, 2);
            when(resultado.getString("nome")).thenReturn("Ana", "Bruno");
            when(resultado.getString("sobrenome")).thenReturn("Souza", "Lima");
            when(resultado.getString("telefone")).thenReturn("111", "222");
            when(resultado.getString("usuario")).thenReturn("ana", "bruno");
            when(resultado.getString("senha")).thenReturn("h1", "h2");

            List<Cliente> clientes = dao.listarTodos();

            assertEquals(2, clientes.size());
            assertEquals(1, clientes.get(0).getId_cliente());
            assertEquals("Ana", clientes.get(0).getNome());
            assertEquals("Souza", clientes.get(0).getSobrenome());
            assertEquals("111", clientes.get(0).getTelefone());
            assertEquals("ana", clientes.get(0).getUsuario());
            assertEquals("h1", clientes.get(0).getSenha());
            assertEquals(2, clientes.get(1).getId_cliente());
            assertEquals("Bruno", clientes.get(1).getNome());
            assertEquals("bruno", clientes.get(1).getUsuario());
            assertTrue(clientes.stream().allMatch(c -> c.getFg_ativo() == 1));
        }

        @Test
        @DisplayName("Sem clientes ativos devolve lista vazia, não null")
        void deveDevolverListaVazia() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenReturn(resultado);
            when(resultado.next()).thenReturn(false);

            List<Cliente> clientes = dao.listarTodos();

            assertNotNull(clientes);
            assertTrue(clientes.isEmpty());
        }

        @Test
        @DisplayName("Filtra apenas ativos, ordena por id e fecha os recursos")
        void deveFiltrarAtivosOrdenarEFechar() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenReturn(resultado);
            when(resultado.next()).thenReturn(false);

            dao.listarTodos();

            verify(conexao).prepareStatement("SELECT * FROM tb_clientes WHERE fg_Ativo='1' ORDER BY id_cliente");
            verify(resultado).close();
            verify(comando).close();
        }

        @Test
        @DisplayName("Falha no executeQuery vira RuntimeException")
        void deveRepassarFalhaDoExecuteQuery() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenThrow(new SQLException("relação não existe"));

            RuntimeException erro = assertThrows(RuntimeException.class, () -> dao.listarTodos());
            assertTrue(erro.getCause() instanceof SQLException);
        }
    }

    @Nested
    @DisplayName("pesquisaPorUsuario() e pesquisaPorID() — casos adicionais")
    class ConsultasEntrega2 {

        private Cliente porUsuario(String usuario) {
            Cliente c = new Cliente();
            c.setUsuario(usuario);
            return c;
        }

        @Test
        @DisplayName("Usuário não encontrado devolve Cliente vazio (id 0 e campos nulos)")
        void deveDevolverClienteVazioQuandoUsuarioNaoExiste() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenReturn(resultado);
            when(resultado.next()).thenReturn(false);

            Cliente encontrado = dao.pesquisaPorUsuario(porUsuario("ninguem"));

            assertNotNull(encontrado);
            assertEquals(0, encontrado.getId_cliente());
            assertNull(encontrado.getUsuario());
            assertEquals(0, encontrado.getFg_ativo());
            verify(resultado).close();
            verify(comando).close();
        }

        @Test
        @DisplayName("pesquisaPorUsuario também carrega telefone e hash da senha (mata mutantes sobreviventes do PIT)")
        void deveCarregarTelefoneESenhaNaPesquisaPorUsuario() throws Exception {
            bancoRespondeUmaLinha();
            // lenient: o DAO lê outras colunas com getString, que devem devolver null
            lenient().when(resultado.getString(anyString())).thenReturn(null);
            when(resultado.getString("telefone")).thenReturn("41977776666");
            when(resultado.getString("senha")).thenReturn(MD5_DE_123456);

            Cliente encontrado = dao.pesquisaPorUsuario(porUsuario("ana"));

            assertEquals("41977776666", encontrado.getTelefone());
            assertEquals(MD5_DE_123456, encontrado.getSenha());
        }

        @Test
        @DisplayName("Falha de SQL em pesquisaPorUsuario vira RuntimeException")
        void deveRepassarFalhaNaPesquisaPorUsuario() throws Exception {
            when(conexao.prepareStatement(anyString())).thenThrow(new SQLException("banco fora do ar"));

            assertThrows(RuntimeException.class, () -> dao.pesquisaPorUsuario(porUsuario("ana")));
        }

        @Test
        @DisplayName("Caracterização: pesquisaPorUsuario marca o cliente como ativo sem consultar fg_ativo")
        void caracterizacaoPesquisaPorUsuarioIgnoraFgAtivo() throws Exception {
            bancoRespondeUmaLinha();

            Cliente encontrado = dao.pesquisaPorUsuario(porUsuario("inativo"));

            // A consulta não filtra fg_ativo e o DAO fixa 1: um cliente desativado aparece como ativo
            assertEquals(1, encontrado.getFg_ativo());
            verify(resultado, never()).getInt("fg_ativo");
        }

        @Test
        @DisplayName("Caracterização: pesquisaPorUsuario com usuário duplicado devolve a última linha")
        void caracterizacaoPesquisaPorUsuarioDuplicado() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenReturn(resultado);
            when(resultado.next()).thenReturn(true, true, false);
            when(resultado.getInt("id_cliente")).thenReturn(10, 20);

            assertEquals(20, dao.pesquisaPorUsuario(porUsuario("bruna")).getId_cliente());
        }

        @Test
        @DisplayName("pesquisaPorID não carrega usuário nem senha")
        void pesquisaPorIdNaoExpoeCredenciais() throws Exception {
            bancoRespondeUmaLinha();
            when(resultado.getInt("id_cliente")).thenReturn(42);

            Cliente encontrado = dao.pesquisaPorID("42");

            assertNull(encontrado.getUsuario());
            assertNull(encontrado.getSenha());
            verify(resultado, never()).getString("senha");
            verify(resultado).close();
            verify(comando).close();
        }

        @Test
        @DisplayName("Caracterização do DEF-04: pesquisaPorID concatena o id no SQL")
        void caracterizacaoPesquisaPorIdConcatenaSql() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenReturn(resultado);
            when(resultado.next()).thenReturn(false);

            dao.pesquisaPorID("1' OR '1'='1");

            verify(conexao).prepareStatement("SELECT * FROM tb_clientes WHERE id_cliente='1' OR '1'='1'");
            verify(comando, never()).setString(anyInt(), anyString());
        }

        @Test
        @Tag("defeito-conhecido")
        @DisplayName("DEF-04: pesquisaPorUsuario deve enviar o usuário como parâmetro, sem concatenar")
        void pesquisaPorUsuarioDeveSerParametrizada() throws Exception {
            when(conexao.prepareStatement(anyString())).thenReturn(comando);
            when(comando.executeQuery()).thenReturn(resultado);
            when(resultado.next()).thenReturn(false);

            dao.pesquisaPorUsuario(porUsuario("o'brien"));

            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            verify(conexao).prepareStatement(sql.capture());
            assertFalse(sql.getValue().contains("o'brien"), "o valor não pode estar dentro do SQL");
            verify(comando).setString(1, "o'brien");
        }
    }

}
