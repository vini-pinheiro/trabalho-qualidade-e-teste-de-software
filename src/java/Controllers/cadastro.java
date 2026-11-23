/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controllers;

import DAO.DaoCliente;
import Model.Cliente;
import Model.Endereco;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 *
 * @author kener_000
 */
@WebServlet(name = "cadastro", urlPatterns = {"/cadastro"})
public class cadastro extends HttpServlet {

    // Entrega 2 (Sonar): logger para as falhas de doGet/doPost e constantes para os blocos do JSON
    private static final Logger LOGGER = Logger.getLogger(cadastro.class.getName());
    private static final String ENDERECO = "endereco";
    private static final String USUARIO = "usuario";

    // Nulo no uso normal (Tomcat): o DaoCliente continua sendo criado a cada requisição
    // Entrega 2 (Sonar): final, porque um servlet é compartilhado entre requisições
    private final DaoCliente clienteDaoInjetado;

    public cadastro() {
        this.clienteDaoInjetado = null;
    }

    // Usado nos testes para isolar o banco
    public cadastro(DaoCliente clienteDao) {
        this.clienteDaoInjetado = clienteDao;
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        //Seta o tipo de Conteudo que será recebido, nesse caso, um JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        //Pra receber JSONs, é necessario utilizar esse Buffer pra receber os dados,
        //Então tem que ser Feito assim:
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));

        // Entrega 2 (Sonar): sem a checagem de leitor nulo, que era sempre verdadeira (o leitor acabou de ser criado)

        //Converte os dados do JSON pra um Formato de Objeto que o Java consiga lidar
        String json = br.readLine();
        byte[] bytes = json.getBytes(ISO_8859_1);
        String jsonStr = new String(bytes, UTF_8);
        JSONObject dados = new JSONObject(jsonStr);

        //Aqui, ele Instancia um objeto do Model endereco, e Popula ele com os dados do JSON
        Endereco endereco = new Endereco();
        endereco.setBairro(dados.getJSONObject(ENDERECO).getString("bairro"));
        endereco.setCidade(dados.getJSONObject(ENDERECO).getString("cidade"));
        endereco.setEstado(dados.getJSONObject(ENDERECO).getString("estado"));
        endereco.setComplemento(dados.getJSONObject(ENDERECO).getString("complemento"));
        endereco.setRua(dados.getJSONObject(ENDERECO).getString("rua"));
        endereco.setNumero(dados.getJSONObject(ENDERECO).getInt("numero"));

        //Aqui, ele Instancia um objeto do Model Cliente, e Popula ele com os dados do JSON
        Cliente cliente = new Cliente();
        cliente.setNome(dados.getJSONObject(USUARIO).getString("nome"));
        cliente.setSobrenome(dados.getJSONObject(USUARIO).getString("sobrenome"));
        cliente.setTelefone(dados.getJSONObject(USUARIO).getString("telefone"));
        cliente.setUsuario(dados.getJSONObject(USUARIO).getString(USUARIO));
        cliente.setSenha(dados.getJSONObject(USUARIO).getString("senha"));
        cliente.setFg_ativo(1);

        //E Para finalizar, salva no Banco usando o DAO deles
        cliente.setEndereco(endereco);

        DaoCliente clienteDAO = (clienteDaoInjetado != null) ? clienteDaoInjetado : new DaoCliente();
        clienteDAO.salvar(cliente);

        
        
        try (PrintWriter out = response.getWriter()) {
            
            //Aqui é onde a Resposta é mandada para o Cliente, dando um Feedback de que tudo deu certo.
            out.println("Usuário Cadastrado!");

        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        tratarRequisicao(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        tratarRequisicao(request, response);
    }

    // Entrega 2 (Sonar): ServletException e IOException passam a ser registradas em log e respondidas com HTTP 500,
    // em vez de escaparem do servlet. Erros de JSON (RuntimeException) continuam saindo como antes.
    private void tratarRequisicao(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
        } catch (ServletException | IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao processar o cadastro", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
