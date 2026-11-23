
package DAO;

import Model.Ingrediente;
import Model.Lanche;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários de DaoLanche")
class DaoLancheTest {

    @Mock
    private Connection connectionMock;

    @Mock
    private PreparedStatement preparedStatementMock;

    @Mock
    private ResultSet resultSetMock;

    @Mock
    private ResultSet generatedKeysMock;

    private DaoLanche daoLanche;

    @BeforeEach
    void setUp() {
        daoLanche = new DaoLanche(connectionMock);
    }

    /**
     * Cria um lanche com dados válidos para reutilização nos testes.
     */
    private Lanche criarLancheValido() {
        Lanche lanche = new Lanche();
        lanche.setNome("X-Burguer");
        lanche.setDescricao("Pão, hambúrguer e queijo");
        lanche.setValor_venda(15.50);
        return lanche;
    }

    /**
     * Cria um ingrediente com dados válidos para reutilização.
     */
    private Ingrediente criarIngredienteValido() {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId_ingrediente(3);
        ingrediente.setQuantidade(2);
        return ingrediente;
    }

    /**
     * Configura o retorno de uma inserção com chave gerada.
     */
    private void configurarInsercaoComId(int id) throws SQLException {
        when(connectionMock.prepareStatement(
                anyString(),
                eq(Statement.RETURN_GENERATED_KEYS)
        )).thenReturn(preparedStatementMock);

        when(preparedStatementMock.getGeneratedKeys())
                .thenReturn(generatedKeysMock);

        when(generatedKeysMock.next()).thenReturn(true);
        when(generatedKeysMock.getInt(1)).thenReturn(id);
    }

    private void configurarRetornoDoResultSet() throws SQLException {
        ResultSet resultSetMock = mock(ResultSet.class);

        when(preparedStatementMock.getGeneratedKeys())
                .thenReturn(resultSetMock);

        when(resultSetMock.next())
                .thenReturn(true);
    }

    // =====================================================
    // salvar
    // =====================================================

    @Nested
    @DisplayName("Método salvar")
    class SalvarTests {

        @Test
        @DisplayName("Deve salvar um lanche ativo com sucesso")
        void deveSalvarLancheAtivo() throws SQLException {
            Lanche lanche = criarLancheValido();
            configurarRetornoDoResultSet();

            when(connectionMock.prepareStatement(
                    anyString(),
                    eq(Statement.RETURN_GENERATED_KEYS)
            )).thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeUpdate()).thenReturn(1);

            daoLanche.salvar(lanche);

            verify(connectionMock).prepareStatement(
                    contains("INSERT INTO tb_lanches"),
                    eq(Statement.RETURN_GENERATED_KEYS)
            );

            verify(preparedStatementMock).setString(1, "X-Burguer");
            verify(preparedStatementMock).setString(
                    2, "Pão, hambúrguer e queijo"
            );
            verify(preparedStatementMock).setDouble(3, 15.50);
            verify(preparedStatementMock).setInt(4, 1);
            verify(preparedStatementMock).executeUpdate();
            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Deve lançar exceção quando ocorrer falha no banco")
        void deveLancarExcecaoQuandoFalharAoSalvar() throws SQLException {
            Lanche lanche = criarLancheValido();

            when(connectionMock.prepareStatement(
                    anyString(),
                    eq(Statement.RETURN_GENERATED_KEYS)
            )).thenThrow(new SQLException("Database error"));

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.salvar(lanche)
            );

            assertInstanceOf(SQLException.class, exception.getCause());
        }

