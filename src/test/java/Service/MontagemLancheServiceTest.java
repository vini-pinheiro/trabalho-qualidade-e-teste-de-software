package Service;

import DAO.DaoLanche;
import Model.Ingrediente;
import Model.Lanche;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitarios de MontagemLancheService")
class MontagemLancheServiceTest {

    @Mock
    private DaoLanche daoLanche;

    private MontagemLancheService service;

    @BeforeEach
    void setUp() {
        service = new MontagemLancheService(daoLanche);
    }

    private Lanche lancheValido() {
        Lanche lanche = new Lanche();
        lanche.setNome("X-Especial");
        lanche.setDescricao("Pao, carne e queijo");
        return lanche;
    }

    private Ingrediente ingredienteValido(int id, double valor, int quantidade) {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setId_ingrediente(id);
        ingrediente.setFg_ativo(1);
        ingrediente.setValor_venda(valor);
        ingrediente.setQuantidade(quantidade);
        return ingrediente;
    }

    @Nested
    @DisplayName("Construtor")
    class ConstrutorTests {

        @Test
        @DisplayName("Deve rejeitar DAO nulo")
        void deveRejeitarDaoNulo() {
            IllegalArgumentException excecao = assertThrows(
                    IllegalArgumentException.class,
                    () -> new MontagemLancheService(null)
            );

            assertEquals("O DAO de lanches não pode ser nulo", excecao.getMessage());
        }

        @Test
        @DisplayName("Deve construir o service com DAO valido")
        void deveConstruirComDaoValido() {
            assertNotNull(service);
        }
    }

    @Nested
    @DisplayName("Validacao dos dados do lanche")
    class DadosLancheTests {

        @Test
        @DisplayName("Deve rejeitar lanche nulo")
        void deveRejeitarLancheNulo() {
            IllegalArgumentException excecao = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(null,
                            Collections.singletonList(ingredienteValido(1, 10.0, 1)))
            );

