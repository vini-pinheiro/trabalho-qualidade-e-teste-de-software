/* Persistência e consultas ao banco de dados. */
package DAO;

import Model.Ingrediente;
import Model.Lanche;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Statement;

/**
 *
 * @author kener_000
 */
public class DaoLanche {
    private final Connection conecta;

    public DaoLanche() {
        this.conecta = new DaoUtil().conecta();
    }

    /**
     * Construtor com o intuito de facilitar os testes unitários sem depender de um banco de dados real em execução
     * @author Vinicius Fonseca
     * */
    public DaoLanche(Connection conecta) {
        this.conecta = conecta;
    }

    public void salvar(Lanche lanche) {
        salvar(lanche, 1);
    }

    public int salvarCliente(Lanche lanche) {
        return salvar(lanche, 0);
    }

    private int salvar(Lanche lanche, int status) {
        String sql = "INSERT INTO tb_lanches(nm_lanche, descricao, valor_venda, fg_ativo) "
                + "VALUES(?,?,?,?)";

        validarLanche(lanche);

        try (PreparedStatement stmt = conecta.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, lanche.getNome());
            stmt.setString(2, lanche.getDescricao());
            stmt.setDouble(3, lanche.getValor_venda());
            stmt.setInt(4, status);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new SQLException("Não foi possível recuperar o ID do lanche");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Lanche> listarTodos() {
        String sql = "SELECT * FROM tb_lanches WHERE fg_Ativo='1' ORDER BY id_lanche";

        List<Lanche> lanches = new ArrayList<>();

        try (PreparedStatement stmt = conecta.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lanches.add(criarLanche(rs, 1));
            }

            return lanches;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public Lanche pesquisaPorNome(Lanche lanche){
        if (lanche == null) {
            throw new IllegalArgumentException("O lanche não pode ser nulo");
        }
        return pesquisaPorNome(lanche.getNome());
    }
    
    public Lanche pesquisaPorNome(String nome){
        String sql = "SELECT * FROM tb_lanches WHERE nm_lanche=?";
        Lanche lancheResultado = new Lanche();
        
        validarNome(nome);

        try (PreparedStatement stmt = conecta.prepareStatement(sql);
             ResultSet rs = buscaPorNome(stmt, nome)){
            while (rs.next()){
                lancheResultado = criarLanche(rs, 1);
            }
            return lancheResultado;
        } catch(SQLException e){
             throw new RuntimeException(e);
        }
        
    }

    public void vincularIngrediente(Lanche lanche, Ingrediente ingrediente){
        String sql = "INSERT INTO tb_ingredientes_lanche(id_lanche, id_ingrediente, quantidade)"
                + "VALUES(?,?,?)";

        validarLancheVinculo(lanche, ingrediente);

        try (PreparedStatement stmt = conecta.prepareStatement(sql)){
            stmt.setInt(1, lanche.getId_lanche());
            stmt.setInt(2, ingrediente.getId_ingrediente());
            stmt.setInt(3, ingrediente.getQuantidade());
            stmt.execute();
        } catch(SQLException e){
             throw new RuntimeException(e);
        }
    }

    private void validarLanche(Lanche lanche) {
        if (lanche == null) {
            throw new IllegalArgumentException("O lanche não pode ser nulo");
        }
        if (lanche.getNome() == null) {
            throw new IllegalArgumentException("O nome do lanche é obrigatório");
        }
        if (lanche.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do lanche não pode estar vazio");
        }
        if (lanche.getDescricao() == null) {
            throw new IllegalArgumentException("A descrição do lanche é obrigatória");
        }
        if (lanche.getDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("A descrição do lanche não pode estar vazia");
        }
        if (lanche.getValor_venda() == null) {
            throw new IllegalArgumentException("O valor do lanche é obrigatório");
        }
        if (Double.isNaN(lanche.getValor_venda())) {
            throw new IllegalArgumentException("O valor do lanche não pode ser NaN");
        }
        if (Double.isInfinite(lanche.getValor_venda())) {
            throw new IllegalArgumentException("O valor do lanche não pode ser infinito");
        }
        if (lanche.getValor_venda() < 0) {
            throw new IllegalArgumentException("O valor do lanche não pode ser negativo");
        }
    }

    private void validarNome(String nome) {
        if (nome == null) {
            throw new IllegalArgumentException("O nome da pesquisa é obrigatório");
        }
        if (nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da pesquisa não pode estar vazio");
        }
    }

    private void validarLancheVinculo(Lanche lanche, Ingrediente ingrediente) {
        if (lanche == null) {
            throw new IllegalArgumentException("O lanche não pode ser nulo");
        }
        if (lanche.getId_lanche() <= 0) {
            throw new IllegalArgumentException("O lanche deve possuir um identificador válido");
        }
        if (ingrediente == null) {
            throw new IllegalArgumentException("O ingrediente não pode ser nulo");
        }
        if (ingrediente.getId_ingrediente() <= 0) {
            throw new IllegalArgumentException("O ingrediente deve possuir um identificador válido");
        }
        if (ingrediente.getQuantidade() <= 0) {
            throw new IllegalArgumentException("A quantidade do ingrediente deve ser maior que zero");
        }
    }

    private ResultSet buscaPorNome(PreparedStatement stmt, String nome) throws SQLException {
        stmt.setString(1, nome);
        return stmt.executeQuery();
    }

    private Lanche criarLanche(ResultSet rs, int status) throws SQLException {
        Lanche lanche = new Lanche();
        lanche.setId_lanche(rs.getInt("id_lanche"));
        lanche.setNome(rs.getString("nm_lanche"));
        lanche.setDescricao(rs.getString("descricao"));
        lanche.setValor_venda(rs.getDouble("valor_venda"));
        lanche.setFg_ativo(status);
        return lanche;
    }
}
    
    
    
    
