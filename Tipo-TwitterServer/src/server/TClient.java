package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextField;
import static server.Servidor.onlines;


/**
* CLASSE - TClient (Thread of Client):
*   Representa um Cliente em execução (sua Thread) na Pool do Servidor, 
*   tendo também as funções de leitura e escrita dos dados
*
*   @author  Drayton Corrêa Filho
*   @author  Armando de Souza Salvador Neto
*   @since   29-04-2019 
*/
public class TClient implements Runnable {
    //Cria o hash para salvar os seguidores usando o IP como chave primária
    HashMap<String, String[]> seguidores = new HashMap<String, String[]>();
    //Cria um semáforo para organizar o acesso a leitura e escria do "banco de dados"
    Semaphore sem = new Semaphore(1);
    //Cria as strings com os nomes dos arquivos usados como banco de dados e LOG
    String seguidoresFile = "Seguidores.txt";
    String menssagensFile = "Menssagens.txt";
    String logFile = "Log.txt";

    Socket cliente;
    DataInputStream entrada;
    DataOutputStream saida;

    /**
    * Construtor:
    *   Recebe o Socket do cliente e atualiza os seguidores
    *   conhecidos no HashMap
    * 
    *   @param cliente É o Socket relativo ao cliente
    */
    public TClient(Socket cliente) throws IOException{
        this.cliente = cliente;
        //Carrega os seguidores conhecidos no hash
        seguidores = attSeguidores(seguidores);
    }


