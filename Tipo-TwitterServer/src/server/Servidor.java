package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Servidor {

    public static void main(String args[]) throws IOException {
       
        //Cria o noo Pool de Threads para os nossos clientes
        ExecutorService piscina = Executors.newCachedThreadPool();

        try {
            //Inicia o servidor na porta 3322
            ServerSocket server = new ServerSocket(3322);
            System.out.println("Servidor iniciado na porta 3322");
            while (true) {
                //Socket fica em LISTENIN
                Socket cliente = server.accept();
                System.out.println("Cliente conectado do IP " + cliente.getInetAddress().getHostAddress()
                        + " e na porta " + cliente.getPort());                
                //O cliente que se conectou é enviado para o thread na nossa pool
                piscina.submit(new TClient(cliente));
            }

        } catch (IOException ex) {
            Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);

        }

    }

    
}
