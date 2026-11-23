package DAO;

import Model.Ingrediente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração de DAO.DaoIngrediente contra o PostgreSQL 13 do docker-compose
 * (host "db", banco "lanchonete"). Rodam só no perfil "integracao": mvn -Pintegracao verify
 */
class DaoIngredienteIT {

    private Connection jdbcConnection;
    private Connection daoConnection;
    private DaoIngrediente dao;

    private String prefixo;
    private int idLanche;

    @BeforeEach
    void preparar() {
        jdbcConnection = new DaoUtil().conecta();
        daoConnection = new DaoUtil().conecta();
        dao = new DaoIngrediente(daoConnection);
        // Todo dado criado pelo teste começa com este prefixo, para a limpeza não tocar em mais nada
        prefixo = "IT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + " ";
    }

    @AfterEach
    void limpar() throws SQLException {
        try {
            if (jdbcConnection != null && !jdbcConnection.isClosed()) {
                executar("DELETE FROM tb_ingredientes_lanche WHERE id_ingrediente IN "
                        + "(SELECT id_ingrediente FROM tb_ingredientes WHERE nm_ingrediente LIKE ?)", prefixo + "%");
                if (idLanche > 0) {
                    executar("DELETE FROM tb_ingredientes_lanche WHERE id_lanche = ?", idLanche);
                    executar("DELETE FROM tb_lanches WHERE id_lanche = ?", idLanche);
                }
                executar("DELETE FROM tb_ingredientes WHERE nm_ingrediente LIKE ?", prefixo + "%");
            }
        } finally {
            fecharSilenciosamente(daoConnection);
            fecharSilenciosamente(jdbcConnection);
        }
    }

    private Ingrediente novoIngrediente(String nome, int quantidade) {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setNome(prefixo + nome);
        ingrediente.setDescricao("Insumo do teste de integracao");
        ingrediente.setQuantidade(quantidade);
        ingrediente.setValor_compra(0.85);
        ingrediente.setValor_venda(2.50);
        ingrediente.setTipo("Queijo");
        ingrediente.setFg_ativo(1);
        return ingrediente;
    }

    private int salvarEObterId(Ingrediente ingrediente) {
        dao.salvar(ingrediente);
        int id = dao.pesquisaPorNome(ingrediente).getId_ingrediente();
        assertTrue(id > 0, "O banco deve gerar id_ingrediente para o insumo salvo");
        ingrediente.setId_ingrediente(id);
        return id;
    }