    /**
    * Método - run:
    *   Override do método run, padrão de qualquer thread. Ele
    *   serve como fluxo principal de execução de uma thread e
    *   é o responsável por dar inicio ao paralelismo da mesma 
    */
    @Override
    public void run(){
        try {
            //Cria o SEND e o RECEIVE do servidor 
            entrada = new DataInputStream(cliente.getInputStream());
            saida = new DataOutputStream(cliente.getOutputStream());
            
            //Checa se o IP conectado é de algum seguidor ou um IP novo
            if(seguidores.get(cliente.getInetAddress().getHostAddress()) == null){
               //Se for novo ele vai enviar 0 e receber o nick 
               //dele, depois ele salva no hash junto com o ip e porta dele
               sem.acquire();   //Usa o semáforo para apenas um thread acessar por vez
               saida.writeUTF("0");
               String[] entradas = new String[2];
               entradas[0] = String.valueOf(cliente.getPort());
               entradas[1] = entrada.readUTF();
               seguidores.put(cliente.getInetAddress().getHostAddress(), entradas);
               sem.release();   //Libera o semáforo para voltar a ser paralelo
            }else{
               //Se o IP for de um seguidor ele envia 
               //o nick para ele e atualiza a porta que ele se conectou
               sem.acquire();   //Usa o semáforo para apenas um thread acessar por vez
               saida.writeUTF(seguidores.get(cliente.getInetAddress().getHostAddress())[1]);
               seguidores.get(cliente.getInetAddress().getHostAddress())[0] = String.valueOf(cliente.getPort());
               sem.release();   //Libera o semáforo para voltar a ser paralelo
            }    

            sem.acquire();   //Usa o semáforo para apenas um thread acessar por vez
            onlines.put(seguidores.get(cliente.getInetAddress().getHostAddress())[1], saida);
            //Escreve o novo seguidor no arquivo de seguidores
            writeInFile(seguidoresFile, formatHashToWrite(seguidores));
            //Pega as menssagens que ja foram publicadas que mostra pro novo cliente
            String str = String.join("\n\n", readFile(menssagensFile)); 
            saida.writeUTF(str);
            sem.release();   //Libera o semáforo para voltar a ser paralelo

        } catch (IOException ex) {
            Logger.getLogger(TClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(TClient.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Esse while continua até o cliente deslogar
        while (cliente.isConnected()) {
            String menssagem;
            try {
                //Recebe a mensagem enviada
                menssagem = entrada.readUTF();
                sem.acquire();

                //Pegamos a data e hora que a mensagem foi enviada
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss");
                java.util.Date d = new Date();
                String dStr = dateFormat.format(d);

                //Checamos para ver se é uma mensagem para parar de seguir o servidor
                if(menssagem.equals("Console:UNFOLLOW:1B71")){
                    //Se for ele tira o seguidor do hash de seguidores, retira ele do arquivo e depois desconecta o cliente
                    seguidores.remove(cliente.getInetAddress().getHostAddress());
                    saida.writeUTF("Voçê deixou de seguir o servidor");
                    sem.release();
                    writeInFile(seguidoresFile, formatHashToWrite(seguidores));
                    cliente.close();             
                }

                //Checamos para ver se é uma mensagem que manda atualizar
                if(menssagem.equals("Console:ATTALLMENSSAGE:1B71")){
                    // Se for, enviamos as mensagens atualizadas
                    saida.writeUTF(String.join("\n\n", readFile(menssagensFile)));
                    sem.release();
                }else{
                    //Caso contrário, adicionamos o remetente e a Data/hora a mensagem
                    String nMenssagem = "@" + seguidores.get(cliente.getInetAddress().getHostAddress())[1] + ": " +
                            menssagem + "(" + "Enviado em: " + dStr + ")";
                    //Adicionamos ela ao arquivo de menssagens
                    writeInFile(menssagensFile, readFile(menssagensFile, nMenssagem)); 
                    //E enviamos de volta as menssagens atualizadas
                    saida.writeUTF(String.join("\n\n", readFile(menssagensFile)));
                    //Por fim atualizo o log de menssagens
                    attLog(menssagem, dStr, seguidores.get(cliente.getInetAddress().getHostAddress()));
                    sem.release();
                }      
            } catch (IOException ex) {
                Logger.getLogger(TClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(TClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    

    /**
    * Método - formatHashToWrite (Format Hash to Write in File):
    *   Transforma o HashMap de seguidores em um array de Strings para
    *   possibilitar a escrita dele em um arquivo
    * 
    *   @param hash É o HashMap que será transformado em um array
    *
    *   @return String[] É um array em que cada elemento será uma linha do arquivo contendo a informação do Hash 
    */
    public String[] formatHashToWrite(HashMap<String, String[]> hash){
        String[] lines = new String[hash.size()];
        int i = 0;

        //Para cada chave primária no hash
        for ( String key : hash.keySet() ) {
            //usa ela para pegar o que está escrito em formato de String
            String[] resto = hash.get(key);                
            //e transformar em um array o qual cada elemento será
            //uma linha do arquivo final aonde será salvo o Hash 
            lines[i] = key + " " + resto[0] + " " + resto[1];                
            
            i++;
        }
        
        return lines;
    }
    

    /**
    * Método - writeInFile:
    *   Escreve um Array de Strings num arquivo txt, aonde cada linha
    *   conterá a informação de cada elemento do HashMap
    * 
    *   @param filename É o nome do arquivo aonde sera salvo os dados
    *   @param x Array cujos dados serão salvos no arquivo
    */
    public static void writeInFile(String filename, String[] x) throws IOException{               
        try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(filename))) {
            for(int i = 0; i < x.length; i++){  
                outputWriter.write(x[i]);
                outputWriter.newLine();
            }

            outputWriter.flush();    
        }

    }
    

    /**
    * Método - readFile:
    *   Lê um arquivo txt e salva cada linha dele como um elemento de um Array
    * 
    *   @param filename É o nome do arquivo aonde sera salvo os dados
    *   @return String[] O Array aonde as linhas do arquivo serão salvas
    */
    public String[] readFile(String filename) throws FileNotFoundException, IOException{
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


    /**
    * Método - readFile:
    *   Lê um arquivo txt e salva cada linha dele como um elemento de um Array e, além disso,
    *   retorna ele com a última mensagem recebida já inclusa
    * 
    *   @param filename É o nome do arquivo aonde sera salvo os dados
    *   @param menssagem Última mensagem recebida que será adicionada ao array
    *
    *   @return String[] O Array aonde as linhas do arquivo serão salvas em conjunto com a última mensagem recebida
    */
    public String[] readFile(String filename, String menssagem) throws FileNotFoundException, IOException{
    
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
    

    /**
    * Método - attLog:
    *   Atualiza o Log de mensagens salvas no arquivo
    * 
    *   @param menssagem Última mensagem recebida que será adicionada ao Log
    *   @param dataHora Data e hora em que a mensagem foi enviada
    *   @param seguidor Contém o IP e nome de usuário do Seguidor para salvar no Log 
    *
    *   @exception IOException Em caso de erro ao tentar acessar o arquivo
    *   @see IOException
    */
    public void attLog(String menssagem, String dataHora, String[] seguidor) throws IOException{
        String ip = cliente.getInetAddress().getHostAddress();
        String newLog = "<" + seguidor[1] + ", " + ip +  ":" + seguidor[0] + ", "
                + dataHora + ", " + menssagem + ">";
        
        writeInFile(logFile, readFile(logFile, newLog));
    }


    /**
    * Método - attSeguidores:
    *   Lê os Seguidores salvos no arquivo Seguidores.txt e os retorna em uma
    *   HashMap que contém eles
    * 
    *   @param hash HashMap aonde serão atualizados os Seguidores
    *
    *   @exception FileNotFoundException Em caso de erro ao tentar buscar o arquivo
    *   @exception IOException Em caso de erro ao tentar acessar o arquivo
    *   @see IOException
    *
    *   @return HashMap atualizado com os Seguidores
    */
    public static HashMap<String, String[]> attSeguidores(HashMap<String, String[]> hash)
            throws FileNotFoundException, IOException {
        //Abre o arquivo no modo reader
        try (BufferedReader reader = new BufferedReader(new FileReader("Seguidores.txt"))) {            
            
            boolean continua = true;
            String readStringLine;
            List<String> lines = new ArrayList<String>();
            //Salva cada linha em uma lista de string criada a cima
            while(continua){
            
                readStringLine = reader.readLine();  
                if(readStringLine != null){
                    lines.add(readStringLine);
                }else{
                    continua = false;
                }  
            }

            String[] linesArray = lines.toArray(new String[lines.size()]);
            
            //Depois de transformar a lista em array de strings adicionamos os seguidores no hash
            for (int i = 0; i < linesArray.length; i++) {
                System.out.println("foi");
                String[] partes = linesArray[i].split(" ");
                String[] entradas = new String[2];
                entradas[0] = partes[1];
                entradas[1] = partes[2];

                hash.put(partes[0], entradas);
                
            }

            return hash;
        }
    }
}
