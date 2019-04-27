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


public class TClient implements Runnable {
    //Cria o hash para salvar os seguidores usando o IP como chave
    HashMap<String, String[]> seguidores = new HashMap<String, String[]>();
    //Cria um semaforo para organizar o acesso a leitura e escria do "banco de dados"
    Semaphore sem = new Semaphore(1);
    //Cria as strings com os nomes dos arquivos usados como banco de dados e LOG
    String seguidoresFile = "Seguidores.txt";
    String menssagensFile = "Menssagens.txt";
    String logFile = "Log.txt";


    Socket cliente;
    DataInputStream entrada;
    DataOutputStream saida;

    //Construtor que recebe o socket
    public TClient(Socket cliente) throws IOException{
        this.cliente = cliente;
        //Carrega os seguidores conhecidos no hash
        seguidores = attSeguidores(seguidores);
    }

    @Override
    public void run(){
        try {
            //Cria o SEND e o RECEIVE do servidor 
            entrada = new DataInputStream(cliente.getInputStream());
            saida = new DataOutputStream(cliente.getOutputStream());
            
            //Checa se o IP conctado é de algum seguidor ou um ip novo
            if(seguidores.get(cliente.getInetAddress().getHostAddress()) == null){
               //Se for novo ele vai enviar 0 e receber o nick dele, depois ele salva no hash junto com o ip e porta dele
               sem.acquire();
               saida.writeUTF("0");
               String[] entradas = new String[2];
               entradas[0] = String.valueOf(cliente.getPort());
               entradas[1] = entrada.readUTF();
               seguidores.put(cliente.getInetAddress().getHostAddress(),
                        entradas);
               sem.release();
            }else{
               //Se o IP for de um seguidor ele envia o nick para ele e atualiza a porta que ele se conectou
               sem.acquire();
               saida.writeUTF(seguidores.get(cliente.getInetAddress().getHostAddress())[1]);
               seguidores.get(cliente.getInetAddress().getHostAddress())[0] = String.valueOf(cliente.getPort());
               sem.release();
            }            
            sem.acquire();
            //Escreve o novo seguidor no arquivo de seguidores
            writeInFile(seguidoresFile, formatHashToWrite(seguidores));
            //Pega as menssagens que ja foram publicadas que mostra pro novo cliente
            String str = String.join("\n\n", readFile(menssagensFile)); 
            saida.writeUTF(str);
            sem.release();
            
        } catch (IOException ex) {
            Logger.getLogger(TClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(TClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Esse while continua até o cliente deslogar
        while (cliente.isConnected()) {
            String menssagem;
            try {
                //Recebe a menssagem enviada
                menssagem = entrada.readUTF();
                sem.acquire();
                //Pegamos a data e hora que a menssagem foi enviada
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss");
                java.util.Date d = new Date();
                String dStr = dateFormat.format(d);
                //Checamos para ver se é uma menssagem para parar de seguir o servidor
                if(menssagem.equals("Console:UNFOLLOW:1B71")){
                    //Se for ele tira o seguidor do hash de seguidores, retira ele do arquivo e depois desconecta o cliente
                    seguidores.remove(cliente.getInetAddress().getHostAddress());
                    saida.writeUTF("Voçê deixou de seguir o servidor");
                    writeInFile(seguidoresFile, formatHashToWrite(seguidores));
                    cliente.close();             
                }
                //Checamos para ver se é uma menssagem mandar atualizar
                if(menssagem.equals("Console:ATTALLMENSSAGE:1B71")){
                    //Enviamos as menssagens atualizadas
                    saida.writeUTF(String.join("\n\n", readFile(menssagensFile)));
                }else{
                    //Adicionamos o remetente e a Data/hora a menssagem
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
    
    //Função que tranforma o hash de reguidores em um array de String para poder escrever ele no arquivo
    public String[] formatHashToWrite(HashMap<String, String[]> hash){
        String[] lines = new String[hash.size()];
        int i = 0;
        for ( String key : hash.keySet() ) {
                String[] resto = hash.get(key);                
                lines[i] = key + " " + resto[0] + " " + resto[1];                
                i++;
        }
        
        return lines;
    }
    
    //Função que escreve um Array de string em um arquivo, onde cara posisão do array é u m linha de txt
    public static void writeInFile(String filename, String[] x) throws IOException{       
        
        
        try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(filename))) {
            
            for(int i = 0; i < x.length; i++){  
                outputWriter.write(x[i]);
                outputWriter.newLine();
            }
            outputWriter.flush();    
        }

    }
    
    //Função que ler arquivo txt e salva cada linha em um posição do array
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
    //Função que ler arquivo txt e salva cada linha em um posição do array, mas essa é feita para ja retornar com a ultima menssagem recebida
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
    
    public void attLog(String menssagem, String dataHora, String[] seguidor) throws IOException{
        String ip = cliente.getInetAddress().getHostAddress();
        String newLog = "<" + seguidor[1] + ", " + ip +  ":" + seguidor[0] + ", "
                + dataHora + ", " + menssagem + ">";
        
        writeInFile(logFile, readFile(logFile, newLog));
    
    }


    //Essa função pega os seguidores ja conhecidos do nosso "banco de dados" e coloca em um hash
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
