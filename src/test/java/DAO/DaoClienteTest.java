package DAO;

import Model.Cliente;
import Helpers.EncryptadorMD5;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;


import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DaoClienteTest {

    // 1. Criamos os "atores falsos" do JDBC
    @Mock
    private Connection Falseconnection;

    @Mock
    private PreparedStatement Falsestatement;

    @Mock
    private ResultSet FalseresultSet;

    private DaoCliente dao;

    @BeforeEach
    void setUp() throws Exception {
        // Criamos a instância normalmente (pode dar erro de conexão aqui se o banco estiver desligado,
        // por isso substituímos o campo 'conecta' imediatamente via Reflection)
        Objenesis objenesis = new ObjenesisStd();


        dao = objenesis.newInstance(DaoCliente.class);

        // Injeta a Falseconnection diretamente dentro do campo privado "conecta" do DaoCliente
        Field campoConecta = DaoCliente.class.getDeclaredField("conecta");
        campoConecta.setAccessible(true);
        campoConecta.set(dao, Falseconnection);
    }

    // Método utilitário simples para instanciar a classe sem executar o "new DaoUtil().conecta()"
    /*private Object allocateInstanceWithoutConstructor(Class<?> clazz) throws Exception {
        sun.misc.Unsafe unsafe;
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (sun.misc.Unsafe) f.get(null);
        return unsafe.allocateInstance(clazz);
    }*/

    @Test
    void testeLoginComSucesso() throws Exception {
        // PASSO 1: DADOS FICTÍCIOS DE ENTRADA (O que o usuário digitaria na tela)
        Cliente clienteTentandoLogar = new Cliente();
        clienteTentandoLogar.setUsuario("admin");
        clienteTentandoLogar.setSenha("123456");

        // PASSO 2: ENSINAR O BANCO FALSO A RESPONDER (Quando o Java perguntar ao Mockito)
        // Quando pedir para preparar a SQL, devolva o statement falso:
        when(Falseconnection.prepareStatement(anyString())).thenReturn(Falsestatement);
        // Quando executar a SQL, devolva o resultado falso:
        when(Falsestatement.executeQuery()).thenReturn(FalseresultSet);

        // Fingimos que o banco achou 1 linha (primeiro next() é true, o próximo é false para sair do loop)
        when(FalseresultSet.next()).thenReturn(true, false);
        // Fingimos os dados que o banco retornaria para essa linha:
        when(FalseresultSet.getString("usuario")).thenReturn("admin");
        when(FalseresultSet.getString("senha")).thenReturn(new EncryptadorMD5().encryptar("123456"));
        when(FalseresultSet.getInt("fg_ativo")).thenReturn(1);

        // PASSO 3: EXECUTAR O MÉTODO REAL
        boolean logou = dao.login(clienteTentandoLogar);

        // PASSO 4: CONFERIR SE DEU CERTO
        assertTrue(logou, "O login deveria ser aprovado com usuário e senha corretos");
    }

    @Test
    void testeLoginSenhaIncorreta() throws Exception {
        // PASSO 1: DADOS FICTÍCIOS (Senha errada digitada)
        Cliente clienteTentandoLogar = new Cliente();
        clienteTentandoLogar.setUsuario("admin");
        clienteTentandoLogar.setSenha("senha_errada");

        // PASSO 2: O banco finge que a senha guardada lá dentro é a do "123456"
        when(Falseconnection.prepareStatement(anyString())).thenReturn(Falsestatement);
        when(Falsestatement.executeQuery()).thenReturn(FalseresultSet);

        when(FalseresultSet.next()).thenReturn(true, false);
        when(FalseresultSet.getString("usuario")).thenReturn("admin");
        when(FalseresultSet.getString("senha")).thenReturn(new EncryptadorMD5().encryptar("123456"));
        when(FalseresultSet.getInt("fg_ativo")).thenReturn(1);

        // PASSO 3: EXECUTAR
        boolean logou = dao.login(clienteTentandoLogar);

        // PASSO 4: CONFERIR (Deve ser falso!)
        assertFalse(logou, "O login não deve ser permitido com senha errada");
    }

    @Test
    void testePesquisaPorUsuarioComSucesso() throws Exception {
        // PASSO 1: DADOS FICTÍCIOS DE ENTRADA
        Cliente parametroPesquisa = new Cliente();
        parametroPesquisa.setUsuario("carlos.silva");

        // PASSO 2: ENSINAR O BANCO FALSO A RESPONDER
        when(Falseconnection.prepareStatement(anyString())).thenReturn(Falsestatement);
        when(Falsestatement.executeQuery()).thenReturn(FalseresultSet);

        // O ResultSet encontra 1 registo e depois termina
        when(FalseresultSet.next()).thenReturn(true, false);
        when(FalseresultSet.getInt("id_cliente")).thenReturn(15);
        when(FalseresultSet.getString("nome")).thenReturn("Carlos");
        when(FalseresultSet.getString("sobrenome")).thenReturn("Silva");
        when(FalseresultSet.getString("telefone")).thenReturn("988887777");
        when(FalseresultSet.getString("usuario")).thenReturn("carlos.silva");
        when(FalseresultSet.getString("senha")).thenReturn("hash123");

        // PASSO 3: EXECUTAR O MÉTODO
        Cliente resultado = dao.pesquisaPorUsuario(parametroPesquisa);

        // PASSO 4: VALIDAÇÕES
        assertNotNull(resultado, "O cliente retornado não deve ser nulo");
        assertEquals(15, resultado.getId_cliente());
        assertEquals("Carlos", resultado.getNome());
        assertEquals("Silva", resultado.getSobrenome());
        assertEquals("carlos.silva", resultado.getUsuario());
        assertEquals(1, resultado.getFg_ativo());
    }

    @Test
    void testePesquisaPorIDComSucesso() throws Exception {
        // PASSO 1: ENSINAR O BANCO FALSO A RESPONDER
        when(Falseconnection.prepareStatement(anyString())).thenReturn(Falsestatement);
        when(Falsestatement.executeQuery()).thenReturn(FalseresultSet);

        when(FalseresultSet.next()).thenReturn(true, false);
        when(FalseresultSet.getInt("id_cliente")).thenReturn(42);
        when(FalseresultSet.getString("nome")).thenReturn("Ana");
        when(FalseresultSet.getString("sobrenome")).thenReturn("Souza");
        when(FalseresultSet.getString("telefone")).thenReturn("911112222");

        // PASSO 2: EXECUTAR PASSANDO O ID COMO STRING
        Cliente resultado = dao.pesquisaPorID("42");

        // PASSO 3: VALIDAÇÕES
        assertNotNull(resultado);
        assertEquals(42, resultado.getId_cliente());
        assertEquals("Ana", resultado.getNome());
        assertEquals("Souza", resultado.getSobrenome());
        assertEquals("911112222", resultado.getTelefone());
        assertEquals(1, resultado.getFg_ativo());
    }

    @Test
    void testePesquisaPorIDNaoEncontrado() throws Exception {
        // Simula o caso em que o ID pesquisado não existe na base de dados (next() retorna false logo de início)
        when(Falseconnection.prepareStatement(anyString())).thenReturn(Falsestatement);
        when(Falsestatement.executeQuery()).thenReturn(FalseresultSet);
        when(FalseresultSet.next()).thenReturn(false);

        Cliente resultado = dao.pesquisaPorID("999");

        // O método instancia um new Cliente() vazio caso não entre no while
        assertNotNull(resultado);
        assertEquals(0, resultado.getId_cliente());
        assertNull(resultado.getNome());
    }
}