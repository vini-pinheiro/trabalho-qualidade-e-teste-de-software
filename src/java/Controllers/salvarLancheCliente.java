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
    private final DaoLanche lancheDaoInjetado;
    private final DaoIngrediente ingredienteDaoInjetado;

    public SalvarLancheCliente() {
        this(new ValidadorCookie(), new DaoLanche(), new DaoIngrediente());
    }

    // Usado nos testes para isolar cookie e banco
    public SalvarLancheCliente(ValidadorCookie validadorCookie, DaoLanche lancheDao, DaoIngrediente ingredienteDao) {
        this.validadorCookie = validadorCookie;
        this.lancheDaoInjetado = lancheDao;
        this.ingredienteDaoInjetado = ingredienteDao;
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
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String json = "";
        
        ////////Validar Cookie
        boolean resultado = false;
        
        try {
            Cookie[] cookies = request.getCookies();
            resultado = validadorCookie.validar(cookies);
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "Cookies ausentes; a requisi\u00e7\u00e3o ser\u00e1 recusada", e);
        }
        //////////////
        
        if ((br != null) && resultado) {
            json = br.readLine();
            byte[] bytes = json.getBytes(ISO_8859_1); 
            String jsonStr = new String(bytes, UTF_8);            
            JSONObject dados = new JSONObject(jsonStr);
            JSONObject ingredientes = dados.getJSONObject("ingredientes");
            
            double precoDoLanche = 0.00;
            
            Lanche lanche = new Lanche();
            
            lanche.setNome(dados.getString("nome"));
            lanche.setDescricao(dados.getString("descricao"));
            
            
            DaoLanche lancheDao = (lancheDaoInjetado != null) ? lancheDaoInjetado : new DaoLanche();
            DaoIngrediente ingredienteDao = (ingredienteDaoInjetado != null) ? ingredienteDaoInjetado : new DaoIngrediente();
            
            Iterator<String> keys = ingredientes.keys();
            
            while(keys.hasNext()) {
                
                String key = keys.next(); 
                Ingrediente ingredienteLanche = new Ingrediente();
                ingredienteLanche.setNome(key);
                
                Ingrediente ingredienteComID = ingredienteDao.pesquisaPorNome(ingredienteLanche);
                precoDoLanche += ingredienteComID.getValor_venda() * Double.valueOf(ingredientes.getInt(key));
            }
            
            
            lanche.setValor_venda(precoDoLanche);
            lancheDao.salvarCliente(lanche);
            
            Lanche lancheComID = lancheDao.pesquisaPorNome(lanche);
            
            while(keys.hasNext()) {
                
                String key = keys.next(); 
                Ingrediente ingredienteLanche = new Ingrediente();
                ingredienteLanche.setNome(key);
                
                Ingrediente ingredienteComID = ingredienteDao.pesquisaPorNome(ingredienteLanche);
                ingredienteComID.setQuantidade(ingredientes.getInt(key));
                lancheDao.vincularIngrediente(lancheComID, ingredienteComID);
            }
            
            try (PrintWriter out = response.getWriter()) {
            out.println("../carrinho/carrinho.html?nome="+lancheComID.getNome()+"&preco="+lancheComID.getValor_venda());
            }
        } else {
            try (PrintWriter out = response.getWriter()) {
            out.println("erro");
        }
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
        } catch (ServletException | IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao processar requisi\u00e7\u00e3o GET", e);
        }
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
        } catch (ServletException | IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao processar requisi\u00e7\u00e3o POST", e);
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
