/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DAO;

import Helpers.EncryptadorMD5;
import Model.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author kener_000
 */
public class DaoCliente {

    // Entrega 2 (Sonar): logger no lugar de System.out/System.err
    private static final Logger LOGGER = Logger.getLogger(DaoCliente.class.getName());

    // Entrega 2 (Sonar): nomes de coluna repetidos viram constantes
    private static final String COL_ID_CLIENTE = "id_cliente";
    private static final String COL_SOBRENOME = "sobrenome";
    private static final String COL_TELEFONE = "telefone";
    private static final String COL_USUARIO = "usuario";
    private static final String COL_SENHA = "senha";

    private Connection conecta;
    // Nulo no uso normal: salvar() continua criando o DaoEndereco sob demanda
    private DaoEndereco enderecoDao;
    private EncryptadorMD5 md5;
    
    public DaoCliente(){
        this.conecta = new DaoUtil().conecta();
        this.md5 = new EncryptadorMD5();
    }

    // Permite testar o DAO sem banco real, inclusive o DaoEndereco usado em salvar()
    public DaoCliente(Connection conecta, DaoEndereco enderecoDao){
        this(conecta, enderecoDao, new EncryptadorMD5());
    }

    public DaoCliente(Connection conecta, DaoEndereco enderecoDao, EncryptadorMD5 md5){
        this.conecta = conecta;
        this.enderecoDao = enderecoDao;
        this.md5 = md5;
    }
    
    public void salvar(Cliente cliente){
        String sql = "INSERT INTO tb_clientes(nome, sobrenome, telefone, usuario, senha, fg_ativo, id_endereco) "
                  + "VALUES(?,?,?,?, MD5(?),?,?)";
        
        
        try{
            PreparedStatement stmt = conecta.prepareStatement(sql);
            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getSobrenome());
            stmt.setString(3, cliente.getTelefone());
            stmt.setString(4, cliente.getUsuario());
            stmt.setString(5, cliente.getSenha());
            stmt.setInt(6, cliente.getFg_ativo());
            DaoEndereco dend = (enderecoDao != null) ? enderecoDao : new DaoEndereco();
            if(dend.validaEndereco(cliente.getEndereco()) == 0){
                dend.salvar(cliente.getEndereco());
                stmt.setInt(7, dend.validaEndereco(cliente.getEndereco()));
            } else{
                stmt.setInt(7, dend.validaEndereco(cliente.getEndereco()));
            }
            stmt.execute();
            stmt.close();
            
            
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    
    public List<Cliente> listarTodos(){
        String sql = "SELECT * FROM tb_clientes WHERE fg_Ativo='1' ORDER BY id_cliente";
        ResultSet rs;
        List<Cliente> clientes = new ArrayList<>();
        
        try{
            
            PreparedStatement stmt = conecta.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()){
            
                Cliente cliente = new Cliente();
                cliente.setId_cliente(rs.getInt(COL_ID_CLIENTE));
                cliente.setNome(rs.getString("nome"));
                cliente.setSobrenome(rs.getString(COL_SOBRENOME));
                cliente.setTelefone(rs.getString(COL_TELEFONE));
                cliente.setUsuario(rs.getString(COL_USUARIO));
                cliente.setSenha(rs.getString(COL_SENHA));
                cliente.setFg_ativo(1);
                
                clientes.add(cliente);
            }
            rs.close();
            stmt.close();
            return clientes;
        
            
        } catch(SQLException e){
            
             throw new RuntimeException(e);
        }
        
    }
    
    public Cliente pesquisaPorUsuario(Cliente cliente){
        String sql = "SELECT * FROM tb_clientes WHERE usuario='"+cliente.getUsuario()+"'";
        ResultSet rs;
        Cliente clienteResultado = new Cliente();
        
        try{
            
            PreparedStatement stmt = conecta.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()){
            
                clienteResultado.setId_cliente(rs.getInt(COL_ID_CLIENTE));
                clienteResultado.setNome(rs.getString("nome"));
                clienteResultado.setSobrenome(rs.getString(COL_SOBRENOME));
                clienteResultado.setTelefone(rs.getString(COL_TELEFONE));
                clienteResultado.setUsuario(rs.getString(COL_USUARIO));
                clienteResultado.setSenha(rs.getString(COL_SENHA));
                clienteResultado.setFg_ativo(1);

            }
            rs.close();
            stmt.close();
            return clienteResultado;
        
            
        } catch(SQLException e){
            
             throw new RuntimeException(e);
        }
        
    }
    
    public Cliente pesquisaPorID(String id){
        String sql = "SELECT * FROM tb_clientes WHERE id_cliente='"+id+"'";
        ResultSet rs;
        Cliente clienteResultado = new Cliente();
        
        try{
            
            PreparedStatement stmt = conecta.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()){
                clienteResultado.setId_cliente(rs.getInt(COL_ID_CLIENTE));
                clienteResultado.setNome(rs.getString("nome"));
                clienteResultado.setSobrenome(rs.getString(COL_SOBRENOME));
                clienteResultado.setTelefone(rs.getString(COL_TELEFONE));
                clienteResultado.setFg_ativo(1);
            }
            rs.close();
            stmt.close();
            return clienteResultado;
        
            
        } catch(SQLException e){
            
             throw new RuntimeException(e);
        }
        
    }
    
    public boolean login(Cliente cliente){
        String sql = "SELECT usuario, senha, fg_ativo FROM tb_clientes WHERE usuario = ?";
        
        // Entrega 2 (Sonar): try-with-resources fecha o ResultSet e o PreparedStatement mesmo quando há erro
        try (PreparedStatement stmt = conecta.prepareStatement(sql)) {
            stmt.setString(1, cliente.getUsuario());
            Cliente validCliente = new Cliente();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()){
                    validCliente.setUsuario(rs.getString(COL_USUARIO));
                    validCliente.setSenha(rs.getString(COL_SENHA));
                    validCliente.setFg_ativo(rs.getInt("fg_ativo"));
                }
            }

            // Entrega 2 (Sonar): sem os System.out que imprimiam o hash da senha; um único return no lugar do if/else
            return md5.encryptar(cliente.getSenha()).equals(validCliente.getSenha())
                    && validCliente.getFg_ativo() == 1;

        } catch(SQLException e){
            LOGGER.log(Level.WARNING, "Erro ao fazer login", e);
        }
        
        return false;
    }
    
}


