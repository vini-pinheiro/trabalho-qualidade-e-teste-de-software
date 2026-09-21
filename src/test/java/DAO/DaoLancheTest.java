package DAO;

import Model.Ingrediente;
import Model.Lanche;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class DaoLancheTest {

    @Mock
    private Connection connectionMock;

    @Mock
    private PreparedStatement preparedStatementMock;

    @Mock
    private ResultSet resultSetMock;

    private DaoLanche daoLanche;

    @BeforeEach
    void setUp() {
        daoLanche = new DaoLanche(connectionMock);
    }


    // ==========================================
    // Testes: salvar
    // ==========================================
    @Test
    @DisplayName("Deve salvar um lanche com status ativo com sucesso")
    void shouldSaveLancheSuccessfully() throws SQLException {
        Lanche lanche = new Lanche();
        lanche.setNome("X-Burguer");
        lanche.setDescricao("Pão, hambúrguer e queijo");
        lanche.setValor_venda(15.50);

        when(connectionMock.prepareStatement(anyString() ))
                .thenReturn(preparedStatementMock);

        daoLanche.salvar(lanche);

        verify(preparedStatementMock).setString(1, "X-Burguer");
        verify(preparedStatementMock).setString(2, "Pão, hambúrguer e queijo");
        verify(preparedStatementMock).setDouble(3, 15.50);
        verify(preparedStatementMock).setInt(4, 1);
        verify(preparedStatementMock).execute();
        verify(preparedStatementMock).close();
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando falhar ao salvar o lanche")
    void shouldThrowRuntimeExceptionWhenSalvarFails() throws SQLException {
        Lanche lanche = new Lanche();
        lanche.setNome("X-Bacon");

        when(connectionMock.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        assertThrows(RuntimeException.class, () -> daoLanche.salvar(lanche));
    }


    // ==========================================
    // Testes: salvarCliente
    // ==========================================
    @Test
    @DisplayName("Deve salvar um lanche personalizado do cliente com status inativo")
    void shouldSaveClienteLancheSuccessfully() throws SQLException {
        Lanche lanche = new Lanche();
        lanche.setNome("Lanche Customizado");
        lanche.setDescricao("Sem cebola, dobro de queijo");
        lanche.setValor_venda(22.00);

        when(connectionMock.prepareStatement(anyString())).thenReturn(preparedStatementMock);

        daoLanche.salvarCliente(lanche);

        verify(preparedStatementMock).setString(1, "Lanche Customizado");
        verify(preparedStatementMock).setString(2, "Sem cebola, dobro de queijo");
        verify(preparedStatementMock).setDouble(3, 22.00);
        verify(preparedStatementMock).setInt(4, 0); // fg_ativo = 0
        verify(preparedStatementMock).execute();
        verify(preparedStatementMock).close();
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando falhar ao salvar lanche do cliente")
    void shouldThrowRuntimeExceptionWhenSalvarClienteFails() throws SQLException {
        Lanche lanche = new Lanche();

        when(connectionMock.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        assertThrows(RuntimeException.class, () -> daoLanche.salvarCliente(lanche));
    }


    // ==========================================
    // Testes: listarTodos
    // ==========================================
    @Test
    @DisplayName("Deve listar todos os lanches ativos com sucesso")
    void shouldReturnListOfAllActiveLanches() throws SQLException {
        when(connectionMock.prepareStatement(anyString())).thenReturn(preparedStatementMock);
        when(preparedStatementMock.executeQuery()).thenReturn(resultSetMock);

        when(resultSetMock.next()).thenReturn(true, true, false);
        when(resultSetMock.getInt("id_lanche")).thenReturn(1, 2);
        when(resultSetMock.getString("nm_lanche")).thenReturn("X-Burguer", "X-Salada");
        when(resultSetMock.getString("descricao")).thenReturn("Tradicional", "Com alface e tomate");
        when(resultSetMock.getDouble("valor_venda")).thenReturn(15.00, 18.00);

        List<Lanche> resultList = daoLanche.listarTodos();

        assertNotNull(resultList);
        assertEquals(2, resultList.size());

        assertEquals(1, resultList.get(0).getId_lanche());
        assertEquals("X-Burguer", resultList.get(0).getNome());
        assertEquals(15.00, resultList.get(0).getValor_venda());

        assertEquals(2, resultList.get(1).getId_lanche());
        assertEquals("X-Salada", resultList.get(1).getNome());

        verify(resultSetMock).close();
        verify(preparedStatementMock).close();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver lanches ativos")
    void shouldReturnEmptyListWhenNoLanchesFound() throws SQLException {
        when(connectionMock.prepareStatement(anyString())).thenReturn(preparedStatementMock);
        when(preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        when(resultSetMock.next()).thenReturn(false);

        List<Lanche> resultList = daoLanche.listarTodos();

        assertNotNull(resultList);
        assertTrue(resultList.isEmpty());
        verify(resultSetMock).close();
        verify(preparedStatementMock).close();
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando falhar ao listar os lanches")
    void shouldThrowRuntimeExceptionWhenListarTodosFails() throws SQLException {
        when(connectionMock.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));

        assertThrows(RuntimeException.class, () -> daoLanche.listarTodos());
    }

    /** pesquisaPorNome possui uma sobrecarga para realizar busca por String ou Lanche  */
    // ==========================================
    // Testes: pesquisaPorNome(Lanche)
    // ==========================================
    @Test
    @DisplayName("Deve pesquisar e encontrar lanche passando objeto Lanche")
    void shouldFindLancheByLancheObject() throws SQLException {
        Lanche searchFilter = new Lanche();
        searchFilter.setNome("X-Egg");

        when(connectionMock.prepareStatement(contains("X-Egg"))).thenReturn(preparedStatementMock);
        when(preparedStatementMock.executeQuery()).thenReturn(resultSetMock);

        when(resultSetMock.next()).thenReturn(true, false);
        when(resultSetMock.getInt("id_lanche")).thenReturn(5);
        when(resultSetMock.getString("nm_lanche")).thenReturn("X-Egg");
        when(resultSetMock.getString("descricao")).thenReturn("Com ovo frito");
        when(resultSetMock.getDouble("valor_venda")).thenReturn(17.00);

        Lanche result = daoLanche.pesquisaPorNome(searchFilter);

        assertNotNull(result);
        assertEquals(5, result.getId_lanche());
        assertEquals("X-Egg", result.getNome());
        assertEquals("Com ovo frito", result.getDescricao());
        assertEquals(17.00, result.getValor_venda());
        assertEquals(1, result.getFg_ativo());

        verify(resultSetMock).close();
        verify(preparedStatementMock).close();
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando pesquisa por objeto Lanche falhar")
    void shouldThrowRuntimeExceptionWhenPesquisaPorNomeObjectFails() throws SQLException {
        Lanche searchFilter = new Lanche();
        searchFilter.setNome("Inexistente");

        when(connectionMock.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));

        assertThrows(RuntimeException.class, () -> daoLanche.pesquisaPorNome(searchFilter));
    }

    // ==========================================
    // Testes: pesquisaPorNome(String)
    // ==========================================
    @Test
    @DisplayName("Deve pesquisar e encontrar lanche passando o nome por String")
    void shouldFindLancheByStringName() throws SQLException {
        String sandwichName = "X-Frango";

        when(connectionMock.prepareStatement(contains("X-Frango"))).thenReturn(preparedStatementMock);
        when(preparedStatementMock.executeQuery()).thenReturn(resultSetMock);

        when(resultSetMock.next()).thenReturn(true, false);
        when(resultSetMock.getInt("id_lanche")).thenReturn(10);
        when(resultSetMock.getString("nm_lanche")).thenReturn("X-Frango");
        when(resultSetMock.getString("descricao")).thenReturn("Filé de frango desfiado");
        when(resultSetMock.getDouble("valor_venda")).thenReturn(16.50);

        Lanche result = daoLanche.pesquisaPorNome(sandwichName);

        assertNotNull(result);
        assertEquals(10, result.getId_lanche());
        assertEquals("X-Frango", result.getNome());
        assertEquals("Filé de frango desfiado", result.getDescricao());
        assertEquals(16.50, result.getValor_venda());

        verify(resultSetMock).close();
        verify(preparedStatementMock).close();
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando pesquisa por nome em String falhar")
    void shouldThrowRuntimeExceptionWhenPesquisaPorNomeStringFails() throws SQLException {
        when(connectionMock.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));

        assertThrows(RuntimeException.class, () -> daoLanche.pesquisaPorNome("X-Tudo"));
    }


    // ==========================================
    // Testes: vincularIngrediente
    // ==========================================
    @Test
    @DisplayName("Deve vincular ingrediente ao lanche com sucesso")
    void shouldLinkIngredientToLancheSuccessfully() throws SQLException {
        Lanche lanche = new Lanche();
        lanche.setId_lanche(1);

        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId_ingrediente(3);
        ingrediente.setQuantidade(2);

        when(connectionMock.prepareStatement(anyString())).thenReturn(preparedStatementMock);

        daoLanche.vincularIngrediente(lanche, ingrediente);

        verify(preparedStatementMock).setInt(1, 1);
        verify(preparedStatementMock).setInt(2, 3);
        verify(preparedStatementMock).setInt(3, 2);
        verify(preparedStatementMock).execute();
        verify(preparedStatementMock).close();
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando falhar ao vincular ingrediente")
    void shouldThrowRuntimeExceptionWhenVincularIngredienteFails() throws SQLException {
        Lanche lanche = new Lanche();
        lanche.setId_lanche(1);

        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId_ingrediente(3);

        when(connectionMock.prepareStatement(anyString())).thenThrow(new SQLException("Insert failed"));

        assertThrows(RuntimeException.class, () -> daoLanche.vincularIngrediente(lanche, ingrediente));
    }
}