        @Test
        @DisplayName("Não deve acessar o banco quando o lanche for nulo")
        void naoDeveSalvarLancheNulo() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(null)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com nome nulo")
        void naoDeveSalvarLancheComNomeNulo() {
            Lanche lanche = criarLancheValido();
            lanche.setNome(null);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com nome vazio")
        void naoDeveSalvarLancheComNomeVazio() {
            Lanche lanche = criarLancheValido();
            lanche.setNome("");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com nome contendo apenas espaços")
        void naoDeveSalvarLancheComNomeEmBranco() {
            Lanche lanche = criarLancheValido();
            lanche.setNome("   ");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com descrição nula")
        void naoDeveSalvarLancheComDescricaoNula() {
            Lanche lanche = criarLancheValido();
            lanche.setDescricao(null);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com descrição vazia")
        void naoDeveSalvarLancheComDescricaoVazia() {
            Lanche lanche = criarLancheValido();
            lanche.setDescricao("");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com descrição em branco")
        void naoDeveSalvarLancheComDescricaoEmBranco() {
            Lanche lanche = criarLancheValido();
            lanche.setDescricao("   ");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com valor nulo")
        void naoDeveSalvarLancheComValorNulo() {
            Lanche lanche = criarLancheValido();
            lanche.setValor_venda(null);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com valor NaN")
        void naoDeveSalvarLancheComValorNaN() {
            Lanche lanche = criarLancheValido();
            lanche.setValor_venda(Double.NaN);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com valor infinito")
        void naoDeveSalvarLancheComValorInfinito() {
            Lanche lanche = criarLancheValido();
            lanche.setValor_venda(Double.POSITIVE_INFINITY);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve salvar lanche com valor negativo")
        void naoDeveSalvarLancheComValorNegativo() {
            Lanche lanche = criarLancheValido();
            lanche.setValor_venda(-1.0);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvar(lanche)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Deve aceitar valor zero conforme a validação atual")
        void deveAceitarValorZero() throws SQLException {
            Lanche lanche = criarLancheValido();
            lanche.setValor_venda(0.0);

            configurarRetornoDoResultSet();

            when(connectionMock.prepareStatement(
                    anyString(),
                    eq(Statement.RETURN_GENERATED_KEYS)
            )).thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> daoLanche.salvar(lanche));

            verify(preparedStatementMock).setDouble(3, 0.0);
        }
    }

    // =====================================================
    // salvarCliente
    // =====================================================

    @Nested
    @DisplayName("Método salvarCliente")
    class SalvarClienteTests {

        @Test
        @DisplayName("Deve salvar lanche personalizado com status inativo e retornar ID")
        void deveSalvarLancheClienteERetornarId() throws SQLException {
            Lanche lanche = criarLancheValido();

            configurarInsercaoComId(25);

            int idGerado = daoLanche.salvarCliente(lanche);

            assertEquals(25, idGerado);

            verify(preparedStatementMock).setString(1, "X-Burguer");
            verify(preparedStatementMock).setString(
                    2, "Pão, hambúrguer e queijo"
            );
            verify(preparedStatementMock).setDouble(3, 15.50);
            verify(preparedStatementMock).setInt(4, 0);
            verify(preparedStatementMock).executeUpdate();

            verify(preparedStatementMock).getGeneratedKeys();
            verify(generatedKeysMock).next();
            verify(generatedKeysMock).getInt(1);

            verify(generatedKeysMock).close();
            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Deve lançar exceção quando não houver chave gerada")
        void deveLancarExcecaoQuandoNaoHouverIdGerado()
                throws SQLException {

            Lanche lanche = criarLancheValido();

            when(connectionMock.prepareStatement(
                    anyString(),
                    eq(Statement.RETURN_GENERATED_KEYS)
            )).thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeUpdate()).thenReturn(1);
            when(preparedStatementMock.getGeneratedKeys())
                    .thenReturn(generatedKeysMock);
            when(generatedKeysMock.next()).thenReturn(false);

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.salvarCliente(lanche)
            );

            assertInstanceOf(SQLException.class, exception.getCause());

            verify(generatedKeysMock).close();
            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Deve lançar exceção quando ocorrer falha ao cadastrar lanche cliente")
        void deveLancarExcecaoQuandoFalharAoSalvarCliente()
                throws SQLException {

            Lanche lanche = criarLancheValido();

            when(connectionMock.prepareStatement(
                    anyString(),
                    eq(Statement.RETURN_GENERATED_KEYS)
            )).thenThrow(new SQLException("Insert failed"));

            assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.salvarCliente(lanche)
            );
        }

        @Test
        @DisplayName("Não deve cadastrar lanche cliente com dados inválidos")
        void naoDeveSalvarClienteComDadosInvalidos() {
            Lanche lanche = criarLancheValido();
            lanche.setNome(" ");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.salvarCliente(lanche)
            );

            verifyNoInteractions(connectionMock);
        }
    }

    // =====================================================
    // listarTodos
    // =====================================================

    @Nested
    @DisplayName("Método listarTodos")
    class ListarTodosTests {

        @Test
        @DisplayName("Deve retornar todos os lanches ativos")
        void deveListarTodosOsLanchesAtivos() throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeQuery())
                    .thenReturn(resultSetMock);

            when(resultSetMock.next()).thenReturn(true, true, false);

            when(resultSetMock.getInt("id_lanche"))
                    .thenReturn(1, 2);

            when(resultSetMock.getString("nm_lanche"))
                    .thenReturn("X-Burguer", "X-Salada");

            when(resultSetMock.getString("descricao"))
                    .thenReturn("Tradicional", "Com alface e tomate");

            when(resultSetMock.getDouble("valor_venda"))
                    .thenReturn(15.00, 18.00);

            List<Lanche> resultado = daoLanche.listarTodos();

            assertNotNull(resultado);
            assertEquals(2, resultado.size());

            assertEquals(1, resultado.get(0).getId_lanche());
            assertEquals("X-Burguer", resultado.get(0).getNome());
            assertEquals("Tradicional", resultado.get(0).getDescricao());
            assertEquals(15.00, resultado.get(0).getValor_venda());
            assertEquals(1, resultado.get(0).getFg_ativo());

            assertEquals(2, resultado.get(1).getId_lanche());
            assertEquals("X-Salada", resultado.get(1).getNome());
            assertEquals(18.00, resultado.get(1).getValor_venda());

            verify(connectionMock).prepareStatement(
                    contains("fg_Ativo='1'")
            );

            verify(resultSetMock, times(3)).next();
            verify(resultSetMock).close();
            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver lanches")
        void deveRetornarListaVaziaQuandoNaoExistiremLanches()
                throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeQuery())
                    .thenReturn(resultSetMock);

            when(resultSetMock.next()).thenReturn(false);

            List<Lanche> resultado = daoLanche.listarTodos();

            assertNotNull(resultado);
            assertTrue(resultado.isEmpty());

            verify(resultSetMock).close();
            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Deve lançar exceção quando falhar ao preparar consulta")
        void deveLancarExcecaoQuandoFalharAoPrepararConsulta()
                throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenThrow(new SQLException("Query failed"));

            assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.listarTodos()
            );
        }

        @Test
        @DisplayName("Deve lançar exceção quando falhar ao executar consulta")
        void deveLancarExcecaoQuandoFalharAoExecutarConsulta()
                throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeQuery())
                    .thenThrow(new SQLException("Execution failed"));

            assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.listarTodos()
            );

            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Deve lançar exceção quando falhar ao percorrer resultados")
        void deveLancarExcecaoQuandoFalharAoPercorrerResultados()
                throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeQuery())
                    .thenReturn(resultSetMock);