            assertEquals("O lanche não pode ser nulo", excecao.getMessage());
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar nome nulo")
        void deveRejeitarNomeNulo() {
            Lanche lanche = lancheValido();
            lanche.setNome(null);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lanche,
                            Collections.singletonList(ingredienteValido(1, 10.0, 1))));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar nome vazio")
        void deveRejeitarNomeVazio() {
            Lanche lanche = lancheValido();
            lanche.setNome("   ");

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lanche,
                            Collections.singletonList(ingredienteValido(1, 10.0, 1))));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar descricao nula")
        void deveRejeitarDescricaoNula() {
            Lanche lanche = lancheValido();
            lanche.setDescricao(null);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lanche,
                            Collections.singletonList(ingredienteValido(1, 10.0, 1))));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar descricao vazia")
        void deveRejeitarDescricaoVazia() {
            Lanche lanche = lancheValido();
            lanche.setDescricao(" ");

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lanche,
                            Collections.singletonList(ingredienteValido(1, 10.0, 1))));
            verifyNoInteractions(daoLanche);
        }
    }

    @Nested
    @DisplayName("Validacao dos ingredientes")
    class IngredientesTests {

        @Test
        @DisplayName("Deve rejeitar lista nula")
        void deveRejeitarListaNula() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(), null));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar lista vazia")
        void deveRejeitarListaVazia() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(),
                            Collections.<Ingrediente>emptyList()));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar ingrediente nulo")
        void deveRejeitarIngredienteNulo() {
            List<Ingrediente> ingredientes = new ArrayList<Ingrediente>();
            ingredientes.add(null);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(), ingredientes));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar identificador invalido")
        void deveRejeitarIdentificadorInvalido() {
            Ingrediente ingrediente = ingredienteValido(0, 10.0, 1);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(),
                            Collections.singletonList(ingrediente)));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar ingrediente inativo")
        void deveRejeitarIngredienteInativo() {
            Ingrediente ingrediente = ingredienteValido(1, 10.0, 1);
            ingrediente.setFg_ativo(0);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(),
                            Collections.singletonList(ingrediente)));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar quantidade invalida")
        void deveRejeitarQuantidadeInvalida() {
            Ingrediente ingrediente = ingredienteValido(1, 10.0, 0);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(),
                            Collections.singletonList(ingrediente)));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar ingrediente duplicado")
        void deveRejeitarIngredienteDuplicado() {
            Ingrediente primeiro = ingredienteValido(1, 10.0, 1);
            Ingrediente segundo = ingredienteValido(1, 5.0, 2);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(),
                            Arrays.asList(primeiro, segundo)));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar valor nulo")
        void deveRejeitarValorNulo() {
            Ingrediente ingrediente = ingredienteValido(1, 10.0, 1);
            ingrediente.setValor_venda(null);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(),
                            Collections.singletonList(ingrediente)));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar valor NaN")
        void deveRejeitarValorNaN() {
            Ingrediente ingrediente = ingredienteValido(1, Double.NaN, 1);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(),
                            Collections.singletonList(ingrediente)));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar valor infinito")
        void deveRejeitarValorInfinito() {
            Ingrediente ingrediente = ingredienteValido(1, Double.POSITIVE_INFINITY, 1);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(),
                            Collections.singletonList(ingrediente)));
            verifyNoInteractions(daoLanche);
        }

        @Test
        @DisplayName("Deve rejeitar valor menor ou igual a zero")
        void deveRejeitarValorNaoPositivo() {
            Ingrediente ingrediente = ingredienteValido(1, 0.0, 1);

            assertThrows(IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lancheValido(),
                            Collections.singletonList(ingrediente)));
            verifyNoInteractions(daoLanche);
        }
    }

    @Nested
    @DisplayName("Montagem e persistencia")
    class MontagemTests {

        @Test
        @DisplayName("Deve montar lanche com sucesso e vincular todos os ingredientes")
        void deveMontarComSucesso() {
            Lanche lanche = lancheValido();
            lanche.setNome("  X-Especial  ");
            Ingrediente carne = ingredienteValido(1, 12.345, 2);
            Ingrediente queijo = ingredienteValido(2, 3.333, 1);
            List<Ingrediente> ingredientes = Arrays.asList(carne, queijo);

            when(daoLanche.pesquisaPorNome("X-Especial")).thenReturn(null);
            when(daoLanche.salvarCliente(lanche)).thenReturn(42);

            Lanche resultado = service.montarLanchePersonalizado(lanche, ingredientes);

            assertSame(lanche, resultado);
            assertEquals(42, resultado.getId_lanche());
            assertEquals(28.02, resultado.getValor_venda(), 0.001);
            assertEquals(28.02, resultado.getValor_lanche(), 0.001);
            verify(daoLanche).pesquisaPorNome("X-Especial");
            verify(daoLanche).salvarCliente(lanche);
            verify(daoLanche).vincularIngrediente(lanche, carne);
            verify(daoLanche).vincularIngrediente(lanche, queijo);
            verify(daoLanche, times(2)).vincularIngrediente(eq(lanche), any(Ingrediente.class));
        }

        @Test
        @DisplayName("Deve considerar lanche existente somente com ID positivo")
        void deveRejeitarNomeJaCadastrado() {
            Lanche lanche = lancheValido();
            Lanche existente = new Lanche();
            existente.setId_lanche(7);

            when(daoLanche.pesquisaPorNome("X-Especial")).thenReturn(existente);

            IllegalStateException excecao = assertThrows(IllegalStateException.class,
                    () -> service.montarLanchePersonalizado(lanche,
                            Collections.singletonList(ingredienteValido(1, 10.0, 1))));

            assertEquals("Já existe um lanche cadastrado com esse nome", excecao.getMessage());
            verify(daoLanche, never()).salvarCliente(any());
            verify(daoLanche, never()).vincularIngrediente(any(), any());
        }

        @Test
        @DisplayName("Deve permitir nome encontrado em objeto sem ID positivo")
        void devePermitirNomeSemIdentificador() {
            Lanche lanche = lancheValido();
            Lanche existente = new Lanche();
            existente.setId_lanche(0);
            Ingrediente ingrediente = ingredienteValido(1, 10.0, 1);

            when(daoLanche.pesquisaPorNome("X-Especial")).thenReturn(existente);
            when(daoLanche.salvarCliente(lanche)).thenReturn(10);

            service.montarLanchePersonalizado(lanche,
                    Collections.singletonList(ingrediente));

            verify(daoLanche).salvarCliente(lanche);
        }

        @Test
        @DisplayName("Deve rejeitar ID invalido retornado pelo DAO")
        void deveRejeitarIdInvalidoRetornado() {
            Lanche lanche = lancheValido();
            Ingrediente ingrediente = ingredienteValido(1, 10.0, 1);

            when(daoLanche.pesquisaPorNome("X-Especial")).thenReturn(null);
            when(daoLanche.salvarCliente(lanche)).thenReturn(0);

            IllegalStateException excecao = assertThrows(IllegalStateException.class,
                    () -> service.montarLanchePersonalizado(lanche,
                            Collections.singletonList(ingrediente)));

            assertEquals("Não foi possível identificar o lanche cadastrado",
                    excecao.getMessage());
            verify(daoLanche, never()).vincularIngrediente(any(), any());
        }

        @Test
        @DisplayName("Deve rejeitar ID negativo retornado pelo DAO")
        void deveRejeitarIdNegativoRetornado() {
            Lanche lanche = lancheValido();
            Ingrediente ingrediente = ingredienteValido(1, 10.0, 1);

            when(daoLanche.pesquisaPorNome("X-Especial")).thenReturn(null);
            when(daoLanche.salvarCliente(lanche)).thenReturn(-1);

            assertThrows(IllegalStateException.class,
                    () -> service.montarLanchePersonalizado(lanche,
                            Collections.singletonList(ingrediente)));
            verify(daoLanche, never()).vincularIngrediente(any(), any());
        }

        @Test
        @DisplayName("Deve rejeitar valor total acima do limite")
        void deveRejeitarValorAcimaDoLimite() {
            Lanche lanche = lancheValido();
            Ingrediente ingrediente = ingredienteValido(1, 100000.0, 1);

            IllegalArgumentException excecao = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.montarLanchePersonalizado(lanche,
                            Collections.singletonList(ingrediente))
            );

            assertEquals("O valor total ultrapassa o limite permitido",
                    excecao.getMessage());
            verify(daoLanche).pesquisaPorNome("X-Especial");
            verify(daoLanche, never()).salvarCliente(any());
        }

        @Test
        @DisplayName("Deve arredondar o subtotal para duas casas")
        void deveArredondarValorTotal() {
            Lanche lanche = lancheValido();
            Ingrediente ingrediente = ingredienteValido(1, 10.005, 1);

            when(daoLanche.pesquisaPorNome("X-Especial")).thenReturn(null);
            when(daoLanche.salvarCliente(lanche)).thenReturn(11);

            service.montarLanchePersonalizado(lanche,
                    Collections.singletonList(ingrediente));

            assertEquals(10.01, lanche.getValor_venda(), 0.001);
        }
    }
}