    private void executar(String sql, Object... parametros) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                stmt.setObject(i + 1, parametros[i]);
            }
            stmt.executeUpdate();
        }
    }

    private int contarPorId(int idIngrediente) throws SQLException {
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(
                "SELECT COUNT(*) FROM tb_ingredientes WHERE id_ingrediente = ?")) {
            stmt.setInt(1, idIngrediente);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int inserirLanche() throws SQLException {
        String sql = "INSERT INTO tb_lanches(nm_lanche, descricao, valor_venda, fg_ativo) "
                + "VALUES(?,?,?,1) RETURNING id_lanche";
        try (PreparedStatement stmt = jdbcConnection.prepareStatement(sql)) {
            stmt.setString(1, prefixo.trim());
            stmt.setString(2, "Lanche do teste de integracao");
            stmt.setDouble(3, 18.00);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Lanche de teste deve gerar id_lanche");
                return rs.getInt(1);
            }
        }
    }

    private Ingrediente buscarNaLista(List<Ingrediente> lista, int idIngrediente) {
        for (Ingrediente ingrediente : lista) {
            if (ingrediente.getId_ingrediente() == idIngrediente) {
                return ingrediente;
            }
        }
        return null;
    }

    private void fecharSilenciosamente(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
                // conexão de teste: nada a fazer se já caiu
            }
        }
    }

    @Test
    @DisplayName("salvar grava o insumo e pesquisaPorNome o devolve com todos os campos")
    void deveSalvarEPesquisarInsumoNoPostgres() throws SQLException {
        Ingrediente cheddar = novoIngrediente("Cheddar", 150);

        int id = salvarEObterId(cheddar);
        Ingrediente encontrado = dao.pesquisaPorNome(cheddar);

        assertEquals(id, encontrado.getId_ingrediente());
        assertEquals(prefixo + "Cheddar", encontrado.getNome());
        assertEquals("Insumo do teste de integracao", encontrado.getDescricao());
        assertEquals(150, encontrado.getQuantidade());
        assertEquals(0.85, encontrado.getValor_compra(), 0.001);
        assertEquals(2.50, encontrado.getValor_venda(), 0.001);
        assertEquals("Queijo", encontrado.getTipo());
        assertEquals(1, encontrado.getFg_ativo());
        assertEquals(1, contarPorId(id));
    }

    @Test
    @DisplayName("pesquisaPorNome encontra nome com apóstrofo, sem quebrar o SQL (defeito #23)")
    void devePesquisarNomeComApostrofo() {
        Ingrediente paoDagua = novoIngrediente("Pão d'água", 20);

        int id = salvarEObterId(paoDagua);

        assertEquals(id, dao.pesquisaPorNome(paoDagua).getId_ingrediente());
    }

    @Test
    @DisplayName("pesquisaPorNome não é desviada por entrada com injeção de SQL")
    void naoDeveAceitarInjecaoDeSqlNaPesquisa() {
        salvarEObterId(novoIngrediente("Bacon", 40));
        Ingrediente ataque = new Ingrediente();
        ataque.setNome("x' OR '1'='1");

        Ingrediente encontrado = dao.pesquisaPorNome(ataque);

        assertEquals(0, encontrado.getId_ingrediente());
        assertNull(encontrado.getNome());
    }

    @Test
    @DisplayName("alterar atualiza quantidade e preços no banco e mantém o insumo ativo")
    void deveAlterarInsumoNoPostgres() {
        Ingrediente alface = novoIngrediente("Alface", 10);
        salvarEObterId(alface);
        alface.setQuantidade(0);
        alface.setValor_compra(1.10);
        alface.setValor_venda(3.00);

        dao.alterar(alface);
        Ingrediente encontrado = dao.pesquisaPorNome(alface);

        assertEquals(0, encontrado.getQuantidade());
        assertEquals(1.10, encontrado.getValor_compra(), 0.001);
        assertEquals(3.00, encontrado.getValor_venda(), 0.001);
        assertTrue(buscarNaLista(dao.listarTodos(), alface.getId_ingrediente()) != null,
                "Insumo alterado continua na lista de ativos");
    }

    @Test
    @DisplayName("listarTodos traz só os ativos, em ordem de id")
    void deveListarSomenteAtivosEmOrdem() throws SQLException {
        int idTomate = salvarEObterId(novoIngrediente("Tomate", 30));
        int idCebola = salvarEObterId(novoIngrediente("Cebola", 25));
        Ingrediente inativo = novoIngrediente("Picles", 5);
        int idInativo = salvarEObterId(inativo);
        executar("UPDATE tb_ingredientes SET fg_ativo = 0 WHERE id_ingrediente = ?", idInativo);

        List<Ingrediente> ativos = dao.listarTodos();

        assertTrue(buscarNaLista(ativos, idTomate) != null);
        assertTrue(buscarNaLista(ativos, idCebola) != null);
        assertNull(buscarNaLista(ativos, idInativo), "Insumo com fg_ativo = 0 não aparece");
        for (int i = 1; i < ativos.size(); i++) {
            assertTrue(ativos.get(i - 1).getId_ingrediente() < ativos.get(i).getId_ingrediente(),
                    "listarTodos deve vir ordenado por id_ingrediente");
        }
    }

    @Test
    @DisplayName("listarTodosPorLanche traz a quantidade do vínculo, não a do estoque")
    void deveListarIngredientesDoLancheComQuantidadeDoVinculo() throws SQLException {
        int idQueijo = salvarEObterId(novoIngrediente("Queijo", 200));
        salvarEObterId(novoIngrediente("Fora do lanche", 50));
        idLanche = inserirLanche();
        executar("INSERT INTO tb_ingredientes_lanche(id_lanche, id_ingrediente, quantidade) VALUES(?,?,?)",
                idLanche, idQueijo, 2);

        List<Ingrediente> doLanche = dao.listarTodosPorLanche(idLanche);

        assertEquals(1, doLanche.size());
        assertEquals(idQueijo, doLanche.get(0).getId_ingrediente());
        assertEquals(2, doLanche.get(0).getQuantidade());
        assertEquals(200, dao.pesquisaPorNome(novoIngrediente("Queijo", 0)).getQuantidade(),
                "O estoque do insumo não muda ao vinculá-lo a um lanche");
    }

    @Test
    @DisplayName("remover apaga o insumo do banco")
    void deveRemoverInsumoDoPostgres() throws SQLException {
        Ingrediente oregano = novoIngrediente("Oregano", 8);
        int id = salvarEObterId(oregano);

        dao.remover(oregano);

        assertEquals(0, contarPorId(id));
        assertFalse(dao.pesquisaPorNome(oregano).getId_ingrediente() > 0);
    }

    @Test
    @DisplayName("O banco aceita quantidade e valores negativos: tb_ingredientes não tem CHECK (defeito #25)")
    void caracterizacaoBancoAceitaValoresNegativos() {
        Ingrediente negativo = novoIngrediente("Negativo", -5);
        negativo.setValor_compra(-1.00);
        negativo.setValor_venda(-2.00);

        salvarEObterId(negativo);
        Ingrediente gravado = dao.pesquisaPorNome(negativo);

        assertEquals(-5, gravado.getQuantidade());
        assertEquals(-2.00, gravado.getValor_venda(), 0.001);
    }
}
