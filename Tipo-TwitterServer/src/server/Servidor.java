package server;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import static server.TClient.writeInFile;


/**
* CLASSE - Servidor:
*   Representa o Servidor da aplicação, ficando em espera para
*   receber novos Clientes e fazendo tanto o controle do estabelecimento 
*   de conexão via Socket com eles quanto a inicialização da Thread que
*   representará eles na Pool de Threads
*
*   @author  Mikaelly Felício Pedrosa
*   @since   29-04-2019 
*/
public class Servidor {
    //Cria o novo Pool de Threads para os nossos clientes
    static ExecutorService piscina = Executors.newCachedThreadPool();
    static HashMap<String, DataOutputStream> onlines = new HashMap<String, DataOutputStream>();
    
    static  String seguidoresFile = "Seguidores.txt";
    static String menssagensFile = "Menssagens.txt";
    static String logFile = "Log.txt";
    
    static ServerSocket server;
    
    public static void main(String args[]) throws IOException {

        try {
            //Inicia o servidor na porta 3322
            server = new ServerSocket(3322);
            System.out.println("Servidor iniciado na porta 3322");
             piscina.submit(new ServerJFrame());
            while (true) {               
                //Socket fica em LISTEN
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
    
    public static class ServerJFrame extends javax.swing.JFrame implements Runnable{
    private  Socket cliente;
    private  DataInputStream entrada;
    private  DataOutputStream saida;

    /**
    * Construtor:
    *   Inicializa os componentes da Interface Gráfica,
    *   estabelece a conexão e prepara o botão de enviar
    */
    public ServerJFrame() {
        //Inicio os componentes graficos
        initComponents();        
               
        //Adiciona um KeyListener para o botão de enviar
        enviarMenssagem.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    enviarMenssagem.doClick();
                    System.out.println("click");
                }
            }
        });       
        
        attComArquivo();
    }

    /**
     * Método - initComponents:
     *   É chamado no construtor para inicializar o Form que exibe a Interface Gráfica.
     *   
     *   WARNING: Do NOT modify this code. The content of this method is always
     *   regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();       
        enviarMenssagem = new javax.swing.JButton();
        AttButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        enviarMenssagem.setText("Enviar Menssagem");
        enviarMenssagem.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                try {
                    enviarMenssagemMouseClicked(evt);
                } catch (IOException ex) {
                    Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        AttButton.setText("Atualizar");
        AttButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                AttButtonMouseClicked(evt);
            }
        });

        jTextPane2.setFont(new java.awt.Font("Ubuntu", 0, 12)); // NOI18N
        jScrollPane2.setViewportView(jTextPane2);

        jScrollPane1.setViewportView(jTextPane1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
               
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 273, Short.MAX_VALUE)
                .addComponent(enviarMenssagem, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(AttButton)
                .addGap(305, 305, 305))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 388, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AttButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(enviarMenssagem, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>                        


    /**
    * Método - AttButtonMouseClicked:
    *   É uma função relativa ao botão de atualizar o painel de texto fazendo-o
    *   receber o conjunto novo de mensagens
    *
    *   @param evt Evento do click no Botão
    */
    private void AttButtonMouseClicked(java.awt.event.MouseEvent evt) {                                       
        attComArquivo();
    }
    
    private void attComArquivo(){
        try {
            //Envia uma menssagem com um comando determinado por nós para o servidor manda as novas menssagens
            String str = String.join("\n\n", readFile("Menssagens.txt")); 
            //Depois recebe essas menssagens e atualiza o painel de texto
            jTextPane2.setText(str);
        } catch (IOException ex) {
            Logger.getLogger(ServerJFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
    * Método - enviarMenssagemMouseClicked:
    *   É uma função relativa ao botão de enviar uma mensagem para o servidor
    *
    *   @param evt Evento do click no Botão
    */
    private void enviarMenssagemMouseClicked(java.awt.event.MouseEvent evt) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss");
        java.util.Date d = new Date();
        String dStr = dateFormat.format(d);
        
        
        String nMenssagem = "@" + "ServidorTipo-Twitter" + ": " +
                            jTextPane1.getText() + "(" + "Enviado em: " + dStr + ")";
                    //Adicionamos ela ao arquivo de menssagens
                    writeInFile(menssagensFile, readFile(menssagensFile, nMenssagem)); 
                    //E enviamos de volta as menssagens atualizadas
                    String enviando = String.join("\n\n", readFile(menssagensFile));
                    //Por fim atualizo o log de menssagens
                    attLog(jTextPane1.getText(), dStr);
         
        for (Map.Entry<String, DataOutputStream> dude : onlines.entrySet()) {
                dude.getValue().writeUTF(enviando);
            }
        jTextPane1.setText("");
        attComArquivo();
    }                                            

   
    
    public void run() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */    

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new ServerJFrame().setVisible(true);
        });
    }


    // Variables declaration - do not modify                     
    private javax.swing.JButton AttButton; 
    private javax.swing.JButton enviarMenssagem;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    // End of variables declaration                   
    }
    
    
     public static void writeInFile(String filename, String[] x) throws IOException{               
        try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(filename))) {
            for(int i = 0; i < x.length; i++){  
                outputWriter.write(x[i]);
                outputWriter.newLine();
            }

            outputWriter.flush();    
        }

    }
     
     
      public static String[] readFile(String filename, String menssagem) throws FileNotFoundException, IOException{
    
        try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            boolean continua = true;
            List<String> listaLines = new ArrayList<String>();
            StringBuilder text = new StringBuilder();
            String readStringLine = reader.readLine();
            listaLines.add(readStringLine);       

            while(continua){
                readStringLine = reader.readLine();

                if(readStringLine != null){
                    listaLines.add(readStringLine);
                }else{
                    continua = false;
                }
            }

            listaLines.add(menssagem);
            
            String[] lines = listaLines.toArray(new String[listaLines.size()]);
            return  lines;
        }
    
    }
      
      
      public static String[] readFile(String filename) throws FileNotFoundException, IOException{
        try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            boolean continua = true;
            List<String> listaLines = new ArrayList<String>();
            StringBuilder text = new StringBuilder();
            String readStringLine = reader.readLine();
            listaLines.add(readStringLine);       


            while(continua){
                readStringLine = reader.readLine();

                if(readStringLine != null){
                    listaLines.add(readStringLine);
                }else{
                    continua = false;
                }
            }
            
            String[] lines = listaLines.toArray(new String[listaLines.size()]);

            return  lines;
        }
    
    }
      
      
      public static  void attLog(String menssagem, String dataHora) throws IOException{
        String ip = server.getInetAddress().getHostAddress();
        String newLog = "<" + "ServidorTipo-Twitter" + ", " + ip +  ":" + "3322" + ", "
                + dataHora + ", " + menssagem + ">";
        
        writeInFile(logFile, readFile(logFile, newLog));
    }
      
     

}