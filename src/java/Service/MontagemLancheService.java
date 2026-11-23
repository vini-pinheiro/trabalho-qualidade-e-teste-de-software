
package Service;

import DAO.DaoLanche;
import Model.Ingrediente;
import Model.Lanche;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serviço responsável pelas regras de negócio relacionadas à montagem de lanches personalizados pelos clientes.
 * @author Vinicius Fonseca
 */
public class MontagemLancheService {

    private static final int INGREDIENTE_ATIVO = 1;
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999.99");

    private final DaoLanche daoLanche;

    /**
     * Constrói o serviço com a dependência de persistência injetada.
     * @param daoLanche DAO utilizado para consultar e persistir lanches
     */
    public MontagemLancheService(DaoLanche daoLanche) {
        if (daoLanche == null) {
            throw new IllegalArgumentException("O DAO de lanches não pode ser nulo");
        }

        this.daoLanche = daoLanche;
    }

    /**
     * Valida, calcula e cadastra um lanche personalizado,
     * associando os ingredientes selecionados pelo cliente.
     * O valor de venda do lanche é calculado pela soma
     * do valor de venda de cada ingrediente multiplicado
     * pela quantidade utilizada na composição.
     * @param lanche dados do lanche personalizado
     * @param ingredientes ingredientes selecionados pelo cliente
     * @return lanche cadastrado com seu identificador e valor calculado
     */
    public Lanche montarLanchePersonalizado(
            Lanche lanche,
            List<Ingrediente> ingredientes) {

        validarDadosLanche(lanche);
        validarIngredientesSelecionados(ingredientes);

        verificarNomeDisponivel(lanche);

        BigDecimal valorTotal = calcularValorComposicao(ingredientes);

        valorTotal = validarValorTotal(valorTotal);

        lanche.setValor_venda(valorTotal.doubleValue());
        lanche.setValor_lanche(valorTotal.doubleValue());

        int idLanche = daoLanche.salvarCliente(lanche);

        if (idLanche <= 0) {
            throw new IllegalStateException(
                    "Não foi possível identificar o lanche cadastrado"
            );
        }

        lanche.setId_lanche(idLanche);

        for (Ingrediente ingrediente : ingredientes) {
            daoLanche.vincularIngrediente(lanche, ingrediente);
        }

        return lanche;
    }

    /**
     * Valida os dados básicos do lanche personalizado.
     *
     * @param lanche lanche que será validado
     */
    private void validarDadosLanche(Lanche lanche) {

        if (lanche == null) {
            throw new IllegalArgumentException(
                    "O lanche não pode ser nulo"
            );
        }

        if (lanche.getNome() == null) {
            throw new IllegalArgumentException(
                    "O nome do lanche é obrigatório"
            );
        }

        if (lanche.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "O nome do lanche não pode estar vazio"
            );
        }

        if (lanche.getDescricao() == null) {
            throw new IllegalArgumentException(
                    "A descrição do lanche é obrigatória"
            );
        }

        if (lanche.getDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "A descrição do lanche não pode estar vazia"
            );
        }
    }

    /**
     * Verifica se os ingredientes selecionados são válidos.
     * Impede ingredientes nulos, inativos, identificadores inválidos,
     * quantidades inválidas e ingredientes duplicados.
     * @param ingredientes ingredientes selecionados
     */
    private void validarIngredientesSelecionados(
            List<Ingrediente> ingredientes) {

        if (ingredientes == null) {
            throw new IllegalArgumentException(
                    "A lista de ingredientes não pode ser nula"
            );
        }

        if (ingredientes.isEmpty()) {
            throw new IllegalArgumentException(
                    "O lanche deve possuir pelo menos um ingrediente"
            );
        }

        Set<Integer> idsIngredientes = new HashSet<Integer>();

        for (Ingrediente ingrediente : ingredientes) {

            if (ingrediente == null) {
                throw new IllegalArgumentException(
                        "A lista não pode conter ingredientes nulos"
                );
            }

            if (ingrediente.getId_ingrediente() <= 0) {
                throw new IllegalArgumentException(
                        "O ingrediente possui identificador inválido"
                );
            }

            if (ingrediente.getFg_ativo() != INGREDIENTE_ATIVO) {
                throw new IllegalArgumentException(
                        "Não é permitido utilizar ingredientes inativos"
                );
            }

            if (ingrediente.getQuantidade() <= 0) {
                throw new IllegalArgumentException(
                        "A quantidade do ingrediente deve ser maior que zero"
                );
            }

            if (!idsIngredientes.add(ingrediente.getId_ingrediente())) {
                throw new IllegalArgumentException(
                        "Não é permitido adicionar ingredientes duplicados"
                );
            }

            if (ingrediente.getValor_venda() == null) {
                throw new IllegalArgumentException(
                        "O valor de venda do ingrediente é obrigatório"
                );
            }

            double valorIngrediente = ingrediente.getValor_venda();

            if (Double.isNaN(valorIngrediente)
                    || Double.isInfinite(valorIngrediente)) {
                throw new IllegalArgumentException(
                        "O valor do ingrediente deve ser um número válido"
                );
            }

            if (valorIngrediente <= 0) {
                throw new IllegalArgumentException(
                        "O valor do ingrediente deve ser maior que zero"
                );
            }
        }
    }

    /**
     * Verifica se já existe um lanche com o mesmo nome.
     *
     * @param lanche lanche que será cadastrado
     */
    private void verificarNomeDisponivel(Lanche lanche) {

        Lanche existente = daoLanche.pesquisaPorNome(
                lanche.getNome().trim()
        );

        if (existente != null && existente.getId_lanche() > 0) {
            throw new IllegalStateException(
                    "Já existe um lanche cadastrado com esse nome"
            );
        }
    }

    /**
     * Calcula o valor total dos ingredientes utilizados.
     *
     * @param ingredientes ingredientes da composição
     * @return valor total calculado com precisão decimal
     */
    private BigDecimal calcularValorComposicao(
            List<Ingrediente> ingredientes) {

        BigDecimal valorTotal = BigDecimal.ZERO;

        for (Ingrediente ingrediente : ingredientes) {

            BigDecimal valorUnitario = BigDecimal.valueOf(
                    ingrediente.getValor_venda()
            );

            BigDecimal quantidade = BigDecimal.valueOf(
                    ingrediente.getQuantidade()
            );

            BigDecimal subtotal = valorUnitario.multiply(quantidade);

            valorTotal = valorTotal.add(subtotal);
        }

        return valorTotal;
    }

    /**
     * Verifica se o valor calculado pode ser armazenado
     * na coluna NUMERIC(7,2) do banco de dados.
     *
     * @param valorTotal valor calculado da composição
     */
    private BigDecimal validarValorTotal(BigDecimal valorTotal) {

        if (valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "O valor total do lanche deve ser maior que zero"
            );
        }

        valorTotal = valorTotal.setScale(
                2,
                java.math.RoundingMode.HALF_UP
        );

        if (valorTotal.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                    "O valor total ultrapassa o limite permitido"
            );
        }

        return valorTotal;
    }
}