            when(resultSetMock.next())
                    .thenThrow(new SQLException("ResultSet failed"));

            assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.listarTodos()
            );

            verify(resultSetMock).close();
            verify(preparedStatementMock).close();
        }
    }

    // =====================================================
    // pesquisaPorNome(Lanche)
    // =====================================================

    @Nested
    @DisplayName("Método pesquisaPorNome com objeto Lanche")
    class PesquisaPorLancheTests {

        @Test
        @DisplayName("Deve encontrar lanche utilizando objeto Lanche")
        void deveEncontrarLancheUtilizandoObjeto() throws SQLException {

            Lanche filtro = new Lanche();
            filtro.setNome("X-Egg");

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeQuery())
                    .thenReturn(resultSetMock);

            when(resultSetMock.next()).thenReturn(true, false);
            when(resultSetMock.getInt("id_lanche")).thenReturn(5);
            when(resultSetMock.getString("nm_lanche")).thenReturn("X-Egg");
            when(resultSetMock.getString("descricao"))
                    .thenReturn("Com ovo frito");
            when(resultSetMock.getDouble("valor_venda")).thenReturn(17.00);

            Lanche resultado = daoLanche.pesquisaPorNome(filtro);

            assertNotNull(resultado);
            assertEquals(5, resultado.getId_lanche());
            assertEquals("X-Egg", resultado.getNome());
            assertEquals("Com ovo frito", resultado.getDescricao());
            assertEquals(17.00, resultado.getValor_venda());
            assertEquals(1, resultado.getFg_ativo());

            verify(preparedStatementMock).setString(1, "X-Egg");
            verify(resultSetMock).close();
            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Deve rejeitar objeto Lanche nulo")
        void deveRejeitarObjetoLancheNulo() {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.pesquisaPorNome((Lanche) null)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Deve rejeitar objeto Lanche com nome nulo")
        void deveRejeitarObjetoLancheComNomeNulo() {

            Lanche filtro = new Lanche();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.pesquisaPorNome(filtro)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Deve propagar falha de banco ao pesquisar por objeto")
        void devePropagarFalhaAoPesquisarPorObjeto() throws SQLException {

            Lanche filtro = new Lanche();
            filtro.setNome("X-Egg");

            when(connectionMock.prepareStatement(anyString()))
                    .thenThrow(new SQLException("Query failed"));

            assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.pesquisaPorNome(filtro)
            );
        }
    }

    // =====================================================
    // pesquisaPorNome(String)
    // =====================================================

    @Nested
    @DisplayName("Método pesquisaPorNome com String")
    class PesquisaPorNomeTests {

        @Test
        @DisplayName("Deve encontrar lanche pelo nome")
        void deveEncontrarLanchePeloNome() throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeQuery())
                    .thenReturn(resultSetMock);

            when(resultSetMock.next()).thenReturn(true, false);
            when(resultSetMock.getInt("id_lanche")).thenReturn(10);
            when(resultSetMock.getString("nm_lanche"))
                    .thenReturn("X-Frango");
            when(resultSetMock.getString("descricao"))
                    .thenReturn("Filé de frango desfiado");
            when(resultSetMock.getDouble("valor_venda")).thenReturn(16.50);

            Lanche resultado = daoLanche.pesquisaPorNome("X-Frango");

            assertNotNull(resultado);
            assertEquals(10, resultado.getId_lanche());
            assertEquals("X-Frango", resultado.getNome());
            assertEquals("Filé de frango desfiado", resultado.getDescricao());
            assertEquals(16.50, resultado.getValor_venda());
            assertEquals(1, resultado.getFg_ativo());

            verify(preparedStatementMock).setString(1, "X-Frango");
            verify(resultSetMock).close();
            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Deve retornar objeto sem ID quando o lanche não existir")
        void deveRetornarLancheVazioQuandoNaoEncontrarNome()
                throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeQuery())
                    .thenReturn(resultSetMock);

            when(resultSetMock.next()).thenReturn(false);

            Lanche resultado = daoLanche.pesquisaPorNome("Inexistente");

            assertNotNull(resultado);
            assertEquals(0, resultado.getId_lanche());

            verify(preparedStatementMock).setString(1, "Inexistente");
            verify(resultSetMock).close();
            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Deve rejeitar nome nulo")
        void deveRejeitarNomeNulo() {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.pesquisaPorNome((String) null)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Deve rejeitar nome vazio")
        void deveRejeitarNomeVazio() {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.pesquisaPorNome("")
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Deve rejeitar nome contendo apenas espaços")
        void deveRejeitarNomeEmBranco() {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.pesquisaPorNome("   ")
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Deve utilizar parâmetro SQL para pesquisar o nome")
        void deveUtilizarParametroParaPesquisarNome()
                throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeQuery())
                    .thenReturn(resultSetMock);

            when(resultSetMock.next()).thenReturn(false);

            daoLanche.pesquisaPorNome("X' OR '1'='1");

            verify(connectionMock).prepareStatement(
                    argThat(sql ->
                            sql.contains("nm_lanche=?")
                                    && !sql.contains("X' OR")
                    )
            );

            verify(preparedStatementMock).setString(
                    1, "X' OR '1'='1"
            );
        }

        @Test
        @DisplayName("Deve lançar exceção quando falhar ao executar pesquisa")
        void deveLancarExcecaoQuandoFalharPesquisa()
                throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenThrow(new SQLException("Query failed"));

            assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.pesquisaPorNome("X-Tudo")
            );
        }

        @Test
        @DisplayName("Deve lançar exceção quando falhar ao percorrer resultado")
        void deveLancarExcecaoQuandoFalharAoPercorrerResultado()
                throws SQLException {

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            when(preparedStatementMock.executeQuery())
                    .thenReturn(resultSetMock);

            when(resultSetMock.next())
                    .thenThrow(new SQLException("ResultSet failed"));

            assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.pesquisaPorNome("X-Tudo")
            );

            verify(resultSetMock).close();
            verify(preparedStatementMock).close();
        }
    }

    // =====================================================
    // vincularIngrediente
    // =====================================================

    @Nested
    @DisplayName("Método vincularIngrediente")
    class VincularIngredienteTests {

        @Test
        @DisplayName("Deve vincular ingrediente ao lanche com sucesso")
        void deveVincularIngredienteComSucesso() throws SQLException {
            Lanche lanche = criarLancheValido();
            lanche.setId_lanche(1);

            Ingrediente ingrediente = criarIngredienteValido();

            when(connectionMock.prepareStatement(anyString()))
                    .thenReturn(preparedStatementMock);

            daoLanche.vincularIngrediente(lanche, ingrediente);

            verify(connectionMock).prepareStatement(
                    contains("INSERT INTO tb_ingredientes_lanche")
            );

            verify(preparedStatementMock).setInt(1, 1);
            verify(preparedStatementMock).setInt(2, 3);
            verify(preparedStatementMock).setInt(3, 2);
            verify(preparedStatementMock).execute();
            verify(preparedStatementMock).close();
        }

        @Test
        @DisplayName("Não deve vincular quando o lanche for nulo")
        void naoDeveVincularLancheNulo() {

            Ingrediente ingrediente = criarIngredienteValido();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.vincularIngrediente(null, ingrediente)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve vincular lanche com identificador zero")
        void naoDeveVincularLancheComIdZero() {

            Lanche lanche = criarLancheValido();
            lanche.setId_lanche(0);

            Ingrediente ingrediente = criarIngredienteValido();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.vincularIngrediente(lanche, ingrediente)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve vincular lanche com identificador negativo")
        void naoDeveVincularLancheComIdNegativo() {

            Lanche lanche = criarLancheValido();
            lanche.setId_lanche(-1);

            Ingrediente ingrediente = criarIngredienteValido();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.vincularIngrediente(lanche, ingrediente)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve vincular ingrediente nulo")
        void naoDeveVincularIngredienteNulo() {

            Lanche lanche = criarLancheValido();
            lanche.setId_lanche(1);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.vincularIngrediente(lanche, null)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve vincular ingrediente com identificador zero")
        void naoDeveVincularIngredienteComIdZero() {

            Lanche lanche = criarLancheValido();
            lanche.setId_lanche(1);

            Ingrediente ingrediente = criarIngredienteValido();
            ingrediente.setId_ingrediente(0);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.vincularIngrediente(lanche, ingrediente)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve vincular ingrediente com identificador negativo")
        void naoDeveVincularIngredienteComIdNegativo() {

            Lanche lanche = criarLancheValido();
            lanche.setId_lanche(1);

            Ingrediente ingrediente = criarIngredienteValido();
            ingrediente.setId_ingrediente(-1);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.vincularIngrediente(lanche, ingrediente)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve vincular ingrediente com quantidade zero")
        void naoDeveVincularQuantidadeZero() {

            Lanche lanche = criarLancheValido();
            lanche.setId_lanche(1);

            Ingrediente ingrediente = criarIngredienteValido();
            ingrediente.setQuantidade(0);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.vincularIngrediente(lanche, ingrediente)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Não deve vincular ingrediente com quantidade negativa")
        void naoDeveVincularQuantidadeNegativa() {

            Lanche lanche = criarLancheValido();
            lanche.setId_lanche(1);

            Ingrediente ingrediente = criarIngredienteValido();
            ingrediente.setQuantidade(-2);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> daoLanche.vincularIngrediente(lanche, ingrediente)
            );

            verifyNoInteractions(connectionMock);
        }

        @Test
        @DisplayName("Deve propagar falha do banco ao vincular ingrediente")
        void devePropagarFalhaAoVincularIngrediente()
                throws SQLException {

            Lanche lanche = criarLancheValido();
            lanche.setId_lanche(1);

            Ingrediente ingrediente = criarIngredienteValido();

            when(connectionMock.prepareStatement(anyString()))
                    .thenThrow(new SQLException("Insert failed"));

            assertThrows(
                    RuntimeException.class,
                    () -> daoLanche.vincularIngrediente(lanche, ingrediente)
            );
        }
    }
}
