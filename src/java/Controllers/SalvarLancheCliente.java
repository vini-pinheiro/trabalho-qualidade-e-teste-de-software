/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controllers;

import DAO.DaoIngrediente;
import DAO.DaoLanche;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import Model.Lanche;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 *
 * @author kener_000
 */
public class SalvarLancheCliente extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SalvarLancheCliente.class.getName());

    private final ValidadorCookie validadorCookie;
    private final DaoLanche lancheDao;
    private final DaoIngrediente ingredienteDao;

    public SalvarLancheCliente() {
        this(new ValidadorCookie(), new DaoLanche(), new DaoIngrediente());
    }

    // Usado nos testes para isolar cookie e banco
    public SalvarLancheCliente(ValidadorCookie validadorCookie, DaoLanche lancheDao, DaoIngrediente ingredienteDao) {
        this.validadorCookie = validadorCookie;
        this.lancheDao = lancheDao;
        this.ingredienteDao = ingredienteDao;
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

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        BufferedReader leitor = new BufferedReader(new InputStreamReader(request.getInputStream()));

        if (sessaoClienteValida(request)) {
            JSONObject dados = lerPayloadJson(leitor);
            Lanche lancheSalvo = montarESalvarLanche(dados);
            enviarResposta(response, montarUrlCarrinho(lancheSalvo));
        } else {
            enviarResposta(response, "erro");
        }
    }

    private boolean sessaoClienteValida(HttpServletRequest request) {
        try {
            Cookie[] cookies = request.getCookies();
            return this.validadorCookie.validar(cookies);
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "Cookies ausentes; a requisição será recusada", e);
            return false;
        }
    }

    private JSONObject lerPayloadJson(BufferedReader leitor) throws IOException {
        String linhaJson = leitor.readLine();
        String jsonUtf8 = new String(linhaJson.getBytes(ISO_8859_1), UTF_8);
        return new JSONObject(jsonUtf8);
    }

    private Lanche montarESalvarLanche(JSONObject dados) {
        JSONObject ingredientes = dados.getJSONObject("ingredientes");
        Lanche lanche = new Lanche();
        lanche.setNome(dados.getString("nome"));
        lanche.setDescricao(dados.getString("descricao"));

        Iterator<String> nomesIngredientes = ingredientes.keys();
        double precoTotal = calcularPrecoLanche(ingredientes, nomesIngredientes);

        lanche.setValor_venda(precoTotal);
        this.lancheDao.salvarCliente(lanche);

        Lanche lancheComId = this.lancheDao.pesquisaPorNome(lanche);
        vincularIngredientes(lancheComId, ingredientes, nomesIngredientes);
        return lancheComId;
    }

    private double calcularPrecoLanche(JSONObject ingredientes, Iterator<String> nomesIngredientes) {
        double precoTotal = 0.0;
        while (nomesIngredientes.hasNext()) {
            String nomeIngrediente = nomesIngredientes.next();
            Ingrediente ingrediente = buscarIngredientePorNome(nomeIngrediente);
            precoTotal += ingrediente.getValor_venda() * ingredientes.getInt(nomeIngrediente);
        }
        return precoTotal;
    }

    private void vincularIngredientes(Lanche lancheComId, JSONObject ingredientes,
            Iterator<String> nomesIngredientes) {
        while (nomesIngredientes.hasNext()) {
            String nomeIngrediente = nomesIngredientes.next();
            Ingrediente ingrediente = buscarIngredientePorNome(nomeIngrediente);
            ingrediente.setQuantidade(ingredientes.getInt(nomeIngrediente));
            this.lancheDao.vincularIngrediente(lancheComId, ingrediente);
        }
    }

    private Ingrediente buscarIngredientePorNome(String nome) {
        Ingrediente filtro = new Ingrediente();
        filtro.setNome(nome);
        return this.ingredienteDao.pesquisaPorNome(filtro);
    }

    private String montarUrlCarrinho(Lanche lanche) {
        return "../carrinho/carrinho.html?nome=" + lanche.getNome() + "&preco=" + lanche.getValor_venda();
    }

    private void enviarResposta(HttpServletResponse response, String conteudo) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println(conteudo);
        }
    }

    private void executarRequisicao(HttpServletRequest request, HttpServletResponse response, String mensagemErro) {
        try {
            processRequest(request, response);
        } catch (ServletException | IOException e) {
            LOGGER.log(Level.SEVERE, mensagemErro, e);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        executarRequisicao(request, response, "Erro ao processar requisição GET");
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        executarRequisicao(request, response, "Erro ao processar requisição POST");
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
