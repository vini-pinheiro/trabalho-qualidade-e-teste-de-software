/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controllers;

import DAO.DaoBebida;
import DAO.DaoCliente;
import DAO.DaoLanche;
import DAO.DaoPedido;
import Helpers.ValidadorCookie;
import Model.Bebida;
import Model.Cliente;
import Model.Lanche;
import Model.Pedido;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author kener_000
 */
public class comprar extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(comprar.class.getName());

    private final ValidadorCookie validadorCookie;
    private final DaoCliente clienteDao;
    private final DaoLanche lancheDao;
    private final DaoBebida bebidaDao;
    private final DaoPedido pedidoDao;

    public comprar() {
        this(new ValidadorCookie(), new DaoCliente(), new DaoLanche(), new DaoBebida(), new DaoPedido());
    }

    public comprar(ValidadorCookie validadorCookie, DaoCliente clienteDao, DaoLanche lancheDao, DaoBebida bebidaDao,
            DaoPedido pedidoDao) {
        this.validadorCookie = validadorCookie;
        this.clienteDao = clienteDao;
        this.lancheDao = lancheDao;
        this.bebidaDao = bebidaDao;
        this.pedidoDao = pedidoDao;
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));

        if (validarCookies(request)) {
            JSONObject dados = lerDadosJson(br);
            Cliente cliente = this.clienteDao.pesquisaPorID(Integer.toString(dados.getInt("id")));

            List<Lanche> lanches = new ArrayList<>();
            List<Bebida> bebidas = new ArrayList<>();
            double valorTotal = processarItens(dados, lanches, bebidas);

            salvarEVincularPedido(cliente, valorTotal, lanches, bebidas);
            escreverResposta(response, "Pedido Salvo com Sucesso!");
        } else {
            escreverResposta(response, "erro");
        }
    }

    private boolean validarCookies(HttpServletRequest request) {
        try {
            Cookie[] cookies = request.getCookies();
            return this.validadorCookie.validar(cookies);
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "Erro ao validar cookies da requisiÃ§Ã£o", e);
            return false;
        }
    }

    private JSONObject lerDadosJson(BufferedReader br) throws IOException {
        String json = br.readLine();
        byte[] bytes = json.getBytes(ISO_8859_1);
        String jsonStr = new String(bytes, UTF_8);
        return new JSONObject(jsonStr);
    }

    private double processarItens(JSONObject dados, List<Lanche> lanches, List<Bebida> bebidas) {
        double valorTotal = 0.0;
        for (String nome : dados.keySet()) {
            if (!"id".equals(nome)) {
                valorTotal += processarItem(nome, dados.getJSONArray(nome), lanches, bebidas);
            }
        }
        return valorTotal;
    }

    private double processarItem(String nome, JSONArray item, List<Lanche> lanches, List<Bebida> bebidas) {
        Object tipo = item.get(1);
        if ("lanche".equals(tipo)) {
            Lanche lanche = this.lancheDao.pesquisaPorNome(nome);
            lanche.setQuantidade(item.getInt(2));
            lanches.add(lanche);
            return lanche.getValor_venda();
        }
        if ("bebida".equals(tipo)) {
            Bebida bebida = this.bebidaDao.pesquisaPorNome(nome);
            bebida.setQuantidade(item.getInt(2));
            bebidas.add(bebida);
            return bebida.getValor_venda();
        }
        return 0.0;
    }

    private void salvarEVincularPedido(Cliente cliente, double valorTotal, List<Lanche> lanches,
            List<Bebida> bebidas) {
        Pedido pedido = new Pedido();
        pedido.setData_pedido(Instant.now().toString());
        pedido.setCliente(cliente);
        pedido.setValor_total(valorTotal);
        this.pedidoDao.salvar(pedido);

        Pedido pedidoSalvo = this.pedidoDao.pesquisaPorData(pedido);
        pedidoSalvo.setCliente(cliente);

        for (Lanche lanche : lanches) {
            this.pedidoDao.vincularLanche(pedidoSalvo, lanche);
        }
        for (Bebida bebida : bebidas) {
            this.pedidoDao.vincularBebida(pedidoSalvo, bebida);
        }
    }

    private void escreverResposta(HttpServletResponse response, String mensagem) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println(mensagem);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the
    // + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
        } catch (ServletException | IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao processar requisiÃ§Ã£o GET", e);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
        } catch (ServletException | IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao processar requisiÃ§Ã£o POST", e);
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
