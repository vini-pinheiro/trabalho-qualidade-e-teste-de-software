package DAO;

import Model.Ingrediente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DaoIngredienteTest {

    @Mock
    private Connection conexao;
    @Mock
    private PreparedStatement comando;
    @Mock
    private ResultSet resultado;

    private DaoIngrediente dao;

    @BeforeEach
    void preparar() {
        dao = new DaoIngrediente(conexao);
    }

    private Ingrediente cheddar() {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId_ingrediente(3);
        ingrediente.setNome("Queijo Cheddar");
        ingrediente.setDescricao("Fatia de queijo cheddar");
        ingrediente.setQuantidade(150);
        ingrediente.setValor_compra(0.80);
        ingrediente.setValor_venda(2.00);
        ingrediente.setTipo("queijo");
        ingrediente.setFg_ativo(1);
        return ingrediente;
    }

    private void bancoRespondeLinhaDoCheddar() throws SQLException {
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        when(comando.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(true, false);
        when(resultado.getInt("id_ingrediente")).thenReturn(3);
        when(resultado.getString("nm_ingrediente")).thenReturn("Queijo Cheddar");
        when(resultado.getString("descricao")).thenReturn("Fatia de queijo cheddar");
        when(resultado.getInt("quantidade")).thenReturn(150);
        when(resultado.getDouble("valor_compra")).thenReturn(0.80);
        when(resultado.getDouble("valor_venda")).thenReturn(2.00);
        when(resultado.getString("tipo")).thenReturn("queijo");
    }

    private void conferirCheddar(Ingrediente ingrediente) {
        assertEquals(3, ingrediente.getId_ingrediente());
        assertEquals("Queijo Cheddar", ingrediente.getNome());
        assertEquals("Fatia de queijo cheddar", ingrediente.getDescricao());
        assertEquals(150, ingrediente.getQuantidade());
        assertEquals(0.80, ingrediente.getValor_compra(), 0.001);
        assertEquals(2.00, ingrediente.getValor_venda(), 0.001);
        assertEquals("queijo", ingrediente.getTipo());
        assertEquals(1, ingrediente.getFg_ativo());
    }

    @Test
    @DisplayName("salvar envia os sete campos do ingrediente na ordem do INSERT")
    void deveEnviarTodosOsCamposAoSalvar() throws SQLException {
        when(conexao.prepareStatement(anyString())).thenReturn(comando);

        dao.salvar(cheddar());

        verify(comando).setString(1, "Queijo Cheddar");
        verify(comando).setString(2, "Fatia de queijo cheddar");
        verify(comando).setInt(3, 150);
        verify(comando).setDouble(4, 0.80);
        verify(comando).setDouble(5, 2.00);
        verify(comando).setString(6, "queijo");
        verify(comando).setInt(7, 1);
        verify(comando).execute();
        verify(comando).close();
    }

    @Test
    @DisplayName("salvar não valida quantidade nem valores negativos: a decisão fica com o banco")
    void caracterizacaoSalvarRepassaValoresNegativos() throws SQLException {
        Ingrediente invalido = cheddar();
        invalido.setQuantidade(-5);
        invalido.setValor_compra(-1.00);
        invalido.setValor_venda(-2.00);
        when(conexao.prepareStatement(anyString())).thenReturn(comando);

        dao.salvar(invalido);

        verify(comando).setInt(3, -5);
        verify(comando).setDouble(4, -1.00);
        verify(comando).setDouble(5, -2.00);
        verify(comando).execute();
    }

    @Test
    @DisplayName("alterar atualiza pelo id e não mexe no fg_ativo")
    void deveAlterarPeloId() throws SQLException {
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        Ingrediente reposicao = cheddar();
        reposicao.setQuantidade(0);

        dao.alterar(reposicao);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexao).prepareStatement(sql.capture());
        assertTrue(sql.getValue().startsWith("UPDATE tb_ingredientes SET"));
        assertTrue(sql.getValue().endsWith("WHERE id_ingrediente =?"));
        verify(comando).setString(1, "Queijo Cheddar");
        verify(comando).setString(2, "Fatia de queijo cheddar");
        verify(comando).setInt(3, 0);
        verify(comando).setDouble(4, 0.80);
        verify(comando).setDouble(5, 2.00);
        verify(comando).setString(6, "queijo");
        verify(comando).setInt(7, 3);
        verify(comando).execute();
        verify(comando).close();
    }

    @Test
    @DisplayName("remover apaga pelo id do ingrediente")
    void deveRemoverPeloId() throws SQLException {
        when(conexao.prepareStatement(anyString())).thenReturn(comando);

        dao.remover(cheddar());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexao).prepareStatement(sql.capture());
        assertTrue(sql.getValue().startsWith("DELETE FROM tb_ingredientes"));
        verify(comando).setInt(1, 3);
        verify(comando).execute();
        verify(comando).close();
    }

    @Test
    @DisplayName("listarTodos converte cada linha em Ingrediente ativo")
    void deveListarIngredientesAtivos() throws SQLException {
        bancoRespondeLinhaDoCheddar();

        List<Ingrediente> ingredientes = dao.listarTodos();

        assertEquals(1, ingredientes.size());
        conferirCheddar(ingredientes.get(0));
        verify(resultado).close();
        verify(comando).close();
    }

    @Test
    @DisplayName("listarTodosPorLanche filtra pelo id do lanche e traz a quantidade do vínculo")
    void deveListarIngredientesDoLanche() throws SQLException {
        bancoRespondeLinhaDoCheddar();
        when(resultado.getInt("quantidade")).thenReturn(2);

        List<Ingrediente> ingredientes = dao.listarTodosPorLanche(9);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexao).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("il.quantidade"));
        verify(comando).setInt(1, 9);
        assertEquals(1, ingredientes.size());
        Ingrediente doLanche = ingredientes.get(0);
        assertEquals(3, doLanche.getId_ingrediente());
        assertEquals("Queijo Cheddar", doLanche.getNome());
        assertEquals("Fatia de queijo cheddar", doLanche.getDescricao());
        assertEquals(2, doLanche.getQuantidade());
        assertEquals(0.80, doLanche.getValor_compra(), 0.001);
        assertEquals(2.00, doLanche.getValor_venda(), 0.001);
        assertEquals("queijo", doLanche.getTipo());
        assertEquals(1, doLanche.getFg_ativo());
        verify(resultado).close();
        verify(comando).close();
    }

    @Test
    @DisplayName("listarTodosPorLanche devolve lista vazia para lanche sem ingredientes")
    void deveDevolverListaVaziaParaLancheSemIngredientes() throws SQLException {
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        when(comando.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(false);

        assertTrue(dao.listarTodosPorLanche(9).isEmpty());
        verify(resultado).close();
        verify(comando).close();
    }

    @Test
    @DisplayName("listarTodos devolve uma entrada por linha, na ordem do banco")
    void deveListarVariasLinhasNaOrdem() throws SQLException {
        bancoRespondeLinhaDoCheddar();
        when(resultado.next()).thenReturn(true, true, false);
        when(resultado.getInt("id_ingrediente")).thenReturn(3, 4);
        when(resultado.getString("nm_ingrediente")).thenReturn("Queijo Cheddar", "Bacon");

        List<Ingrediente> ingredientes = dao.listarTodos();

        assertEquals(2, ingredientes.size());
        assertEquals("Queijo Cheddar", ingredientes.get(0).getNome());
        assertEquals(4, ingredientes.get(1).getId_ingrediente());
        assertEquals("Bacon", ingredientes.get(1).getNome());
    }

    @Test
    @DisplayName("pesquisaPorNome devolve o ingrediente encontrado")
    void deveEncontrarIngredientePorNome() throws SQLException {
        bancoRespondeLinhaDoCheddar();

        conferirCheddar(dao.pesquisaPorNome(cheddar()));
        verify(resultado).close();
        verify(comando).close();
    }

    @Test
    @DisplayName("pesquisaPorNome envia o nome como parâmetro, sem montar o SQL por concatenação")
    void devePesquisarPorNomeComParametro() throws SQLException {
        bancoRespondeLinhaDoCheddar();
        Ingrediente comApostrofo = cheddar();
        comApostrofo.setNome("Pão d'água");

        dao.pesquisaPorNome(comApostrofo);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexao).prepareStatement(sql.capture());
        assertTrue(sql.getValue().endsWith("nm_ingrediente = ?"));
        assertFalse(sql.getValue().contains("Pão d'água"));
        verify(comando).setString(1, "Pão d'água");
    }

    @Test
    @DisplayName("listarTodos nomeia as colunas em vez de usar SELECT * e filtra os ativos")
    void naoDeveUsarSelectAsterisco() throws SQLException {
        bancoRespondeLinhaDoCheddar();

        dao.listarTodos();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conexao).prepareStatement(sql.capture());
        assertFalse(sql.getValue().contains("*"));
        assertTrue(sql.getValue().contains("fg_ativo = 1"));
    }

    @Test
    @DisplayName("pesquisaPorNome devolve Ingrediente vazio quando o nome não existe")
    void deveDevolverIngredienteVazioQuandoNomeNaoExiste() throws SQLException {
        when(conexao.prepareStatement(anyString())).thenReturn(comando);
        when(comando.executeQuery()).thenReturn(resultado);
        when(resultado.next()).thenReturn(false);

        Ingrediente encontrado = dao.pesquisaPorNome(cheddar());

        assertEquals(0, encontrado.getId_ingrediente());
        assertNull(encontrado.getNome());
        assertNull(encontrado.getValor_venda());
    }

    @Test
    @DisplayName("Falha de SQL em qualquer operação vira RuntimeException com a causa original")
    void deveRepassarFalhaDoBanco() throws SQLException {
        SQLException falha = new SQLException("banco fora do ar");
        when(conexao.prepareStatement(anyString())).thenThrow(falha);

        assertEquals(falha, assertThrows(RuntimeException.class, () -> dao.salvar(cheddar())).getCause());
        assertEquals(falha, assertThrows(RuntimeException.class, () -> dao.alterar(cheddar())).getCause());
        assertEquals(falha, assertThrows(RuntimeException.class, () -> dao.remover(cheddar())).getCause());
        assertEquals(falha, assertThrows(RuntimeException.class, () -> dao.listarTodos()).getCause());
        assertEquals(falha, assertThrows(RuntimeException.class, () -> dao.listarTodosPorLanche(1)).getCause());
        assertEquals(falha, assertThrows(RuntimeException.class, () -> dao.pesquisaPorNome(cheddar())).getCause());
    }
}
